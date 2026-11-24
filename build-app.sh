#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-arm64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:/opt/gradle/gradle-8.9/bin:$PATH"
export QEMU_LD_PREFIX=/usr/x86_64-linux-gnu
cd /root/focuslock
./wrap-x86.sh
/opt/gradle/gradle-8.9/bin/gradle test assembleDebug assembleRelease --no-daemon --console=plain --stacktrace
