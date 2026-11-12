# История kino.pub — канал для главного экрана ТВ

Мини-приложение Android TV (Mecool KM9 Pro, Projectivy Launcher): публикует
канал «История kino.pub» на главном экране с историей просмотров из API
kino.pub. Показывает до 30 последних просмотренных тайтлов с постерами;
клик открывает карточку тайтла в приложении kino.pub (`com.kinopub`).

## Устройство

- OAuth2 device-flow против `https://api.service-kp.com`. Креденшелы зашиты
  дефолтом прямо в `Prefs.java` — публичные от open-source Kodi-клиента
  (client_id `xbmc`); своя пара выдаётся только через support@kino.pub, её можно
  подставить в том же файле. Refresh-токен хранится в SharedPreferences приложения.
- `GET /v1/history` → записи публикуются в TvProvider как browsable-канал +
  preview-programs; deep-link `https://kinopub.tv/item/view/<id>` (именно
  `/item/view` — фильтры InfoActivity).
- Projectivy запускает клики по каналу в пакете владельца канала, поэтому
  добавлен невидимый `ForwardActivity`-трамплин, перенаправляющий ссылку в
  `com.kinopub` (`setPackage`).
- Синхронизация: WorkManager, раз в 12 ч + `BootReceiver` после перезагрузки

## Сборка

Тулчейн: JDK 17 (`C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot`),
Android SDK (`C:\Android\Sdk`, platform-34, build-tools 34.0.0),
Gradle 8.9 (`C:\gradle\gradle-8.9`).

```bash
export JAVA_HOME="/c/Program Files/Microsoft/jdk-17.0.21.21-hotspot"
export ANDROID_HOME="C:\\Android\\Sdk"
export PATH="$JAVA_HOME/bin:/c/gradle/gradle-8.9/bin:$PATH"
gradle --no-daemon assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Установка: `adb -s 192.168.1.145:5555 install -r app/build/outputs/apk/debug/app-debug.apk`
(пакет с суффиксом: `ru.neoniklain.kinohist.debug`).
