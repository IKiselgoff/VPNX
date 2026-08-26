# vpnx-macos

Менеджер Xray (VLESS Reality TCP/XHTTP) для macOS Catalina 10.15+:
- CLI `vpnx` — установка/сборка Xray, импорт VLESS, старт/стоп, переключение, default;
- Меню-бар приложение (AppKit, без Xcode GUI) — старт/стоп/переключение/импорт из интерфейса;
- Полностью офлайн-установка без Homebrew (опциональная локальная сборка Xray через Go tar.gz).
- Автоматическая синхронизация всех профилей BIRD VPN из Happ при входе и каждые 15 минут.
- Android-приложение VPNX с системным `VpnService`, полным BIRD snapshot и автоматическим восстановлением после сети/перезагрузки.

## Быстрый старт

```bash
./scripts/install.sh         # установить vpnx в ~/.vpnx/bin, добавить PATH, скачать xray
vpnx import NL-2025 'vless://...type=xhttp&...' --default
vpnx start                   # запустить default
vpnx stop
vpnx list; vpnx default; vpnx status
vpnx-bird-sync             # немедленно обновить BIRD-профили
```

### Меню-бар (опционально)
```bash
./scripts/build_menubar.sh   # соберёт VPNXMenu.app без Xcode GUI
cp -R Menubar/VPNXMenu/VPNXMenu.app /Applications/
open /Applications/VPNXMenu.app
```

## Дерево проекта
```
vpnx-macos/
├─ vpnx/                    # исходники CLI (установщик/менеджер)
│  ├─ bin/vpnx
│  ├─ base/config.base.json
│  └─ launchd/local.vpnx.autostart.plist
├─ Menubar/VPNXMenu/        # исходники меню-бара (AppKit)
│  ├─ Sources/AppDelegate.swift
│  ├─ Sources/main.swift
│  ├─ Info.plist
│  └─ build.sh
├─ scripts/
│  ├─ install.sh            # быстрый установщик CLI+Xray
│  ├─ build_menubar.sh      # сборка .app
│  └─ package.sh            # zip-архив с готовыми артефактами
├─ docs/                    # установка, эксплуатация и документация модулей
├─ Android/VPNX/            # нативный Android VPN-клиент (Android 8+)
├─ examples/
│  └─ vless_urls.txt
├─ .gitignore
├─ .gitattributes
├─ LICENSE
└─ README.md
```

## Требования
- macOS 10.15+ (Catalina); официальный бинарник закреплён на совместимой версии Xray `25.4.30`;
- Xcode Command Line Tools (`xcode-select -p`);
- **Опционально**: Go 1.24.x (tar.gz) для локальной сборки Xray, если готовый бинарь не подходит.

## Лицензия
MIT
