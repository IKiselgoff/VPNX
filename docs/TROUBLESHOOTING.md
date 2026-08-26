# TROUBLESHOOTING

- `dyld: Symbol not found: _SecTrustCopyCertificateChain` — бинарь Xray не совместим с Big Sur. Решение: `vpnx build` (локальная сборка через Go 1.24.x).
- На Catalina официальный Xray `latest` может требовать macOS 12. Установщик закрепляет `v25.4.30`, который поддерживает текущие BIRD TCP Reality, xHTTP, `leastLoad` и `burstObservatory`.
- Интернет не работает после импорта — проверь, что ссылка `vless://` имеет `type=xhttp` и корректные `sni`, `pbk`, `spx`.
- Меню-бар не меняет статус — проверка идёт по `~/.vpnx/xray.pid`. Убедись, что файл обновляется при старте/стопе.
