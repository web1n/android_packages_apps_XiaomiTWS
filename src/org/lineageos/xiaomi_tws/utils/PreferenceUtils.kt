package org.lineageos.xiaomi_tws.utils

import android.content.Context
import androidx.annotation.XmlRes
import org.lineageos.xiaomi_tws.configs.ConfigController
import org.xmlpull.v1.XmlPullParser

object PreferenceUtils {

    private const val NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"
    private const val NAMESPACE_APP = "http://schemas.android.com/apk/res-auto"

    private const val NAME_KEY = "key"
    private const val NAME_CONTROLLER = "controller"

    private const val PREFERENCE_TAG_SUFFIX = "Preference"

    fun createAllControllers(context: Context, @XmlRes xmlResId: Int): Set<ConfigController> {
        val configControllers = HashSet<ConfigController>()

        runCatching {
            context.resources.getXml(xmlResId).use { parser ->
                var eventType: Int
                while ((parser.next().also { eventType = it }) != XmlPullParser.END_DOCUMENT) {
                    if (eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    val tagName = parser.name ?: continue
                    val key = parser.getAttributeValue(NAMESPACE_ANDROID, NAME_KEY) ?: continue
                    val controller =
                        parser.getAttributeValue(NAMESPACE_APP, NAME_CONTROLLER) ?: continue

                    if (isPreferenceTag(tagName)) {
                        configControllers.add(createControllerInstance(context, controller, key))
                    }
                }
            }

            configControllers
        }.onFailure {
            throw RuntimeException("Error parsing XML or instantiating controller", it)
        }

        return configControllers
    }

    private fun isPreferenceTag(tagName: String): Boolean {
        return tagName.endsWith(PREFERENCE_TAG_SUFFIX)
    }

    private fun createControllerInstance(
        context: Context,
        controllerClass: String,
        preferenceKey: String
    ): ConfigController {
        val instance = Class.forName(controllerClass)
            .getConstructor(Context::class.java, String::class.java)
            .newInstance(context, preferenceKey)

        check(instance is ConfigController) {
            "$controllerClass is not valid ConfigController"
        }
        return instance
    }

}
