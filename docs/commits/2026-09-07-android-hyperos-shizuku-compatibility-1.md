# Android HyperOS Shizuku Compatibility

## Изменение
Добавлены Shizuku remote-process fallback для Xiaomi/MediaTek HyperOS, двойной watchdog `shizuku_server` и direct-network binding SSH; версия VPNX повышена до 1.2.3.

## Причина
На Android 14/HyperOS Shizuku принимал разрешение VPNX, но запуск UserService завершался `NullPointerException` внутри OEM-реализации `LoadedApk.makeApplicationInner`.

## Влияние
Команды по-прежнему выполняются только после отдельного разрешения Shizuku, под UID `shell`, с прежними лимитами времени и вывода. SSH maintenance обходит собственный VPN через validated non-VPN network; внешние порты и control-контракт не изменены.

## Проверка
Сборка APK, обновление установленного приложения и фактическая проверка shell UID через Shizuku.
