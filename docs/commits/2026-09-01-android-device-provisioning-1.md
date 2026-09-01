# Android device provisioning

## Изменения

- Добавлен USB provisioning нового Android-устройства.
- Maintenance reverse-forward получил индивидуальные порты и credentials.
- Manifest расширен no-root разрешениями для wake/background recovery.

## Влияние

Новый телефон можно подготовить одной командой без копирования конфигураций и без повторного использования секретов планшета.

## Проверки

- Shell syntax provisioning-скрипта.
- Android debug build.
