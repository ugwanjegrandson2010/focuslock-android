#!/usr/bin/env bash
set -euo pipefail
export QEMU_LD_PREFIX=/usr/x86_64-linux-gnu
APK=$(find /root/focuslock/app/build/outputs/apk -name '*.apk' -type f | sort | tail -1)
if [ -z "$APK" ]; then echo "NO APK FOUND"; exit 1; fi
echo "APK: $APK"
ls -lh "$APK"
sha256sum "$APK"
unzip -t "$APK" >/dev/null && echo "ZIP integrity: OK"
AAPT=/opt/android-sdk/build-tools/35.0.0/aapt2
echo "--- badging ---"
"$AAPT" dump badging "$APK" 2>/dev/null | grep -E '^(package|sdkVersion|targetSdkVersion|uses-permission|application):' | head -30
echo "--- manifest services/receivers ---"
"$AAPT" dump xmltree --file AndroidManifest.xml "$APK" 2>/dev/null | grep -E 'E: (service|receiver|activity)|A: android:name' | head -80
