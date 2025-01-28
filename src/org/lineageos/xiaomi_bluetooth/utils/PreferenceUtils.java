package org.lineageos.xiaomi_bluetooth.utils;

import android.content.Context;
import android.content.res.XmlResourceParser;

import androidx.annotation.NonNull;
import androidx.annotation.XmlRes;

import org.lineageos.xiaomi_bluetooth.settings.configs.ConfigController;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PreferenceUtils {

    private static final String NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";
    private static final String NAMESPACE_APP = "http://schemas.android.com/apk/res-auto";

    private static final String NAME_KEY = "key";
    private static final String NAME_CONTROLLER = "controller";

    private static final String PREFERENCE_TAG_SUFFIX = "Preference";

    @NonNull
    public static Set<ConfigController> createAllControllers(@NonNull Context context,
                                                             @XmlRes int xmlResId) {
        Set<ConfigController> configControllers = new HashSet<>();

        try (XmlResourceParser parser = context.getResources().getXml(xmlResId)) {
            int eventType;
            while ((eventType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (eventType != XmlPullParser.START_TAG) {
                    continue;
                }
                String tagName = parser.getName();
                String key = parser.getAttributeValue(NAMESPACE_ANDROID, NAME_KEY);
                String controller = parser.getAttributeValue(NAMESPACE_APP, NAME_CONTROLLER);

                if (isPreferenceTag(tagName) && controller != null && key != null) {
                    configControllers.add(
                            createControllerInstance(context, controller, key));
                }
            }
        } catch (XmlPullParserException | IOException | ReflectiveOperationException e) {
            throw new RuntimeException("Error parsing XML or instantiating controller", e);
        }

        return configControllers;
    }

    private static boolean isPreferenceTag(@NonNull String tagName) {
        return tagName.endsWith(PREFERENCE_TAG_SUFFIX);
    }

    @NonNull
    private static ConfigController createControllerInstance(
            @NonNull Context context, @NonNull String controllerClass, @NonNull String preferenceKey
    ) throws ReflectiveOperationException {
        Object instance = Class.forName(controllerClass)
                .getConstructor(Context.class, String.class)
                .newInstance(context, preferenceKey);

        if (instance instanceof ConfigController controller) {
            return controller;
        } else {
            throw new IllegalStateException(controllerClass + " is not valid ConfigController");
        }
    }

}
