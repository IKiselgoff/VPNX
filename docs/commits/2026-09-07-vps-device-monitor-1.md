# Commit: vps-device-monitor

## Изменения

- Добавлен постоянный systemd-monitor Android endpoint на VPS.
- ADB listener, реальный shell и authenticated control проверяются раздельно.

## Затронутые модули

- `android-vpnx`

## Проверка

- Monitor обязан показывать модель устройства только после успешного shell.
- Проверка не содержит транспортных переключений и restart/kill-команд.
