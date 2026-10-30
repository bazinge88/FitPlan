#!/bin/sh
# 跨 agent 串行化 Gradle：同一工作区只允许一个 Gradle 构建。
# 用法：sh .scratch/iteration11/with-gradle-lock.sh :app:testDebugUnitTest --console=plain
set -u
ROOT=/Users/suyi/projects/exercise
LOCK="$ROOT/.scratch/iteration11/.gradle-lock"
i=0
while ! mkdir "$LOCK" 2>/dev/null; do
  i=$((i+1))
  if [ "$i" -ge 600 ]; then
    echo "with-gradle-lock: timeout waiting for $LOCK" >&2
    exit 75
  fi
  sleep 2
done
cleanup() { rmdir "$LOCK" 2>/dev/null || true; }
trap cleanup EXIT INT TERM
cd "$ROOT/FitPlan" || exit 1
GRADLE_USER_HOME="$ROOT/.gradle-home" ./gradlew --offline "$@"
status=$?
exit $status
