#!/usr/bin/env bash
# 导出真实运行数据（用于替换演示数据 / 重跑雷同评测）。
#
# 前置：docker compose 栈已启动（youda-mysql 容器在跑）。
# 用法：bash scripts/export-real-data.sh [输出目录]
#       默认输出目录 data/real-YYYYMMDD-HHMMSS
#
# 导出表：sys_user / course / course_enrollment / assignment / submission / course_resource / student_behavior
#
# 注意：真实数据含姓名 / 学号 / 邮箱等敏感信息，导出后请**先脱敏**再入库或提交
#       （脱敏步骤见 docs/evaluation/真实数据采集步骤.md），切勿直接提交到公开仓库。
set -euo pipefail

DOCKER="${DOCKER:-docker}"
MYSQL_CONTAINER="youda-mysql"
DB="youda_classroom"
MYSQL_USER="youda"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-youda_pass_2026}"
OUT_DIR="${1:-data/real-$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$OUT_DIR"

TABLES=(sys_user course course_enrollment assignment submission course_resource student_behavior)

for t in "${TABLES[@]}"; do
  echo "导出 ${t} ..."
  "$DOCKER" exec "$MYSQL_CONTAINER" sh -c \
    "mysqldump -u${MYSQL_USER} -p${MYSQL_PASSWORD} --no-create-info --skip-add-locks --complete-insert --skip-comments ${DB} ${t}" \
    > "$OUT_DIR/${t}.sql"
done

echo "导出完成：$OUT_DIR"
echo "下一步：对 sys_user 的姓名/学号/邮箱做脱敏，再按 docs/evaluation/真实数据采集步骤.md 导入或重跑评测。"
