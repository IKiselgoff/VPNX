# Android ADB reverse access

## Изменения

- Добавлен отдельный macOS LaunchAgent для reverse-forward ADB через VPS.
- VPS endpoint ограничен `127.0.0.1:25555`, поэтому ADB не публикуется в интернет.
- Зафиксированы эксплуатационные ограничения ADB TCP без root.

## Влияние

Планшет доступен для диагностического ADB shell через существующий Mac/VPS-мост без USB-кабеля.

## Проверки

- Wireless ADB возвращает `WIFI_ADB_OK`.
- Always-on package и Doze whitelist подтверждены системными настройками Android.
