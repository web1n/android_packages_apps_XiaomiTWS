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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_CONFIG_EARBUDS_IN_EAR_MODE
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_NOTIFY_TYPE_BATTERY
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_CONFIG
import org.lineageos.xiaomi_tws.EarbudsConstants.XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_INFO
import org.lineageos.xiaomi_tws.earbuds.Earbuds
import org.lineageos.xiaomi_tws.mma.DeviceInfoRequestBuilder.Companion.batteryInfo
import org.lineageos.xiaomi_tws.mma.MMAPacketBuilder.RequestBuilder
import org.lineageos.xiaomi_tws.mma.MMAPacketBuilder.RequestNoResponseBuilder
import org.lineageos.xiaomi_tws.utils.BluetoothUtils
import org.lineageos.xiaomi_tws.utils.ByteUtils.bytesToInt
import org.lineageos.xiaomi_tws.utils.ByteUtils.toHexString
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@SuppressLint("MissingPermission")
class MMAManager private constructor(private val context: Context) {

    private enum class DeviceStatus { Connected, Authing, Disconnected }

    private sealed class RequestResponse {
        data class Success(val packet: MMAPacket?) : RequestResponse()
        data class Error(val error: Throwable) : RequestResponse()
    }

    private val mmaDevices = HashMap<String, Pair<MMADevice, DeviceStatus>>()
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

