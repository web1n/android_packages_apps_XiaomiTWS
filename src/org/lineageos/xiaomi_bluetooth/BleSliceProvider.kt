package org.lineageos.xiaomi_bluetooth

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.graphics.drawable.IconCompat
import androidx.slice.Slice
import androidx.slice.SliceProvider
import androidx.slice.builders.ListBuilder
import androidx.slice.builders.SliceAction
import org.lineageos.xiaomi_bluetooth.fragments.EarbudsInfoFragment
import org.lineageos.xiaomi_bluetooth.utils.BluetoothUtils

class BleSliceProvider : SliceProvider() {

    override fun onCreateSliceProvider() = true

    @SuppressLint("Slices")
    override fun onBindSlice(sliceUri: Uri): Slice {
        if (DEBUG) Log.d(TAG, "onBindSlice: $sliceUri")
        val device = getBluetoothDevice(sliceUri)

        return ListBuilder(requireContext(), sliceUri, ListBuilder.INFINITY)
            .addRow(createSettingsActionBuilder(device))
            .build()
    }

    private fun createSettingsActionBuilder(device: BluetoothDevice): ListBuilder.RowBuilder {
        val pendingIntent = Intent(EarbudsInfoFragment.ACTION_EARBUDS_INFO).apply {
            putExtra(BluetoothDevice.EXTRA_DEVICE, device)
            setPackage(requireContext().packageName)
        }.let {
            PendingIntent.getActivity(
                context, device.address.hashCode(), it, PendingIntent.FLAG_IMMUTABLE
            )
        }

        val sliceAction = SliceAction.create(
            pendingIntent,
            IconCompat.createWithResource(requireContext(), R.drawable.ic_settings_24dp),
            ListBuilder.ICON_IMAGE, requireContext().getString(R.string.app_name)
        )

        return ListBuilder.RowBuilder()
            .setTitle(requireContext().getString(R.string.earbuds_settings))
            .setPrimaryAction(sliceAction)
    }

    companion object {
        private val TAG = BleSliceProvider::class.java.simpleName
        private const val DEBUG = true

        private const val AUTHORITY_BLE_SLICE = "org.lineageos.xiaomi_bluetooth.ble-slice"
        private const val KEY_MAC_ADDRESS = "mac"

        private fun getBluetoothDevice(uri: Uri): BluetoothDevice {
            val macAddress = uri.getQueryParameter(KEY_MAC_ADDRESS)!!
            if (DEBUG) Log.d(TAG, "getBluetoothDevice: address: $macAddress")

            return BluetoothUtils.getBluetoothDevice(macAddress)
        }

        fun generateSliceUri(macAddress: String): Uri {
            return Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(AUTHORITY_BLE_SLICE)
                .appendQueryParameter(KEY_MAC_ADDRESS, macAddress)
                .build()
        }
    }

}
