# macOS subscriptions UI

## Изменения
- Менеджер подписок и read-only доставка актуального зеркала VPS; теги не ограничены странами.
- Cocoa окно с поиском, группами, свежестью, ошибками, управлением и проверкой интернета; минимум macOS 10.15.
- Перенесена защита CLI от повторного запуска, manual stop и watchdog; профиль проверяется до остановки работающего Xray.
- Release installer сохраняет backup и устанавливает launchd sync/watchdog.
- Xray 26.10.10 с проверенным архивом и private Security compatibility dylib поддерживает Hysteria на Catalina.

## Модули
- `macos-subscriptions`, интеграции, эксплуатация; ADR-1 фиксирует read-only доставку.

## Проверка
- Сборка x86_64 для Catalina; тесты сохранения full config, last-good при несовместимости Xray и ручных профилей.
- На Mac Ирины старый Xray принимал 20/25 профилей; новый проверил 25/25.
