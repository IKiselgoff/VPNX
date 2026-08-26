# Android libXray bridge API

## Изменения

- Запуск переведён на `runXrayFromJson` с полем `configJSON`, а запуск и остановка используют bridge API v1, поддерживаемый libXray v26.7.28.

## Влияние

Устранён отказ `unsupported apiVersion` при запуске Android VPN engine.

## Проверки

- `git diff --check`
- Android debug build
- Запуск VPNX на SM-X205
