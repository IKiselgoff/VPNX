# ADR-2: Restricted Mac enrollment

## Status
Accepted 2026-10-21.

## Context
Mac Big Sur не имеет SSH-доступа, требуется один переносимый installer с регистрацией и постоянным reverse.

## Decision
Mac генерирует уникальный Ed25519 ключ. Installer содержит только отдельный restricted enrollment credential; forced server command связывает один public key с одним loopback reverse port, без общей shell-привилегии. Root LaunchDaemon запускает SSH до пользовательского входа после загрузки ОС. Ключ оператора добавляется в обычный аккаунт пользователя.

## Consequences
FileVault до разблокировки, сон/закрытая крышка и отсутствие сети остаются физическими ограничениями. Installer одноустройственный; root credentials VPS не распространяются. Возможность соединения проверяется по SSH banner через VPS, не по наличию процесса.
