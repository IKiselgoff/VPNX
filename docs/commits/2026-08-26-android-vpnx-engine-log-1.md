# Android VPNX engine diagnostics

## Изменения

- Ошибка запуска Android TUN или libXray теперь записывается в `logcat` с полным stack trace.

## Влияние

Удалённая диагностика различает отказ системного VPN, преобразования конфигурации и Xray runtime.

## Проверки

- `git diff --check`
- Android debug build
