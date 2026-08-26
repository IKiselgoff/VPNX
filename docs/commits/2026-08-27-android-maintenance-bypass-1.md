# Android maintenance bypass

## Изменения

- Android Xray config получает приоритетный direct-маршрут к VPS управления.

## Влияние

Reverse SSH восстанавливается независимо от состояния выбранного BIRD outbound.

## Проверки

- Android debug build.
- Shizuku UserService работает с UID `2000` при активном VPN.
