# Operations

## BIRD subscription
Ручное обновление: `vpnx-bird-sync`.

Фоновое обновление выполняет пользовательский LaunchAgent `local.vpnx.bird-sync` при входе и каждые 15 минут. Логи находятся в `~/.vpnx/bird-sync.out.log` и `~/.vpnx/bird-sync.err.log`.

При сетевой ошибке синхронизатор пробует HTTPS URL из `~/.vpnx/bird-mirror-url`, затем `~/.vpnx/bird-bootstrap.json`. Поле `source` в `~/.vpnx/bird-subscription-state.json` показывает `live`, `mirror` или `bootstrap`; следующий интервальный запуск всё равно повторяет основной endpoint. Без доступного fallback текущие профили не меняются.

Полные профили с `geoip:`/`geosite:` правилами требуют `~/.vpnx/geoip.dat` и `~/.vpnx/geosite.dat`; установщик Xray сохраняет эти assets рядом с бинарником.

Повторный `vpnx start` для уже активного тега не создаёт второй Xray. `vpnx status` сверяется с фактической командной строкой процесса и исправляет устаревший PID-файл. LaunchAgent вызывает `vpnx watchdog` каждую минуту; ручной `vpnx stop` запрещает восстановление до следующего `vpnx start`.

## Android
Собрать APK: `cd Android/VPNX`, выполнить `scripts/download-runtime.sh`, затем `./gradlew assembleDebug`. Установка на подключённое устройство: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

Для no-root shell-диагностики установить официальный Shizuku 13.6.0, запустить его через Wireless debugging или ADB и выдать VPNX разрешение в Shizuku. Строка `Расширенная диагностика: shell подключён` подтверждает доступ UserService; диагностический вывод должен содержать `uid=2000(shell)`. После перезагрузки автозапуск Shizuku требует включённой системной отладки и доверенной Wi-Fi-сети.

Служебное подключение планшета к `45.146.165.85` всегда направляется через `direct`; это обязательный recovery-path и не является проверкой работоспособности текущего VPN-профиля.

Control endpoint доступен только локально на VPS: клиент отправляет первой строкой содержимое `/root/.vpnx-tablet-control-token`, второй строкой одну из команд `STATUS`, `SYNC`, `RESTART_VPN`, `RESTORE_ADB_TCP`. `RESTORE_ADB_TCP` выполняется только при рабочем Shizuku, поэтому не используется как первая диагностика и не заменяет проверку `STATUS`.

Если оба reverse-forward отсутствуют, maintenance запускается из четырёх источников: boot/package receiver, persisted 15-минутного job, пятиминутного idle-aware alarm и активного VPN service. Alarm может быть отложен системным Doze. Одна зависшая control-сессия не блокирует другие клиенты; SSH socket timeout переводит полумёртвый transport в reconnect-loop.

После `adb install -r` проверять следует реальный shell, а не только listener: `adb disconnect 127.0.0.1:25556`, `adb connect 127.0.0.1:25556`, затем `adb -s 127.0.0.1:25556 shell getprop ro.product.model`. Первые две команды меняют только регистрацию ADB-клиента на VPS и не переключают transport планшета.

Переносимая Android-сборка содержит актуальный bootstrap BIRD и URL подписки. При первом открытии она синхронизирует профили и показывает системные battery-optimization и VPN consent; после подтверждения выбранный Auto WiFi профиль запускается автоматически. Устройство само создаёт индивидуальные maintenance credentials и регистрирует их на VPS ограниченным enrollment-ключом, поэтому USB для основного сценария не нужен.

Android использует только приватное HTTPS-зеркало BIRD. Исходная Happ-подписка остаётся на macOS и доступна серверному обновителю зеркала; APK не содержит её URL и идентифицирующие Happ-заголовки.

Для максимальной no-root подготовки подключить один телефон по USB и выполнить `VPNX_ADB_PORT=<уникальный порт> VPNX_CONTROL_PORT=<уникальный порт> Android/VPNX/scripts/provision-device.sh`. Скрипт устанавливает APK, создаёт уникальные restricted credentials, регистрирует два loopback reverse-forward на VPS, применяет доступные background exemptions и включает ADB TCP. USB debugging и VPN consent подтверждаются пользователем по требованиям Android; Shizuku является дополнительным и не блокирует VPN.

На OEM-прошивках дополнительный `device-maintenance-watchdog.sh` запускается двумя shell-процессами. При выгрузке VPNX он сначала будит persisted job `8621`, затем использует launcher как ограниченный fallback; watchdog не переключает `adbd`, VPN-профили или сеть. После полной перезагрузки его восстанавливают Android recovery-механизмы VPNX либо повторный provisioning, поскольку no-root shell-процесс не переживает reboot самостоятельно.

`vpnx-device-monitor@.service` является read-only VPS-наблюдателем. Конфигурация устройства задаёт `DEVICE_ID`, `ADB_PORT` и `CONTROL_PORT`; монитор различает открытый listener, рабочий ADB shell и authenticated control, не выполняя `adb usb`, `tcpip`, restart или kill.
