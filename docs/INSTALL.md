# INSTALL

## CLI
```bash
./scripts/install.sh
source ~/.zshrc
```

Установщик также загружает `local.vpnx.bird-sync`: подписка BIRD обновляется при
входе пользователя и каждые 15 минут. Ручной запуск: `vpnx-bird-sync`.

## Import / Start
```bash
vpnx import NL-2025 'vless://...type=xhttp...' --default
vpnx start
```

## Build Xray if needed
```bash
vpnx build   # requires Go 1.24.x tar.gz and Xcode CLT
```

## Menubar
```bash
./scripts/build_menubar.sh
cp -R Menubar/VPNXMenu/VPNXMenu.app /Applications/
open /Applications/VPNXMenu.app
```
