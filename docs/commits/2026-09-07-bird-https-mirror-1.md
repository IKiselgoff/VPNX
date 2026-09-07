# BIRD HTTPS mirror

## Изменения
- Добавлен настраиваемый TLS-защищённый last-good источник BIRD snapshot.
- Приоритет источников: основной endpoint, HTTPS-зеркало, локальный bootstrap.
- State-файл фиксирует использование зеркала.

## Модули
- `bird-subscription-sync`
- `integrations`

## Проверка
- Основной endpoint возвращает TLS alert, зеркало выдаёт валидный 12-профильный JSON.
- Клиент применяет зеркало и сохраняет `source=mirror`.
