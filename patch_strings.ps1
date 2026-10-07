$ru = Get-Content -Path "feature/dashboard/src/main/res/values-ru/strings.xml" -Raw
$ru = $ru -replace '</resources>', '    <string name="wireless_adb_show_instructions">📖 Инструкция по подключению</string>
    <string name="wireless_adb_hide_instructions">Скрыть инструкцию</string>
    <string name="wireless_adb_instructions_text">1. Зайдите в "Настройки" -> "Для разработчиков".\n2. Включите "Отладка по Wi-Fi" (Wireless Debugging).\n3. Введите IP и Порт, указанные в настройках отладки.\n4. Внимание: на обычных смартфонах (без встроенного ADB) команды работают только через ПК или Root. На ГУ авто ADB обычно встроен.</string>
</resources>'
Set-Content -Path "feature/dashboard/src/main/res/values-ru/strings.xml" -Value $ru -Encoding UTF8

$en = Get-Content -Path "feature/dashboard/src/main/res/values/strings.xml" -Raw
$en = $en -replace '</resources>', '    <string name="wireless_adb_show_instructions">📖 Connection Instructions</string>
    <string name="wireless_adb_hide_instructions">Hide Instructions</string>
    <string name="wireless_adb_instructions_text">1. Go to "Settings" -> "Developer options".\n2. Enable "Wireless Debugging".\n3. Enter the IP and Port specified in the debugging settings.\n4. Note: On regular smartphones (without built-in ADB), commands only work via PC or Root. Car head units usually have built-in ADB.</string>
</resources>'
Set-Content -Path "feature/dashboard/src/main/res/values/strings.xml" -Value $en -Encoding UTF8
