# Operations

## BIRD subscription
Ручное обновление: `vpnx-bird-sync`.

Фоновое обновление выполняет пользовательский LaunchAgent `local.vpnx.bird-sync` при входе и каждые 15 минут. Логи находятся в `~/.vpnx/bird-sync.out.log` и `~/.vpnx/bird-sync.err.log`.

При сетевой ошибке текущие профили не меняются; следующий интервальный запуск повторяет попытку.

Полные профили с `geoip:`/`geosite:` правилами требуют `~/.vpnx/geoip.dat` и `~/.vpnx/geosite.dat`; установщик Xray сохраняет эти assets рядом с бинарником.

## Android
Собрать APK: `cd Android/VPNX`, выполнить `scripts/download-runtime.sh`, затем `./gradlew assembleDebug`. Установка на подключённое устройство: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
