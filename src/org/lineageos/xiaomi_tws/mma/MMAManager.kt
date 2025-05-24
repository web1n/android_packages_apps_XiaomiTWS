package org.lineageos.xiaomi_tws.mma

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EARBUDS_IN_EAR_MODE
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_NOTIFY_TYPE_BATTERY
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_INFO
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.utils.BluetoothUtils
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@SuppressLint("MissingPermission")
class MMAManager private constructor(private val context: Context) {

    private sealed class RequestResponse {
        abstract val requestId: String

        data class Success(override val requestId: String, val response: MMAResponse) :
            RequestResponse()

        data class Error(override val requestId: String, val error: Throwable) : RequestResponse()
    }

    private val mmaDevices = HashMap<String, MMADevice>()
    private val responseFlows = ConcurrentHashMap<String, MutableSharedFlow<RequestResponse>>()
    private val connectionListeners = mutableListOf<MMAListener>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

    private val bluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = IntentCompat.getParcelableExtra(
                intent, BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java
            ) ?: return
            if (intent.action != BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) {
                return
            }

            val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
            if (state == BluetoothHeadset.STATE_CONNECTED) {
                handleDeviceConnected(device)
            } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                handleDeviceDisconnected(device)
            }
        }
    }

    private fun handleDeviceConnected(device: BluetoothDevice) = coroutineScope.launch {
        if (DEBUG) Log.d(TAG, "Device connected: ${device.address}")
        if (mmaDevices.containsKey(device.address)) {
            return@launch
        }
        if (!BluetoothUtils.isHeadsetA2DPDevice(device)) {
            return@launch
        }

        val mma = MMADevice(device)
        mma.runCatching {
            connect()
        }.onSuccess {
            mmaDevices[device.address] = mma

            startReading(device)
            dispatchDeviceConnected(device)
        }.onFailure {
            Log.e(TAG, "Failed to connect device: ${device.address}", it)
            mma.close()
        }
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        mmaDevices.remove(device.address)?.let { mma ->
            if (DEBUG) Log.d(TAG, "Device disconnected: ${device.address}")
            mma.close()

            cancelAllRequests(device)
            dispatchDeviceDisconnected(device)
        }
    }

    private fun cancelAllRequests(device: BluetoothDevice) {
        responseFlows.keys.filter {
            it.startsWith(device.address)
        }.forEach {
            emitError(it, RuntimeException("Device disconnected"))
        }
    }

    private fun notifyResponse(device: BluetoothDevice, raw: ByteArray) {
        val response = try {
            MMAResponse.fromPacket(device, raw)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown response: ${raw.size} ${raw.toHexString()}", e)
            return
        }

        val requestId = "${device.address}-${response.opCodeSN}"

        val flow = responseFlows[requestId]
        if (flow != null) {
            flow.tryEmit(RequestResponse.Success(requestId, response))
            responseFlows.remove(requestId)
        } else if (response.type == 1 && response.flag == 1) {
            runCatching {
                when (response.opCode) {
                    XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_INFO -> handleNotifyDeviceInfo(response)
                    XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_CONFIG -> handleNotifyDeviceConfig(response)
                    else -> Log.w(TAG, "Unknown notify opCode: $requestId $response")
                }
            }.onFailure {
                Log.w(TAG, "Failed to handle notify: $requestId $response", it)
            }
        } else {
            Log.w(TAG, "Unknown flow for request: $requestId $response")
        }
    }

    private fun handleNotifyDeviceInfo(response: MMAResponse) {
        if (DEBUG) Log.d(TAG, "handleNotifyDeviceInfo: $response")
        check(response.opCode == XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_INFO)
        check(response.type == 1 && response.flag == 1)
        if (response.data.isEmpty()) {
            Log.w(TAG, "handleNotifyDeviceInfo: Empty response data")
        }
        val data = response.data
        val notifyType = data[0]

        when (notifyType) {
            XIAOMI_MMA_NOTIFY_TYPE_BATTERY -> {
                check(data.size == 4) { "Battery report length not 4, actual: ${data.size}" }
                dispatchDeviceBatteryChanged(
                    Earbuds.fromBytes(response.device.address, data[1], data[2], data[3])
                )
            }

            else -> if (DEBUG) Log.d(
                TAG,
                "handleNotifyDeviceInfo: Unknown type: $notifyType, data: ${data.toHexString()}"
            )
        }
    }

    private fun handleNotifyDeviceConfig(response: MMAResponse) {
        if (DEBUG) Log.d(TAG, "handleNotifyDeviceConfig: $response")
        check(response.opCode == XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_CONFIG)
        check(response.type == 1 && response.flag == 1)
        if (response.data.size < 3) {
            Log.w(TAG, "handleNotifyDeviceConfig: Empty response data")
        }
        val config = bytesToInt(response.data[0], response.data[1])
        val value = response.data.drop(2).toByteArray()

        when (config) {
            XIAOMI_MMA_CONFIG_EARBUDS_IN_EAR_MODE -> {
                check(value.size == 1) { "In ear report length not 1, actual: ${value.size}" }

                val left = value[0].toInt() and (1 shl 3) != 0
                val right = value[0].toInt() and (1 shl 2) != 0

                dispatchInEarStateChanged(response.device, left, right)
            }

            else -> dispatchConfigChanged(response.device, config, value)
        }
    }

    private fun emitError(requestId: String, throwable: Throwable) {
        responseFlows.remove(requestId)
            ?.tryEmit(RequestResponse.Error(requestId, throwable))
    }

    private fun isMMADevice(device: BluetoothDevice): Boolean {
        return mmaDevices.contains(device.address)
    }

    private fun getDevice(device: BluetoothDevice): MMADevice {
        return checkNotNull(mmaDevices[device.address]) {
            "Device not connected: ${device.address}"
        }
    }

    private fun startReading(device: BluetoothDevice) = coroutineScope.launch {
        while (isMMADevice(device) && device.isConnected) {
            val mma = mmaDevices[device.address] ?: break
            if (!mma.isConnected) {
                if (DEBUG) Log.d(TAG, "Reconnecting socket")
                try {
                    mma.connect()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reconnect socket", e)
                    break
                }
            }

            mma.runCatching {
                getResponsePacket()
            }.onSuccess {
                notifyResponse(device, it)
            }.onFailure {
                Log.w(TAG, "Failed to read: $device", it)
                mma.close()
            }
        }

        if (DEBUG) Log.d(TAG, "Stop reading: $device")
        handleDeviceDisconnected(device)
    }

    fun startBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "startBluetoothStateListening")

        val filter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }

        if (DEBUG) Log.d(TAG, "registering bluetooth state receiver")
        context.registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_EXPORTED)

        processConnectedDevices()
    }

    fun stopBluetoothStateListening() {
        if (DEBUG) Log.d(TAG, "stopBluetoothStateListening")

        context.unregisterReceiver(bluetoothStateReceiver)
        clearConnectionListeners()
        coroutineScope.coroutineContext.cancel()
    }

    private fun processConnectedDevices() {
        BluetoothUtils.connectedHeadsetA2DPDevices.forEach { handleDeviceConnected(it) }
    }

    fun registerConnectionListener(listener: MMAListener) {
        synchronized(connectionListeners) {
            if (!connectionListeners.contains(listener)) {
                connectionListeners.add(listener)

                mmaDevices.values.forEach { listener.onDeviceConnected(it.device) }
            } else {
                Log.w(TAG, "Listener already registered: $listener")
            }
        }
    }

    fun unregisterConnectionListener(listener: MMAListener) {
        synchronized(connectionListeners) {
            connectionListeners.remove(listener)
        }
    }

    fun clearConnectionListeners() {
        synchronized(connectionListeners) {
            connectionListeners.clear()
        }
    }

    private fun dispatchEvent(logMessage: String, action: (MMAListener) -> Unit) {
        if (DEBUG) Log.d(TAG, logMessage)

        coroutineScope.launch(Dispatchers.Main) {
            synchronized(connectionListeners) {
                connectionListeners.forEach(action)
            }
        }
    }

    private fun dispatchDeviceConnected(device: BluetoothDevice) {
        dispatchEvent("dispatchDeviceConnected: $device") { it.onDeviceConnected(device) }
    }

    private fun dispatchDeviceDisconnected(device: BluetoothDevice) {
        dispatchEvent("dispatchDeviceDisconnected: $device") { it.onDeviceDisconnected(device) }
    }

    private fun dispatchDeviceBatteryChanged(earbuds: Earbuds) {
        dispatchEvent("dispatchDeviceBatteryChanged: $earbuds") {
            it.onDeviceBatteryChanged(earbuds.device, earbuds)
        }
    }

    private fun dispatchConfigChanged(device: BluetoothDevice, config: Int, value: ByteArray) {
        dispatchEvent("dispatchConfigChanged: config: $config, value: ${value.toHexString()}") {
            it.onDeviceConfigChanged(device, config, value)
        }
    }

    private fun dispatchInEarStateChanged(device: BluetoothDevice, left: Boolean, right: Boolean) {
        dispatchEvent("dispatchInEarStateChanged: left: $left, right: $right") {
            it.onInEarStateChanged(device, left, right)
        }
    }

    suspend fun <T> sendRequestSuspend(
        device: BluetoothDevice,
        builder: MMARequestBuilder<T>
    ): OperationResult<T> = suspendCoroutine { continuation ->
        val (requestId, responseFlow) = prepareAndSendRequest(device, builder)

        coroutineScope.launch {
            responseFlow.filter { it.requestId == requestId }.collectLatest { result ->
                responseFlows.remove(requestId)

                if (result is RequestResponse.Success) {
                    if (builder.handler == null) {
                        if (DEBUG) Log.d(TAG, "Request response received, but handler is null")
                    } else {
                        continuation.resume(
                            OperationResult.Success<T>(builder.handler!!.invoke(result.response))
                        )
                    }
                } else if (result is RequestResponse.Error) {
                    continuation.resume(OperationResult.Error(result.error))
                }
            }
        }
    }

    private fun <T> prepareAndSendRequest(
        device: BluetoothDevice, builder: MMARequestBuilder<T>
    ): Pair<String, MutableSharedFlow<RequestResponse>> {
        val responseFlow = MutableSharedFlow<RequestResponse>(replay = 1)

        if (!isMMADevice(device)) {
            Log.w(TAG, "Request device not mma device: ${device.address}")
            val requestId = ""
            coroutineScope.launch {
                val error = RequestResponse.Error(
                    requestId,
                    RuntimeException("Device not available: ${device.address}")
                )
                responseFlow.emit(error)
            }
            return requestId to responseFlow
        }

        val newSN = getDevice(device).newOpCodeSN
        val requestId = "${device.address}-$newSN"
        val request = builder.request.apply { opCodeSN = newSN }

        responseFlows[requestId] = responseFlow

        // Send the request
        coroutineScope.launch {
            runCatching {
                withTimeout(TIMEOUT_MS_WRITE) {
                    getDevice(device).sendRequest(request)
                }
            }.onFailure {
                Log.e(TAG, "Failed to send request: $device", it)
                emitError(requestId, it)
            }
        }

        // Set timeout for the response
        coroutineScope.launch {
            delay(TIMEOUT_MS_READ)

            if (responseFlows.containsKey(requestId)) {
                Log.w(TAG, "Request timed out: $requestId")
                emitError(requestId, RuntimeException("Request timed out"))
            }
        }

        return requestId to responseFlow
    }

    suspend fun <T> runRequestCatching(
        device: BluetoothDevice,
        builder: () -> MMARequestBuilder<T>
    ): OperationResult<T> {
        return try {
            sendRequestSuspend(device, builder())
        } catch (e: Throwable) {
            OperationResult.Error(e)
        }
    }

    companion object {
        private val TAG = MMAManager::class.java.simpleName
        private const val DEBUG = true

        private const val TIMEOUT_MS_READ: Long = 2000
        private const val TIMEOUT_MS_WRITE: Long = 1000

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: MMAManager? = null

        fun getInstance(context: Context): MMAManager {
            return instance ?: synchronized(this) {
                instance ?: MMAManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
