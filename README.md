# История kino.pub — канал для главного экрана Android TV

Мини-приложение: публикует на домашнем экране телевизора канал «История
kino.pub» — до 30 последних просмотренных тайтлов из API kino.pub с постерами.
Клик по тайтлу открывает его карточку в официальном приложении kino.pub
(`com.kinopub`), просмотр остаётся там. Работает с лаунчерами, которые умеют
показывать каналы Android TV (проверено на Projectivy Launcher).

## Установка

1. Установить APK из [релизов](../../releases).
2. Открыть «История kino.pub» в списке приложений → «Привязать устройство».
3. Ввести показанный код на https://kino.watch/device под своим аккаунтом.

Дальше всё само: синхронизация раз в 12 часов и после перезагрузки устройства.
Строку канала на главный экран добавляет лаунчер (в Projectivy: Настройки →
Изменить каналы).

## Устройство

- Авторизация — OAuth2 device flow против `https://api.service-kp.com`.
  Клиентские креденшелы зашиты дефолтом в `Prefs.java` — это публичная пара
  open-source Kodi-клиента kino.pub; свою пару можно получить у
  support@kino.pub и подставить в том же файле. Refresh-токен хранится только
  в SharedPreferences на устройстве.
- `GET /v1/history` → записи публикуются в TvProvider как browsable-канал +
  preview-programs. Deep-link в приложение kino.pub строго вида
  `https://kinopub.tv/item/view/<id>` (другие пути ссылки резолвятся, но
  активность kino.pub их не принимает).
- Projectivy (и потенциально другие лаунчеры) запускают клик по каналу в
  пакете владельца канала, поэтому в приложении есть невидимый
  `ForwardActivity`-трамплин, который перенаправляет ссылку в `com.kinopub`.
- Синхронизация — WorkManager (раз в 12 ч + `BootReceiver` после загрузки):
  TvProvider на ТВ-приставках теряет данные при ребуте, канал при этом
  пересоздаётся автоматически.

## Сборка

Стандартный тулчейн: JDK 17+, Android SDK (platforms;android-34,
build-tools;34.0.0), Gradle 8.9+.

```bash
export JAVA_HOME=<путь к JDK 17>
export ANDROID_HOME=<путь к Android SDK>
gradle --no-daemon assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Установка на устройство по сети (предварительно включить отладку по сети):

```bash
adb connect <IP-адрес устройства>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```
