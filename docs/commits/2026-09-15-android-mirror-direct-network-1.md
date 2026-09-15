# Android mirror direct network

## Изменения
- BIRD sync выбирает валидированную сеть с `NET_CAPABILITY_NOT_VPN`.
- Версия Android VPNX повышена до 1.2.6.

## Влияние
HTTPS-зеркало обновляется при активном VPNX без TLS timeout через собственный TUN.

## Проверка
- Поднять VPNX.
- Принудительно выполнить job `8621` и проверить обновление `synced_at`.
