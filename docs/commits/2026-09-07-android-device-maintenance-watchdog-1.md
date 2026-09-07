# Commit: android-device-maintenance-watchdog

## Изменения

- Добавлены две взаимно контролирующие shell-watchdog-копии для OEM recovery.
- Recovery сначала запускает persisted VPNX job и использует launcher только как fallback.
- Журнал ограничен 64 KiB.

## Затронутые модули

- `android-vpnx`

## Проверка

- Обе watchdog-копии должны присутствовать в `ps`.
- После завершения одной копии peer обязан восстановить её.
