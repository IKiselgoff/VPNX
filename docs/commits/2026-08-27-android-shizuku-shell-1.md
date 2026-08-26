# Android Shizuku shell

## Изменения

- Добавлены официальный Shizuku provider/API и shell UserService.
- VPNX показывает состояние расширенной диагностики и запрашивает разрешение по нажатию.
- Диагностика выполняет ограниченную команду под UID Shizuku и включает результат `id` в отчёт.

## Влияние

VPNX получает расширенные возможности диагностики Android без root, разблокировки загрузчика и открытого shell-порта.

## Проверки

- Сборка Android debug APK с libXray и Shizuku API.
- Запуск официального Shizuku 13.6.0 под UID `shell` на Samsung SM-X205 / Android 14.
