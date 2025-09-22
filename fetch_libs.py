#!/bin/env python3
import os
import tempfile
import zipfile

import requests

DOWNLOAD_URL = "https://cn.tws.wear.mi.com/twswear/download"

FILES_TO_EXTRACT = {
    "lib/armeabi-v7a/libxm_bluetooth.so": "jniLibs/armeabi-v7a/libxm_bluetooth.so",
    "lib/arm64-v8a/libxm_bluetooth.so": "jniLibs/arm64-v8a/libxm_bluetooth.so"
}


def main():
    with tempfile.TemporaryFile() as temp_apk_file:
        result = requests.get(DOWNLOAD_URL)
        result.raise_for_status()

        temp_apk_file.write(result.content)

        with zipfile.ZipFile(temp_apk_file, "r") as zip_ref:
            for source_path, target_path in FILES_TO_EXTRACT.items():
                os.makedirs(os.path.dirname(target_path), exist_ok=True)

                with open(target_path, "wb") as f:
                    f.write(zip_ref.read(source_path))


if __name__ == "__main__":
    main()
