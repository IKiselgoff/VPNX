# BIRD mirror timer recovery

## Изменения
- `OnUnitActiveSec` заменён на календарное расписание каждые 15 минут.

## Влияние
После включения ранее отключённого timer systemd назначает следующий запуск, а не оставляет `Trigger: n/a`.

## Проверка
- `systemctl list-timers vpnx-bird-mirror.timer` показывает NEXT.
- Сервис читает только локальный snapshot, не обращаясь к Happ-подписке.
