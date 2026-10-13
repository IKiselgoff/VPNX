# Operations

macOS VPNX 2.0: окно из меню VPNX, двойной клик подключает профиль; доступны поиск, группировка подписок, ручное обновление и проверка YouTube/Telegram. Дополнительные подписки принимают HTTPS список полных Xray JSON.

`~/.vpnx/bin/vpnx-subscriptions status` возвращает состояние без секретов. `sync` обновляет сразу, `sync --due` соблюдает интервалы. Реестр и состояние имеют права 600; зеркало доступно ключом `~/.vpnx/mirror-key` с закреплённым `mirror-known-hosts`.

`scripts/update_macos.py` устанавливает release bundle, сохраняет backup приложения, CLI, runtime и plist в `~/.vpnx/backup-ui-*`. Требуется заранее настроенный mirror key. При наличии `runtime/` движок заменяется проверенной сборкой. LaunchAgent sync проверяет сроки раз в 60 секунд, обновляет включённые подписки раз в 15 минут. Watchdog уважает `~/.vpnx/disabled`.

`scripts/build_legacy_runtime.sh <output>` воспроизводит Xray 26.10.10 для Intel Catalina/Big Sur: проверяет архив, собирает private Security compatibility dylib, меняет только зависимость framework и подписывает бинарник ad-hoc. При обновлении dylib и Xray должны находиться рядом.

Обновление выкладывается из основной ветки. Отсутствие reverse listener удалённого Mac означает, что пакет подготовлен, но на этот Mac ещё не установлен.
