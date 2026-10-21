# Big Sur host key compatibility

## Изменения
Установщик изолирует SSH от старых config, упаковщик требует Ed25519 и ECDSA host pins.

## Влияние
Исправлено падение регистрации при выборе ECDSA старым Mac. Проверка подлинности VPS не ослабляется. LAN SSH сохраняется.

## Проверка
Тест регистрации с принудительным ECDSA успешно проверяет VPS и доходит до restricted enrollment handler. Bash syntax проверен.
