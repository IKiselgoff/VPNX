# Integrations

BIRD: существующий локальный Happ producer публикует `/var/lib/vpnx-bird/snapshot.json` на VPS. macOS-клиенты читают его через forced SSH command `/usr/local/sbin/vpnx-mirror-read`; ключ ограничен `restrict`, host key закреплён. Snapshot содержит время источника. Никаких запросов оригинальной подписки с VPS или удалённых Mac.

Дополнительные JSON-подписки: HTTPS с проверкой сертификата и запретом redirect на HTTP. Секретные URL не возвращаются интерфейсу и не попадают в сообщения ошибок.

Xray: сохраняются все outbounds, transport, routing и observatory. Проверка `run -test` выполняется установленным на целевом Mac runtime до применения snapshot.
