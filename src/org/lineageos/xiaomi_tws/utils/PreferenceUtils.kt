package org.lineageos.xiaomi_tws.utils

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.annotation.XmlRes
import androidx.preference.Preference
import org.lineageos.xiaomi_tws.configs.BaseConfigController
import org.xmlpull.v1.XmlPullParser

object PreferenceUtils {

    private const val NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"
    private const val NAMESPACE_APP = "http://schemas.android.com/apk/res-auto"

    private const val NAME_KEY = "key"
    private const val NAME_CONTROLLER = "controller"

    fun createAllControllers(
        context: Context,
        @XmlRes xmlResId: Int,
        device: BluetoothDevice
    ): Set<BaseConfigController<Preference>> {
        val configControllers = HashSet<BaseConfigController<Preference>>()

        runCatching {
            context.resources.getXml(xmlResId).use { parser ->
                var eventType: Int
                while ((parser.next().also { eventType = it }) != XmlPullParser.END_DOCUMENT) {
                    if (eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    val key = parser.getAttributeValue(NAMESPACE_ANDROID, NAME_KEY) ?: continue
                    val controller =
                        parser.getAttributeValue(NAMESPACE_APP, NAME_CONTROLLER) ?: continue

                    configControllers.add(createControllerInstance(controller, key, device))
                }
            }

            configControllers
        }.onFailure {
            throw RuntimeException("Error parsing XML or instantiating controller", it)
        }

        return configControllers
    }

    private fun createControllerInstance(
        controllerClass: String,
        preferenceKey: String,
        device: BluetoothDevice
    ): BaseConfigController<Preference> {
        val instance = Class.forName(controllerClass)
            .getConstructor(String::class.java, BluetoothDevice::class.java)
            .newInstance(preferenceKey, device)

        @Suppress("UNCHECKED_CAST")
        return instance as BaseConfigController<Preference>
    }

}
