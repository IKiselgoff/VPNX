# Mac SSH option path quoting

## Изменения
Внутренние кавычки в UserKnownHostsFile на всех SSH вызовах standalone installer.

## Влияние
OpenSSH читает правильный файл из Application Support. Проверка host key остаётся строгой.

## Проверка
Временный путь с пробелами воспроизвёл исходное No ECDSA host key. Тот же запрос с вложенными кавычками успешно аутентифицировал VPS и достиг restricted enrollment handler. Bash syntax проверен.
