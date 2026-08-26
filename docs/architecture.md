# Architecture

VPNX состоит из CLI/runtime Xray, menu bar UI и фонового BIRD sync. Подписка является источником полного набора runtime-конфигов; синхронизатор материализует snapshot в `~/.vpnx/nodes`, CLI копирует выбранный полный config в `~/.vpnx/config.json`, а Xray обслуживает локальные SOCKS/HTTP порты `10808/10809`.

Старый одиночный-outbound формат остаётся доступен для ручного `vpnx import`, но BIRD-профили используют полный Happ contract, включая routing и observatory.
