# Android HyperOS Shizuku Compatibility

## Изменение
Добавлены Shizuku remote-process fallback для Xiaomi/MediaTek HyperOS и двойной watchdog `shizuku_server`; версия VPNX повышена до 1.2.2.

## Причина
На Android 14/HyperOS Shizuku принимал разрешение VPNX, но запуск UserService завершался `NullPointerException` внутри OEM-реализации `LoadedApk.makeApplicationInner`.

## Влияние
Команды по-прежнему выполняются только после отдельного разрешения Shizuku, под UID `shell`, с прежними лимитами времени и вывода. VPN, BIRD и maintenance-контракты не изменены.

## Проверка
Сборка APK, обновление установленного приложения и фактическая проверка shell UID через Shizuku.
