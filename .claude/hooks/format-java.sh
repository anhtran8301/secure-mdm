#!/usr/bin/env bash
# Stop: run Spotless once over Java files changed in be/ (staged, unstaged or untracked).
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
files="$( { git diff --name-only HEAD -- 'be/*.java'; git ls-files --others --exclude-standard -- 'be/*.java'; } 2>/dev/null | sort -u | sed "s|^|$root/|" | paste -sd, -)"
[ -z "$files" ] && exit 0
cd be && ./mvnw -q spotless:apply -DspotlessFiles="$files"
