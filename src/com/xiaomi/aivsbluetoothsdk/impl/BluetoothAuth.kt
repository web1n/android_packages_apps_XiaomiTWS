package com.xiaomi.aivsbluetoothsdk.impl

@Suppress("unused")
object BluetoothAuth {

    init {
        System.loadLibrary("xm_bluetooth")
        nativeInit()
    }

    private external fun nativeInit(): Boolean

    external fun getEncryptedAuthCheckData(data: ByteArray): ByteArray
    external fun getEncryptedAuthData(data: ByteArray): ByteArray
    external fun getRandomAuthCheckData(): ByteArray
    external fun getRandomAuthData(): ByteArray

    external fun setLinkKey(data: ByteArray): Int

}
