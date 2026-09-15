# Android runtime asset guard

## Изменения
- `preBuild` проверяет четыре обязательных Android runtime asset.
- Сборка сообщает команду восстановления вместо выпуска неполного APK.

## Влияние
Исключён запуск APK с `FileNotFoundException` при первой инициализации VPNX.

## Проверка
- `assembleDebug` должен завершиться ошибкой без GeoIP/GeoSite.
- После восстановления assets сборка и первый запуск должны пройти успешно.
