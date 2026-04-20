package org.lineageos.xiaomi_tws.headset.commands

import android.util.Log
import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData.Status
import org.lineageos.xiaomi_tws.mma.configs.InEarState
import org.lineageos.xiaomi_tws.mma.configs.NoiseCancellationMode
import org.lineageos.xiaomi_tws.utils.ByteUtils.parseTLVMap

object StatusCommand : Command<Status>() {

    override val commandType: Byte = 0x01
    override val payloadType: Byte = 0x00

    private val TAG = StatusCommand::class.java.simpleName

    object StatusType {
        const val ANC: Byte = 0x04
        const val IN_EAR: Byte = 0x0A
    }

    override fun decode(payload: Payload): Status {
        val tlvMap = parseTLVMap(payload.value, singleByteTag = true)
        val raw = tlvMap.mapKeys { it.key.toByte() }.toMutableMap()

        val anc = raw.remove(StatusType.ANC)
            ?.firstOrNull()
            ?.let { NoiseCancellationMode.Mode.fromByte(it) }

        val inEar = raw.remove(StatusType.IN_EAR)
            ?.let { runCatching { InEarState.parseConfigValue(it) }.getOrNull() }

        if (raw.isNotEmpty()) {
            val valueString = raw.entries
                .joinToString { "${it.key}: ${it.value.contentToString()}" }
            Log.d(TAG, "Parsed extra raw data: $valueString")
        }

        return Status(
            anc = anc,
            inEar = inEar,
            raw = raw.toMap()
        )
    }
}
