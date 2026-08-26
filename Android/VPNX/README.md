# VPNX for Android

Нативный BIRD VPN-клиент для Android 8+.

## Возможности

- полный JSON snapshot подписки Happ BIRD;
- официальный libXray/Xray-core;
- Android `VpnService` и прямой Xray TUN;
- выбор профиля с исходными названиями Happ;
- синхронизация при запуске, каждые 15 минут и после возвращения сети;
- автозапуск ранее активного VPN после перезагрузки;
- встроенная проверка внешнего IP и Telegram.

## Сборка

```bash
./scripts/download-runtime.sh
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`libXray.aar`, `geoip.dat` и `geosite.dat` скачиваются воспроизводимым скриптом и не хранятся в Git.
