# Module: android-vpnx

## Назначение
VPNX для Android: mirror-профили, VPN, независимые управляющий и ADB SSH-туннели.

## Ответственность
Синхронизация с HTTPS-зеркалом, запуск Xray, фоновое восстановление обслуживания. Root не требуется. Полный shell требует работающего ADB или разрешённого Shizuku.

## Архитектурная роль
Android клиент существующего VPS-зеркала и управляющего канала. Перенесён из сохранённой Android-ветки 69d3826 в основную ветку для воспроизводимой сборки.

## Зависимости
Android SDK 35, Java 17, libXray AAR, geoip/geosite, JSch, Shizuku. Runtime-артефакты и bootstrap-секреты вне Git.

## Структуры данных
SharedPreferences snapshot/selected/synced_at; device-protected ключ, host pins, токен, remote ports.

## Логика работы
Foreground maintenance создаёт две SSH-сессии, самостоятельно переподключает их с ограниченным backoff. AlarmManager, JobScheduler и boot/package-replaced receiver восстанавливают службу. Shizuku и shell-watchdog обеспечивают дополнительное восстановление, пока shell-процессы живы. Mac остаётся внешним резервом.

## Ключевые функции
- `BirdRepository.sync`: получает и валидирует зеркало, атомарно сохраняет профили.
- `MaintenanceTunnelService`: аутентифицирует control и восстанавливает независимые SSH-сессии.
- `MaintenanceStorage.migrateFromCredentialStorage`: сохраняет ключи/токены/порты и добавляет доверенные host pins из подписанного APK.
- `RecoveryScheduler.schedule`: регистрирует восстановление каждые 15 минут с ограничениями Android.

## Failure Modes
Без root Android не гарантирует возврат Shizuku/ADB после перезагрузки или force-stop. Спящий Mac не обеспечивает резервный доступ. Обновление APK останавливает прежний процесс; доступность проверяется заново. Открытый VPS listener не доказывает рабочий shell.

## Recent Changes
### 2026-10-13 — tablet access recovery
Android исходники возвращены в main, версия 1.2.9; при миграции сохранённых ключей добавляются актуальные SSH pins VPS без отключения проверки доверия.
