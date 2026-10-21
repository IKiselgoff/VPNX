# Big Sur remote bootstrap

## Изменения
Шаблон standalone installer, упаковщик и VPS forced enrollment command.

## Влияние
Новый Mac сам генерирует ключ, включает LAN SSH и root LaunchDaemon для reverse 22084. Регистрация ограничена одним ключом/портом, исходные пользовательские ключи и другие туннели сохраняются.

## Проверка
Bash/Python syntax, проверка plist и ограниченного enrollment handler. Подтверждение доступности нового Mac возможно после запуска installer на целевой машине; installer сам проверяет SSH banner через VPS.
