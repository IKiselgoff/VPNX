# Android VPNX

## Изменения
Добавлено Android-приложение VPNX: собственный UI, полный BIRD snapshot, Android VpnService с прямым Xray TUN, 15-минутная и network-reactive синхронизация, boot recovery и диагностика.

## Затронутые модули
- `android-vpnx`
- BIRD integration

## Поведение
Пользователь один раз подтверждает системный VPN. После этого выбранный профиль запускается как полный Happ config; обновления догоняются при сети, а ранее активный VPN восстанавливается после загрузки устройства.

## Проверка
- Gradle debug build
- установка APK через ADB на Samsung SM-X205 / Android 14
- live subscription sync
- системное VPN permission
- внешний IP и Telegram через TUN
