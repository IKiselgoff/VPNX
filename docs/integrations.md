# Integrations

## BIRD VPN
VPNX читает полный JSON snapshot из `https://moonshard.org/_DDgzQApDZfjQ2JA`. Интеграция использует Happ-compatible request headers и не зависит от запущенного приложения Happ.

Android-клиент использует тот же endpoint и сохраняет последний валидный snapshot в приватном app storage.

## Shizuku
Android VPNX использует официальный Shizuku для расширенной no-root диагностики. Binder и разрешения остаются локальными на планшете; наружу Shizuku или shell не публикуются. При запуске через ADB/Android Wireless debugging UserService работает с UID `shell`, а не root.

## Android maintenance control
Планшет устанавливает на VPS два SSH reverse-forward: ADB `127.0.0.1:25556` и control `127.0.0.1:25557`. Control-forward защищён отдельным токеном и принимает только фиксированный набор recovery-команд. SSH authorized key ограничен `permitlisten` для этих двух loopback endpoint.

## XTLS libXray
Android VPNX загружает закреплённый официальный `libXray v26.7.28`; geo assets берутся из официального Xray Android release. Runtime dependency воспроизводится `Android/VPNX/scripts/download-runtime.sh`.
