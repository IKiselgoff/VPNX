# Android VPNX debug connect

## Изменения

- Debug Activity принимает явный флаг `connect` и вызывает штатный VPN workflow.
- Release-сборка игнорирует диагностический флаг.

## Влияние

Удалённая ADB-диагностика может запускать VPN без эмуляции касания, которая нестабильна на Samsung One UI.

## Проверки

- `git diff --check`
- Android debug build
