# Android split maintenance tunnels

## Изменения

- ADB и control reverse-forward перенесены в отдельные SSH-сессии и reconnect-loop.
- Завершение одной сессии не отключает второй recovery-path.

## Влияние

Control-порт может восстанавливать ADB даже при зависшем forwarding channel `25556`.

## Проверки

- Android debug build.
- Изменение основано на воспроизведённом взаимном зависании двух forward в общей JSch-сессии.
