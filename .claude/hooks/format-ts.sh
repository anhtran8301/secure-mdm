#!/usr/bin/env bash
# PostToolUse: format an edited fe/ or simulator/ TypeScript file with Prettier.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
file="$(jq -r '.tool_response.filePath // .tool_input.file_path // empty')"
case "$file" in
  "$root"/fe/*.ts|"$root"/fe/*.tsx) prettier="$root/fe/node_modules/.bin/prettier" ;;
  "$root"/simulator/*.ts) prettier="$root/node_modules/.bin/prettier" ;;
  *) exit 0 ;;
esac
[ -x "$prettier" ] && [ -f "$file" ] && "$prettier" --write --log-level warn "$file"
