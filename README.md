# android_packages_apps_XiaomiTWS

A comprehensive Android application that seamlessly integrates Xiaomi TWS (True Wireless Stereo) earbuds with AOSP-based ROMs, providing enhanced functionality and user experience.

## Features

- **Battery Monitoring**: Real-time battery level reporting for both earbuds and charging case
- **Audio Enhancement**: Advanced equalizer configuration with multiple presets
- **Automatic Media Switching**: Intelligent audio switching - switches to local audio when both earbuds are removed, switches back to earbuds when either earbud is worn
- **Automatic Device Switching**: Automatically take over earbuds from other connected devices when playing media on this device
- **Noise Control**: Intelligent switching between noise cancellation and transparency modes
- **Gesture Customization**: Full customization of touch controls including:
  - Single tap, double tap, triple tap actions
  - Long press gestures
  - Independent configuration for left and right earbuds
- **Find My Earbuds**: Locate misplaced earbuds with audio alerts

## Compatibility

### Verified Compatible Models

|  Brand | Model | Status |
|--------|-------|--------|
| Xiaomi | Air 4 SE | ✅ Fully Tested |
| REDMI  | Buds 6   | ✅ Fully Tested |
| REDMI  | Buds 7S  | ✅ Fully Tested |

> **Note**: Other Xiaomi/REDMI TWS models may also be compatible but have not been extensively tested.

## Integration Steps

1. Clone `android_packages_apps_XiaomiTWS` into the AOSP: `[AOSP code path]/packages/apps/XiaomiTWS/`.
2. Add `XiaomiTWS` to your device tree:
   ```makefile
   PRODUCT_PACKAGES += \
       XiaomiTWS
   ```
3. [OPTIONAL] Add commit for `android_packages_modules_Bluetooth`: [Add support for Xiaomi TWS headset commands](https://github.com/web1n/android_packages_modules_Bluetooth/commit/0df13bbf2b70301003e422dccdd9f48520a80260).

> **Note**: Without this commit, most features will still work except for:
> - Automatic Device Switching

## Screenshots

| Battery Status | Configuration Panel |
|:--------------:|:------------------:|
| ![Battery Status](.assets/battery.png) | ![Configuration Options](.assets/configs.png) |

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Credits

* [packages_apps_BtHelper](https://github.com/TheParasiteProject/packages_apps_BtHelper)
