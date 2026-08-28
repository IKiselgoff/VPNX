# Android maintenance control

## Изменения

- Добавлен независимый token-authenticated reverse-forward `25557` с allowlist recovery-команд.
- Добавлен минутный watchdog VPN, BIRD sync и Shizuku binder.
- Восстановление ADB TCP разрешено только через рабочий control-канал и Shizuku UserService.

## Влияние

Диагностика и базовое восстановление планшета больше не зависят от работоспособности ADB-forward; Mac остаётся аварийным резервом.

## Проверки

- Android debug build.
- SSH key ограничен loopback-портами `25556` и `25557`.
- `STATUS` и `SYNC` успешно выполнены через control-forward; snapshot содержит 12 профилей.
- После `RESTART_VPN` оба reverse-forward восстановились автоматически.
- ADB, Shizuku UID shell, DNS BIRD/Telegram и `tun0` подтверждены после восстановления.
