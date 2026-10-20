@echo off
setlocal enabledelayedexpansion

rem ============================================================
rem  优达智慧课堂教学管理平台 - 一键停止演示脚本
rem  按端口 8080（后端）/ 5173（前端）找到监听进程并终止
rem ============================================================

echo ============================================
echo   优达智慧课堂 - 停止演示环境
echo ============================================
echo.

echo 正在停止后端（8080）与前端（5173）...
set "KILLED="
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    taskkill /PID %%p /F >nul 2>&1 && set "KILLED=!KILLED! 8080"
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5173 " ^| findstr "LISTENING"') do (
    taskkill /PID %%p /F >nul 2>&1 && set "KILLED=!KILLED! 5173"
)

if defined KILLED (
    echo 已停止端口：!KILLED!
) else (
    echo 未发现正在运行的后端（8080）或前端（5173）。
)

echo.
echo 提示：若仍残留 cmd 窗口，请手动关闭。
pause