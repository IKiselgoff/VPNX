# Android mirror-only subscription

## Summary
Android VPNX переведён с прямого BIRD upstream на приватное HTTPS last-good зеркало.

## Modules
- `android-vpnx`
- `BIRD VPN integration`

## Changes
- Удалены настоящий URL подписки и Happ device headers из Android runtime и build script.
- Runtime и bootstrap используют валидируемое зеркало.
- Версия приложения повышена до `1.2.4` (`versionCode 7`).

## Verification
- HTTPS-зеркало возвращает `200` и валидный snapshot из 12 профилей.
- Android debug APK собирается с mirror-only endpoint.
