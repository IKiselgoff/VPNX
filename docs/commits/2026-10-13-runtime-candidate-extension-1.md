# Runtime candidate extension

## Изменения
Временный профиль перед запуском получает суффикс `.json`; Xray корректно определяет формат и выполняет `run -test`.

## Модули
`macos-subscriptions` / runtime launch.

## Проверка
Причина подтверждена на Catalina: `.candidate` давал `Failed to get format`, полный JSON проходил validation.
