# Operations

## Big Sur Remote Bootstrap
Сервер: `/usr/local/sbin/vpnx-mac-enroll <identity> <port>` разрешается только forced SSH command отдельного enrollment key (`restrict` без forwarding). Готовый `.sh` создаёт `scripts/package_remote_mac.py`; private enrollment key не коммитится. Installer запускается `bash ./connect-mac-big-sur.sh` из Terminal обычного пользователя с sudo один раз. После READY оператор подключается `ssh -J root@45.146.165.85 -p 22084 <user>@127.0.0.1`, локально `<user>@<LocalHostName>.local`.

Диагностика Mac: `sudo launchctl print system/local.vpnx.reverse`, `/var/log/vpnx-reverse.log`, `/Library/Application Support/VPNXRemote/access.txt`. Питание до установки сохранено в `power-settings-before.txt`; AC sleep отключён для доступности. FileVault до первоначальной разблокировки и закрытая крышка остаются ограничениями. При удалении отключить только `local.vpnx.reverse` и его ключ на VPS; системный Remote Login и пользовательские authorized_keys не удалять автоматически.

При каждом открытии окна задержки обновляются автоматически в фоне. Текущее подключение сохраняется; рабочие быстрые профили показываются первыми. Для простого использования достаточно кнопки «Подключить лучший»; после только что завершённой проверки она использует свежий результат.

В окне доступны «Пинг выбранного», «Проверить группу», «Выбрать быстрый» и сортировка по задержке. Проверка измеряет реальный HTTPS round trip до YouTube через отдельный Xray каждого профиля; активное подключение не меняется. «Выбрать быстрый» проверяет группу и подключает лучший успешный результат.

Маршрутизация: правила подписки, весь проксируемый трафик через VPN или российские домены/IP напрямую. Это HTTP/SOCKS proxy policy, не TUN для всех приложений. CLI: `vpnx-subscriptions probe [TAG...]`, `best [TAG...]`, `route subscription|global|ru-direct`. Исходные профили подписки сохраняются без изменений.

macOS VPNX 2.0: окно из меню VPNX, двойной клик подключает профиль; доступны поиск, группировка подписок, ручное обновление и проверка YouTube/Telegram. Дополнительные подписки принимают HTTPS список полных Xray JSON.

`~/.vpnx/bin/vpnx-subscriptions status` возвращает состояние без секретов. `sync` обновляет сразу, `sync --due` соблюдает интервалы. Реестр и состояние имеют права 600; зеркало доступно ключом `~/.vpnx/mirror-key` с закреплённым `mirror-known-hosts`.

`scripts/update_macos.py` устанавливает release bundle, сохраняет backup приложения, CLI, runtime и plist в `~/.vpnx/backup-ui-*`. Требуется заранее настроенный mirror key. При наличии `runtime/` движок заменяется проверенной сборкой. LaunchAgent sync проверяет сроки раз в 60 секунд, обновляет включённые подписки раз в 15 минут. Watchdog уважает `~/.vpnx/disabled`.

`scripts/build_legacy_runtime.sh <output>` воспроизводит Xray 26.10.10 для Intel Catalina/Big Sur: проверяет архив, собирает private Security compatibility dylib, меняет только зависимость framework и подписывает бинарник ad-hoc. При обновлении dylib и Xray должны находиться рядом.

Обновление выкладывается из основной ветки. Отсутствие reverse listener удалённого Mac означает, что пакет подготовлен, но на этот Mac ещё не установлен.

Агенты CLI/sync/UI используют `AbandonProcessGroup=true`; проверка recovery включает существование Xray после полного завершения watchdog, а не только строку успешного запуска.
