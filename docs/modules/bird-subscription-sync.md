# Module: bird-subscription-sync

## Назначение
Поддерживает BIRD-профили VPNX синхронными с подпиской Happ.

## Ответственность
Скачивает подписку, валидирует VLESS outbounds, присваивает стабильные теги и атомарно зеркалирует BIRD snapshot в `~/.vpnx/nodes`.

## Архитектурная роль
Фоновый интеграционный слой между BIRD VPN и существующим runtime VPNX/Xray.

## Зависимости
`curl`, системный Python 3, `launchd`, CLI `vpnx`, HTTPS endpoint BIRD VPN, необязательный URL зеркала в `~/.vpnx/bird-mirror-url` и локальный bootstrap `~/.vpnx/bird-bootstrap.json`.

## Структуры данных
Каждый профиль хранится как полный Xray runtime config, включая inbounds, все outbounds, routing, DNS, policy и observatory. `bird-subscription-state.json` содержит последний список управляемых тегов и исходные названия.

## Логика работы
Синхронизатор под межпроцессной блокировкой получает полный JSON, сохраняет runtime-профиль без UI-поля `remarks`, формирует стабильные country-теги, атомарно заменяет изменившиеся узлы и удаляет всё, чего больше нет в BIRD snapshot. Это сохраняет multi-outbound Auto-профили, балансировщики, observatory, transport, DNS и routing без упрощения. При ошибке основного endpoint используется настроенное HTTPS-зеркало, затем проверенный локальный bootstrap snapshot; state-файл явно помечает источник `live`, `mirror` или `bootstrap`. Меню показывает исходные названия Happ из state-файла. Если активный профиль изменился, Xray перезапускается.

## Ключевые функции

- `fetch_configs`: загружает и валидирует JSON подписки.
- `stable_tag`: превращает название BIRD в постоянный тег меню.
- `synchronize`: применяет полный snapshot и перезапускает активный изменившийся узел.

## Failure Modes
При отказе основного endpoint сначала проверяется HTTPS-зеркало, затем локальный bootstrap. Без доступного fallback существующий snapshot сохраняется. Каждый источник проходит одинаковую структурную валидацию; неизвестная страна останавливает применение всего snapshot, чтобы не получить частичное обновление. `launchd` повторит запуск через 15 минут.

## Recent Changes

### 2026-09-07 — bird HTTPS mirror
Добавлен TLS-защищённый last-good источник между основным endpoint и локальным bootstrap.

### 2026-09-07 — bird bootstrap fallback
Добавлен явный fallback на локальный полный snapshot при временной недоступности подписки.

### 2026-08-26 — bird subscription sync
Добавлена автономная синхронизация BIRD VPN при входе и по 15-минутному интервалу.
