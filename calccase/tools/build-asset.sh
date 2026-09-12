#!/usr/bin/env bash
# Wrap the Artifact-style page fragment into a standalone document for the APK.
#
# The Artifact host supplies <!doctype>, <head> and a small reset at publish
# time, so case-rig.html carries none of its own. A WebView gets the raw file,
# so the same skeleton has to be baked in here — keep the two in step by
# regenerating rather than hand-editing app/src/main/assets/index.html.
set -euo pipefail

SRC="${1:-../case-rig.html}"
OUT="${2:-app/src/main/assets/index.html}"

[ -f "$SRC" ] || { echo "source not found: $SRC" >&2; exit 1; }

{
  cat <<'HEAD'
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover">
<meta name="color-scheme" content="light dark">
<style>
:root{color-scheme:light dark}
html,body{margin:0;padding:0;height:100%;overflow:hidden;overscroll-behavior:none}
body{font:14px system-ui,-apple-system,sans-serif;background:#D5D7CE}
img{max-width:100%}
[hidden]{display:none!important}
</style>
</head>
<body>
HEAD
  cat "$SRC"
  cat <<'TAIL'
</body>
</html>
TAIL
} > "$OUT"

echo "wrote $OUT ($(wc -c < "$OUT") bytes)"
