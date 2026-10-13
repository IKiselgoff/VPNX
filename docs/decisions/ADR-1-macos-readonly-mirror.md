# ADR-1: read-only BIRD mirror for macOS

## Контекст
Producer на основном Mac уже получает BIRD через собственную Happ identity и публикует приватный snapshot на VPS. Удалённые Mac используют устаревший endpoint и ограниченный список стран.

## Решение
Получать snapshot с VPS через отдельный SSH ключ, ограниченный forced command `vpnx-mirror-read` и `restrict`. Закреплять host key. Ответ содержит полный JSON и mtime файла. Разделить время получения и свежесть источника в UI.

## Последствия
Новые Mac не создают дополнительные Happ identity. Им не предоставляются shell, forwarding или доступ к другим файлам VPS. Producer должен продолжать обновлять зеркало; доступность SSH не означает свежесть подписки.
