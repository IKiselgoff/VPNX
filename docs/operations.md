# Operations

## BIRD subscription
Ручное обновление: `vpnx-bird-sync`.

Фоновое обновление выполняет пользовательский LaunchAgent `local.vpnx.bird-sync` при входе и каждые 15 минут. Логи находятся в `~/.vpnx/bird-sync.out.log` и `~/.vpnx/bird-sync.err.log`.

При сетевой ошибке текущие профили не меняются; следующий интервальный запуск повторяет попытку.

Полные профили с `geoip:`/`geosite:` правилами требуют `~/.vpnx/geoip.dat` и `~/.vpnx/geosite.dat`; установщик Xray сохраняет эти assets рядом с бинарником.

## Android
Собрать APK: `cd Android/VPNX`, выполнить `scripts/download-runtime.sh`, затем `./gradlew assembleDebug`. Установка на подключённое устройство: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Для no-root shell-диагностики установить официальный Shizuku 13.6.0, запустить его через Wireless debugging или ADB и выдать VPNX разрешение в Shizuku. Строка `Расширенная диагностика: shell подключён` подтверждает доступ UserService; диагностический вывод должен содержать `uid=2000(shell)`. После перезагрузки автозапуск Shizuku требует включённой системной отладки и доверенной Wi-Fi-сети.

Служебное подключение планшета к `45.146.165.85` всегда направляется через `direct`; это обязательный recovery-path и не является проверкой работоспособности текущего VPN-профиля.
