# Mac reverse key options

## Изменения
Заменён недопустимый permitopen port 0 в enrollment handler на loopback port 1.

## Влияние
VPS принимает ограниченный ключ reverse tunnel; shell/PTY запрещены, permitlisten ограничен портом конкретного Mac. Установленная запись исправлена без повторного запуска installer.

## Проверка
Новый Big Sur 11.7.10 отвечает по LAN 192.168.0.106 и через VPS 22084; изменение серверной записи устранило Permission denied.
