#!/usr/bin/env bash
# Wrap x86_64 native ELF binaries with qemu so they run on this arm64 host.
set -euo pipefail
export QEMU_LD_PREFIX=/usr/x86_64-linux-gnu

wrap_dir() {
  local dir="$1"
  [ -d "$dir" ] || return 0
  while IFS= read -r -d '' f; do
    [ -f "$f" ] || continue
    case "$f" in *.real|*.sh) continue ;; esac
    [ -x "$f" ] || continue
    [ -f "$f.real" ] && continue
    local machine
    machine=$(python3 - "$f" <<'PY'
import struct, sys
try:
    with open(sys.argv[1],'rb') as fh:
        h=fh.read(20)
    if h[:4]!=b'\x7fELF' or len(h)<20:
        print('no')
    else:
        print(struct.unpack('<H', h[18:20])[0])
except Exception:
    print('no')
PY
)
    if [ "$machine" = "62" ]; then
      mv "$f" "$f.real"
      cat > "$f" <<WRAP
#!/bin/sh
export QEMU_LD_PREFIX=/usr/x86_64-linux-gnu
exec /usr/bin/qemu-x86_64 "\$(dirname "\$0")/$(basename "$f").real" "\$@"
WRAP
      chmod +x "$f"
      echo "wrapped $f"
    fi
  done < <(find "$dir" -type f -print0)
}

wrap_dir /opt/android-sdk/build-tools
wrap_dir /opt/android-sdk/platform-tools
# Gradle extracts aapt2 from Maven into its cache; wrap those too.
[ -d /root/.gradle/caches ] && wrap_dir /root/.gradle/caches
echo DONE
