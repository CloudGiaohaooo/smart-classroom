@echo off
rem Windows 本地构建脚本：把 esbuild 的临时目录重定向到项目目录内。
rem 背景：部分第三方安全软件（杀毒 / EDR）会拦截 esbuild 0.21.x 原生二进制
rem 写入 %%LOCALAPPDATA%%\Temp，导致 `vite build` 报
rem   "remove ...\Temp\esbuild-<hash>: Access is denied"。
rem 该二进制可正常写入项目目录本身，因此把 TMP/TEMP/TMPDIR 指到
rem 项目内的 .build-tmp（已在 .gitignore 中排除）即可绕过。
rem Linux / GitHub Actions CI 不受影响，直接 `npm run build` 即可。
set "TMP=%~dp0.build-tmp"
set "TEMP=%~dp0.build-tmp"
set "TMPDIR=%~dp0.build-tmp"
if not exist "%TMP%" mkdir "%TMP%"
call npm run build