        val mma = MMADevice(device)
        mma.runCatching {
            connect()
        }.onSuccess {
            mmaDevices[device.address] = mma to DeviceStatus.Authing

            startReading(device)
            checkConnectionState(device)
        }.onFailure {
            Log.e(TAG, "Failed to connect device: ${device.address}", it)
            mma.close()
        }
    }

    private fun getConnectStatus(device: BluetoothDevice): DeviceStatus {
        return mmaDevices[device.address]?.second ?: DeviceStatus.Disconnected
    }

    private fun updateConnectStatus(device: BluetoothDevice, status: DeviceStatus) {
        mmaDevices.computeIfPresent(device.address) { address, (mma) -> mma to status }
    }

    private fun handleDeviceDisconnected(device: BluetoothDevice) {
        mmaDevices.remove(device.address)?.let { (mma) ->
            if (DEBUG) Log.d(TAG, "Device disconnected: ${device.address}")
            mma.close()

            cancelAllRequests(device)
            dispatchEvent(DeviceEvent.Disconnected(device))
        }
    }

    private fun cancelAllRequests(device: BluetoothDevice) {
        responseFlows.keys
            .filter { it.startsWith(device.address) }
            .forEach { emitError(it, IOException("Device disconnected")) }
    }

    private fun notifyResponse(device: BluetoothDevice, raw: ByteArray) {
        val packet = try {
            MMAPacket.fromPacket(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Unknown response: ${raw.size} ${raw.toHexString()}", e)
            return
        }

        val requestId = "${device.address}-${packet.opCodeSN}"

        val flow = responseFlows[requestId]
        if (flow != null) {
            flow.tryEmit(RequestResponse.Success(packet))
            responseFlows.remove(requestId)
        } else {
            runCatching {
                when (packet.opCode) {
                    XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_INFO -> handleNotifyDeviceInfo(device, packet)
                    XIAOMI_MMA_OPCODE_NOTIFY_DEVICE_CONFIG ->
                        handleNotifyDeviceConfig(device, packet)

                    else -> Log.w(TAG, "Unknown notify: $packet")
                }
            }.onFailure {
                Log.w(TAG, "Failed to handle notify: $packet", it)
            }
        }
    }

    private fun handleNotifyDeviceInfo(device: BluetoothDevice, packet: MMAPacket) {
        if (DEBUG) Log.d(TAG, "handleNotifyDeviceInfo: ${device.address} $packet")
        require(packet is MMAPacket.Request)
        require(packet.data.size >= 2) { "Invalid device info data length" }

        val notifyType = packet.data[0]
        val value = packet.data.drop(1).toByteArray()

        when (notifyType) {
            XIAOMI_MMA_NOTIFY_TYPE_BATTERY -> {
                check(value.size >= 3) { "Not valid battery report length: ${value.size}" }

                val battery = Earbuds.fromBytes(value[0], value[1], value[2])
                dispatchEvent(DeviceEvent.BatteryChanged(device, battery))
            }

            else -> if (DEBUG) Log.d(
                TAG,
                "handleNotifyDeviceInfo: Unknown type: $notifyType, value: ${value.toHexString()}"
            )
        }
    }

    private fun handleNotifyDeviceConfig(device: BluetoothDevice, packet: MMAPacket) {
        if (DEBUG) Log.d(TAG, "handleNotifyDeviceConfig: ${device.address} $packet")
        require(packet is MMAPacket.Request)
        check(packet.data.size >= 3) { "Invalid device config data length" }

        val config = bytesToInt(packet.data[0], packet.data[1])
        val value = packet.data.drop(2).toByteArray()

        if (config == XIAOMI_MMA_CONFIG_EARBUDS_IN_EAR_MODE) {
            check(value.size == 1) { "In ear report length not 1, actual: ${value.size}" }

            val left = value[0].toInt() and (1 shl 3) != 0
            val right = value[0].toInt() and (1 shl 2) != 0

            dispatchEvent(DeviceEvent.InEarStateChanged(device, left, right))
        }

        dispatchEvent(DeviceEvent.ConfigChanged(device, config, value))
    }

    private fun emitError(requestId: String, throwable: Throwable) {
        responseFlows.remove(requestId)
            ?.tryEmit(RequestResponse.Error(throwable))
    }

    private fun isMMADevice(device: BluetoothDevice): Boolean {
        return mmaDevices.contains(device.address)
    }

    private fun getDevice(device: BluetoothDevice): MMADevice {
        return checkNotNull(mmaDevices[device.address]?.first) {
            "Device not connected: ${device.address}"
        }
    }

    private fun checkConnectionState(device: BluetoothDevice) = coroutineScope.launch {
        updateConnectStatus(device, DeviceStatus.Authing)

        runCatching {
            request(device, batteryInfo())
        }.onSuccess { battery ->
            updateConnectStatus(device, DeviceStatus.Connected)

            dispatchEvent(DeviceEvent.Connected(device))
            dispatchEvent(DeviceEvent.BatteryChanged(device, battery))
        }.onFailure { e ->
            Log.w(TAG, "Failed to verify device connection: ${device.address}", e)
            handleDeviceDisconnected(device)
        }
    }

    private fun startReading(device: BluetoothDevice) = coroutineScope.launch {
        while (isMMADevice(device) && device.isConnected) {
            val mma = runCatching { getDevice(device) }.getOrNull() ?: break
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
        BluetoothUtils.bondedDevices
            .filter { it.isConnected && BluetoothUtils.isHeadsetDevice(it) }
            .forEach { handleDeviceConnected(it) }
    }

    fun registerConnectionListener(listener: MMAListener) {
        synchronized(connectionListeners) {
            if (!connectionListeners.contains(listener)) {
                connectionListeners.add(listener)

                mmaDevices.filterValues { (_, status) ->
                    status == DeviceStatus.Connected
                }.values.forEach { (mma) ->
                    listener.onDeviceEvent(DeviceEvent.Connected(mma.device))
                }
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

    private fun dispatchEvent(event: DeviceEvent) {
        if (DEBUG) Log.d(TAG, "dispatchEvent: $event")

        coroutineScope.launch(Dispatchers.Main) {
            synchronized(connectionListeners) {
                connectionListeners.forEach { it.onDeviceEvent(event) }
            }
        }
    }

    private suspend fun sendRequestSuspend(
        device: BluetoothDevice,
        packet: MMAPacket
    ): MMAPacket? = suspendCoroutine { continuation ->
        if (packet is MMAPacket.Request) {
            packet.opCodeSN = getDevice(device).newOpCodeSN
        }
        val requestId = "${device.address}-${packet.opCodeSN}"

        val responseFlow = MutableSharedFlow<RequestResponse>(replay = 1)
        responseFlows[requestId] = responseFlow

        coroutineScope.launch {
            runCatching {
                withTimeout(TIMEOUT_MS_WRITE) {
                    getDevice(device).sendPacket(packet)
                }

                if (!packet.needReply) {
                    null
                } else {
                    waitForPacket(requestId)
                }
            }.onSuccess {
                responseFlows.remove(requestId)
                continuation.resume(it)
            }.onFailure {
                Log.e(TAG, "Failed to send packet: $device", it)

                responseFlows.remove(requestId)
                continuation.resumeWithException(it)
            }
        }
    }

    private suspend fun waitForPacket(requestId: String): MMAPacket? {
        requireNotNull(responseFlows[requestId]) { "Unknown response flow id $requestId" }

        return suspendCoroutine { continuation ->
            coroutineScope.launch {
                runCatching {
                    withTimeout(TIMEOUT_MS_READ) {
                        responseFlows[requestId]!!.first()
                    }
                }.onSuccess {
                    responseFlows.remove(requestId)

                    when (it) {
                        is RequestResponse.Success -> continuation.resume(it.packet)
                        is RequestResponse.Error -> continuation.resumeWithException(it.error)
                    }
                }.onFailure {
                    responseFlows.remove(requestId)
                    continuation.resumeWithException(it)
                }
            }
        }
    }

    suspend fun <T> request(device: BluetoothDevice, builder: RequestBuilder<T>): T {
        val packet = sendRequestSuspend(device, builder.request)

        require(packet is MMAPacket.Response) { "Reply packet not response" }
        return builder.handler(packet)
    }

    suspend fun request(device: BluetoothDevice, builder: RequestNoResponseBuilder) {
        val request = builder.request
        request.needReply = false

        sendRequestSuspend(device, request)
    }

    suspend fun request(device: BluetoothDevice, response: MMAPacket.Response) {
        sendRequestSuspend(device, response)
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
