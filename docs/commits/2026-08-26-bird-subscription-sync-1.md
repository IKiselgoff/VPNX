# BIRD subscription sync

## Изменения
Добавлен автономный импорт полного BIRD snapshot в VPNX и LaunchAgent для запуска при входе и каждые 15 минут.

## Затронутые модули
- `bird-subscription-sync`
- установщик VPNX

## Поведение
Полные Happ runtime-конфиги сохраняются без преобразования через VLESS URL, включая multi-outbound Auto, `leastLoad`, `burstObservatory`, DNS и routing. Каталог узлов полностью зеркалирует Happ BIRD, межпроцессная блокировка исключает одновременную запись, меню использует исходные названия подписки, активный изменившийся профиль автоматически перезапускается.

## Проверка
- Python compile check
- shell syntax check
- plist validation
- live BIRD subscription sync
