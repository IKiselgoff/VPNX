# launchd engine lifetime

## Изменения
LaunchAgent сохраняет дочерний Xray после завершения watchdog/sync/UI через `AbandonProcessGroup=true`.

## Модули
`macos-subscriptions`, эксплуатация.

## Проверка
На Catalina без флага watchdog сообщал об успешном запуске, но после его выхода launchd завершал движок.
