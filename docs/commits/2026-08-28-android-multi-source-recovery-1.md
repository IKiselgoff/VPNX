# Android multi-source recovery

## Изменения

- Maintenance recovery продублирован через AlarmManager и основной VPN service.
- SSH-сессии получили ограниченный socket timeout для выхода из полумёртвого состояния.
- Boot/package recovery перепланирует следующий alarm и не прерывается при единичном запрете foreground start.

## Влияние

Удалённый control и ADB forward больше не зависят только от JobScheduler или сохранения одного foreground-процесса.

## Проверки

- Android debug build.
- Полный recovery-тест выполняется после следующего появления планшета на VPS.
