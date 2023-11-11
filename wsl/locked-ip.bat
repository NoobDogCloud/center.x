@echo off
setlocal enabledelayedexpansion

wsl --shutdown ubuntu
wsl -u root service ssh start
wsl -u root service docker start | findstr "Starting Docker" > nul
if !errorlevel! equ 0 (
    echo docker start success
    wsl -u root ip addr | findstr "192.168.120.181" > nul
    if !errorlevel! equ 0 (
        echo wsl ip has set
    ) else (
        wsl -u root ip addr add 192.168.120.181/24 broadcast 192.168.120.0 dev eth0 label eth0:1
        echo set wsl ip success: 192.168.120.181
    )

    ipconfig | findstr "192.168.120.100" > nul
    if !errorlevel! equ 0 (
        echo windows ip has set
    ) else (
        netsh interface ip add address "vEthernet (WSL)" 192.168.120.100 255.255.255.0
        echo set windows ip success: 192.168.120.100
    )
)
pause