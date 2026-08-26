# Android libXray bridge API

## Изменения

- Вызовы `runXray` и `stopXray` переведены на bridge API v1, поддерживаемый libXray v26.7.28.

## Влияние

Устранён отказ `unsupported apiVersion` при запуске Android VPN engine.

## Проверки

- `git diff --check`
- Android debug build
- Запуск VPNX на SM-X205
