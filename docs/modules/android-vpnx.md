# Module: android-vpnx

## Назначение
Предоставляет на Android тот же BIRD VPN workflow, что VPNX на macOS: полный список профилей, переключение, системный VPN и автономное обновление.

## Ответственность
Управляет Android VpnService/TUN, жизненным циклом libXray, BIRD snapshot, выбранным профилем, foreground notification и фоновой синхронизацией.

## Архитектурная роль
Самостоятельный Android frontend/runtime в общем VPNX-репозитории. Подписка остаётся единым источником конфигурации для macOS и Android.

## Зависимости
Android SDK 35, Kotlin, AndroidX Core, официальный `XTLS/libXray`, Xray geo assets, HTTPS endpoint BIRD и официальный Shizuku API 13.1.5.

Удалённая эксплуатация использует встроенный JSch-клиент и отдельный ключ планшета для прямого SSH reverse-forward на VPS. VPS-порт слушает только loopback и не публикует ADB в интернет. Ключ и pinned host key хранятся в приватном каталоге VPNX и не включаются в APK или Git.

Расширенная локальная диагностика использует Shizuku UserService. Команды выполняются с Android UID `shell` только после отдельного разрешения Shizuku, ограничены таймаутом и размером результата; сетевой SSH-порт на планшете не открывается.

## Структуры данных
Snapshot хранится атомарно в приватных SharedPreferences. Профиль содержит стабильный id, исходный `remarks` и полный Xray JSON. Флаги `selected_profile`, `running`, `auto_start`, `synced_at` задают runtime-состояние.

## Логика работы
`BirdRepository` валидирует полный snapshot до commit. При первом запуске он использует проверенный снимок BIRD, встроенный доверенной сборкой и не хранящийся в Git; успешная сетевая синхронизация заменяет его. `VpnxVpnService` создаёт системный TUN, передаёт fd через root `env`, регистрирует socket protector и запускает полный профиль через `runXrayFromJson` libXray bridge API v1. Периодический JobScheduler и network callback обновляют snapshot; активный VPN перезапускается только при реальном изменении.

Перед запуском Xray Android-адаптер добавляет приоритетное правило `45.146.165.85/32 → direct`. Оно сохраняет служебный SSH-канал к VPS при неисправном или несовместимом proxy outbound и не меняет маршрутизацию остального трафика.

## Ключевые функции

- `BirdRepository.sync`: загружает, валидирует и атомарно сохраняет snapshot.
- `VpnxVpnService.androidConfig`: адаптирует Happ config к Android TUN без изменения outbounds/routing/DNS.
- `VpnxVpnService.startEngine`: устанавливает VpnService, DNS/socket protection и libXray lifecycle.
- `SyncScheduler.schedule`: создаёт persisted network-constrained 15-минутную задачу.
- `ShizukuShell.connect`: отслеживает Shizuku binder и подключает shell UserService после выдачи разрешения.
- `VpnxShellUserService.execute`: выполняет диагностическую команду под UID Shizuku с лимитом времени и вывода.

## Failure Modes
Сетевая ошибка не заменяет последний рабочий snapshot. Ошибка Xray журналируется, закрывает TUN и снимает desired-running, чтобы не оставлять устройство без сети. Android всегда требует разового пользовательского подтверждения системного VPN.

ADB TCP без root может сброситься после полной перезагрузки Android. Прямой SSH-канал VPNX от LAN-адреса и Mac-моста не зависит, но после сброса самого `adbd` сможет восстановить только диагностический transport, а не включить системный TCP ADB без shell/root-привилегии.

Shizuku без root также должен быть запущен после загрузки. Shizuku 13.6.0 поддерживает автозапуск на Android 13+ в доверенной Wi-Fi-сети; если системная отладка или доверие к сети сброшены, требуется штатный повторный запуск Shizuku.

## Recent Changes

### 2026-08-27 — android-shizuku-shell
Добавлена no-root интеграция Shizuku UserService для shell-диагностики и отображение её состояния в VPNX.

### 2026-08-27 — android-maintenance-bypass
Служебный VPS исключён из proxy-маршрута, чтобы прямой maintenance-канал восстанавливался независимо от BIRD-профиля.

### 2026-08-26 — android-vpnx-bootstrap
Добавлен сборочный bootstrap актуальной подписки BIRD для надёжного первого запуска.

### 2026-08-26 — android-vpnx-debug-connect
Debug-сборка получила явный Activity intent для прямого запуска уже разрешённого VPN при диагностике через ADB, без нестабильного Samsung consent UI.

### 2026-08-26 — android-adb-reverse
Mac-мост сохранён как неприоритетный резерв на VPS-порту `25555`; основной Android → VPS канал использует `25556`.

### 2026-08-27 — android-direct-maintenance
VPNX получил прямой защищённый reverse-forward планшет → VPS с автоматическим восстановлением; Mac-мост удалён. ADB TCP направляется на loopback-порт `25556` VPS.

### 2026-08-26 — Android VPNX
Добавлены Android UI, libXray TUN runtime, BIRD autosync, boot recovery и диагностика.
