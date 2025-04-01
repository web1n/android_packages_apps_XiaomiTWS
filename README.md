# android_packages_apps_XiaomiTWS

Integration of Xiaomi TWS into AOSP.

## Features

 - Reports the Xiaomi TWS earbuds' volume to the Android system.
 - Configures the equalizer settings.
 - Switches between noise cancellation modes.
 - Customizes single, double, triple-click, and long-press gestures for both ears.
 - Finds the earbuds.

## Supported Models

Currently, only TWS earbuds using the Xiaomi MMA and custom Xiaomi Hands-free Profile are supported. Some earbuds may not support these two protocols and, therefore, are not compatible.

## Integration Steps

1. Add commit for `android_packages_modules_Bluetooth`: [Add support for Xiaomi TWS headset commands](https://github.com/web1n/android_packages_modules_Bluetooth/commit/0df13bbf2b70301003e422dccdd9f48520a80260).
2. Clone `android_packages_apps_XiaomiTWS` into the AOSP: `[AOSP code path]/packages/apps/XiaomiTWS/`.
3. Add `XiaomiTWS` to your device tree:
   ```makefile
   PRODUCT_PACKAGES += \
       XiaomiTWS
   ```

## Screenshots

![battery](.assets/battery.png)
![configs](.assets/configs.png)

## Credits

* [packages_apps_BtHelper](https://github.com/TheParasiteProject/packages_apps_BtHelper)
