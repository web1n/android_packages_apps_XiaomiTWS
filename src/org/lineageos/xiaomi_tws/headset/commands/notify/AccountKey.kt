package org.lineageos.xiaomi_tws.headset.commands.notify

import org.lineageos.xiaomi_tws.headset.ATCommand.Payload
import org.lineageos.xiaomi_tws.headset.CommandData.Notify.AccountKey
import org.lineageos.xiaomi_tws.headset.commands.NotifyCommand

object AccountKey : NotifyCommand<AccountKey>() {

    override val payloadType: Byte = 0x03

    private const val ACCOUNT_KEY_LENGTH: Int = 16

    override fun decode(payload: Payload): AccountKey {
        ensureValidAccountKey(payload.value)
        return AccountKey(payload.value)
    }

    override fun encode(value: AccountKey): Payload {
        ensureValidAccountKey(value.key)
        return Payload(payloadType, value.key)
    }

    private fun ensureValidAccountKey(key: ByteArray) {
        if (key.size != ACCOUNT_KEY_LENGTH) {
            throw IllegalArgumentException("Account key must be exactly $ACCOUNT_KEY_LENGTH bytes, got ${key.size} bytes")
        }
    }
}
