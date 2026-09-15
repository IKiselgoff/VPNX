# Android direct-boot maintenance

## Изменения
- Maintenance service и boot receiver переведены в direct-boot режим.
- Ключ, known-hosts, control token и назначенные VPS-порты копируются в device-protected storage.
- До разблокировки control ограничен безопасным `STATUS`; VPN и BIRD данные остаются закрыты.
- Версия Android VPNX повышена до 1.2.8.

## Влияние
Защищённый входящий канал может восстановиться после полной перезагрузки до первого ввода PIN, не раскрывая VPN-профили в direct-boot хранилище.

## Проверка
- Перезагрузить устройство и не разблокировать экран.
- Проверить новые listener и token-authenticated `STATUS` через VPS без USB-команд восстановления.
- После разблокировки проверить VPN, Shizuku, ADB и полные control-команды.
