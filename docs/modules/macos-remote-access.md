# Module: macos-remote-access

## Назначение
Однократная установка LAN SSH и постоянно восстанавливаемого reverse SSH на macOS Big Sur.

## Ответственность
Генерация собственного ключа Mac, ограниченная первичная регистрация на VPS, установка root LaunchDaemon и ключа оператора в аккаунт пользователя.

## Архитектурная роль
Независимый от VPNX/VPN транспорт. VPS listener доступен только на loopback, подключение оператора через ProxyJump.

## Зависимости
На Mac штатные bash 3.2, OpenSSH, launchd, sudo, pmset; на VPS OpenSSH и Python 3.

## Структуры данных
Root-only `/Library/Application Support/VPNXRemote` хранит ключ/host pins/account/access и исходные настройки питания. VPS `/var/lib/vpnx-mac-enrollment/<id>.json` связывает identity с единственным публичным ключом и портом.

## Логика работы
Installer использует отдельный enrollment key с forced command и запрещённым forwarding. Enroller валидирует Ed25519 blob, сериализует регистрацию flock, запрещает замену привязанного ключа и выдаёт forwarding-only ключ на фиксированный loopback порт. LaunchDaemon и shell loop переподключают туннель с keepalive 15/3, backoff 5 секунд и проверкой локального SSH. SSH pins проверяются строго. Завершающий CHECK подтверждает SSH banner через reverse listener.

## Ключевые функции
- `vpnx-mac-enroll`: принимает public key или CHECK public key на stdin; возвращает OK/READY с портом. Секреты Mac на VPS не передаются.
- `package_remote_mac.py`: собирает standalone shell вне Git из шаблона, public pins и ограниченного enrollment key; отказывается перезаписывать готовый файл.

## Failure Modes
Нет интернета, закрытая крышка/сон или FileVault до первого входа делают Mac недоступным. Installer отключает AC sleep и включает wake-on-LAN, но не меняет FileVault и battery sleep. При отказе macOS включить штатный SSH скрипт завершится с конкретным действием Remote Login. Нужна доступность исходящего TCP 22 к VPS. Готовый installer содержит enrollment credential и после привязки не используется на другом Mac. Неизвестный порт не считается свободным только из-за отсутствия listener: проверяется authorized_keys.

## Recent Changes
### 2026-10-21 — Big Sur host key compatibility
Упаковка требует проверенные Ed25519 и ECDSA pins VPS. Enrollment, reverse и CHECK используют `-F /dev/null`, чтобы прежние пользовательские SSH-настройки не меняли алгоритмы/маршрут. StrictHostKeyChecking остаётся включённым. LAN SSH работает напрямую независимо от reverse.

### 2026-10-21 — Big Sur remote bootstrap
Добавлены ограниченная регистрация, standalone installer, LAN SSH, boot LaunchDaemon, keepalive, ротация логов и end-to-end CHECK.
При существующем SSH access group выбранный пользователь добавляется без изменения остальных участников. Installer выводит mDNS и LAN IP для en0/en1/en2.
