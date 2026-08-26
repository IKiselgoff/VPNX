# Android direct maintenance tunnel

## Изменения

- Добавлен foreground-сервис прямого SSH reverse-forward с планшета на VPS.
- Сервис запускается при открытии VPNX и после загрузки Android, использует keepalive и цикл переподключения.
- Ключ устройства и pinned VPS host key читаются только из приватного каталога приложения.
- ADB endpoint VPS ограничен loopback-портом `25556`.

## Влияние

Удалённая диагностика больше не зависит от Mac-моста, USB или общей локальной сети.

## Проверки

- Android debug build.
- Прямое подключение VPS к `127.0.0.1:25556`.
- ADB shell, VPN status и screenshot через прямой канал.
- После остановки Mac-моста прямой shell вернул `NO_MAC_OK` и модель `SM-X205`.
