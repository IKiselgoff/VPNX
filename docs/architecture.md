# Architecture

VPNX состоит из CLI/runtime Xray, menu bar UI и фонового BIRD sync. Подписка является источником полного набора runtime-конфигов; синхронизатор материализует snapshot в `~/.vpnx/nodes`, CLI копирует выбранный полный config в `~/.vpnx/config.json`, а Xray обслуживает локальные SOCKS/HTTP порты `10808/10809`.

Старый одиночный-outbound формат остаётся доступен для ручного `vpnx import`, но BIRD-профили используют полный Happ contract, включая routing и observatory.

Android runtime использует тот же полный snapshot, но заменяет desktop inbounds на Xray `tun`, созданный через Android `VpnService`. Outbound sockets исключаются из VPN маршрута через libXray controller, чтобы не возникал routing loop.
