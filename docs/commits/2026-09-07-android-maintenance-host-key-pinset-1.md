# Commit: android-maintenance-host-key-pinset

## Изменения

- Расширен доверенный pinset реальными ED25519, ECDSA и RSA host keys VPS.
- Версия Android VPNX повышена до `1.2.1`.

## Затронутые модули

- `android-vpnx`

## Проверка

- Ключи сверены с `/etc/ssh/ssh_host_*_key.pub` на VPS.
- Исходный отказ подтверждён свежим Android logcat как `JSchUnknownHostKeyException`.
