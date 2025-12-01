# TROUBLESHOOTING

- `dyld: Symbol not found: _SecTrustCopyCertificateChain` — бинарь Xray не совместим с Big Sur. Решение: `vpnx build` (локальная сборка через Go 1.24.x).
- Интернет не работает после импорта — проверь, что ссылка `vless://` имеет `type=xhttp` и корректные `sni`, `pbk`, `spx`.
- Меню-бар не меняет статус — проверка идёт по `~/.vpnx/xray.pid`. Убедись, что файл обновляется при старте/стопе.
