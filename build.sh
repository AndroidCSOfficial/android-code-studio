#!/usr/bin/env bash

# 检查参数数量
if [ "$#" -ne 2 ]; then
  echo "Usage: $0 <module> <variant>"
  echo "Eg: $0 core:app Debug"
  exit 1
fi

MODULE="$1"
VARIANT="$2"

# 构造命令
CMD="gradlew :${MODULE}:assemble${VARIANT}"

echo "Exec: $CMD"
# 执行构建并保存日志
$CMD 2>&1 | tee build.log