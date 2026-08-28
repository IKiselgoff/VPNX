# Android maintenance recovery

## Изменения

- Control connections обслуживаются параллельно и имеют независимый timeout.
- SSH-сессия проверяется активным keepalive каждые 15 секунд.
- Persisted BIRD Job повторно запускает maintenance foreground service.

## Влияние

Зависшая control-сессия и выгрузка процесса Android больше не требуют ручного открытия VPNX для восстановления каналов.

## Проверки

- Сценарий сформирован по фактическому зависанию обоих forward при сохранённых listener на VPS.
