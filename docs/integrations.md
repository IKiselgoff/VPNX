# Integrations

## BIRD VPN
VPNX читает полный JSON snapshot из `https://moonshard.org/_DDgzQApDZfjQ2JA`. Интеграция использует Happ-compatible request headers и не зависит от запущенного приложения Happ.

Android-клиент использует тот же endpoint и сохраняет последний валидный snapshot в приватном app storage.

## Shizuku
Android VPNX использует официальный Shizuku для расширенной no-root диагностики. Binder и разрешения остаются локальными на планшете; наружу Shizuku или shell не публикуются. При запуске через ADB/Android Wireless debugging UserService работает с UID `shell`, а не root.

## XTLS libXray
Android VPNX загружает закреплённый официальный `libXray v26.7.28`; geo assets берутся из официального Xray Android release. Runtime dependency воспроизводится `Android/VPNX/scripts/download-runtime.sh`.
