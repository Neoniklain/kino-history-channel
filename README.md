# kino.pub Watch History — home screen channel for Android TV

**[Русский](README.ru.md)**

A mini app that publishes a "kino.pub History" channel on the TV home screen:
up to 30 most recently watched titles from the kino.pub API, with posters.
Clicking a title opens its page in the official kino.pub app (`com.kinopub`) —
playback stays there. Works with launchers that render Android TV channels
(verified with Projectivy Launcher).

## Installation

1. Install the APK from the [releases page](../../releases).
2. Open "История kino.pub" in the app list → "Привязать устройство" (Pair device).
3. Enter the shown code at https://kino.watch/device while logged into your account.

That's it: the channel syncs every 12 hours and after device reboot.
Add the row to the home screen via your launcher (in Projectivy: Settings →
Edit channels).

## How it works

- Auth: OAuth2 device flow against `https://api.service-kp.com`. Client
  credentials are embedded in `Prefs.java` by default — this is the public
  pair from the open-source Kodi client for kino.pub; you can request your own
  from support@kino.pub and put it in the same file. The refresh token lives
  only in the app's SharedPreferences on the device.
- `GET /v1/history` → entries are published to TvProvider as a browsable
  channel + preview programs. The deep link into the kino.pub app must be
  exactly `https://kinopub.tv/item/view/<id>` (other URL paths resolve, but
  the kino.pub activity silently drops them).
- Projectivy (and potentially other launchers) fire channel item clicks inside
  the channel owner's package, so the app ships an invisible `ForwardActivity`
  trampoline that re-dispatches the link into `com.kinopub`.
- Sync runs via WorkManager (every 12 h + a `BootReceiver` after boot):
  TvProvider on TV boxes loses its data on reboot, and the channel is then
  recreated automatically.

## Building

Standard toolchain: JDK 17+, Android SDK (platforms;android-34,
build-tools;34.0.0), Gradle 8.9+.

```bash
export JAVA_HOME=<path to JDK 17>
export ANDROID_HOME=<path to Android SDK>
gradle --no-daemon assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

Install over the network (enable network debugging on the device first):

```bash
adb connect <device IP>:5555
adb install -r app/build/outputs/apk/release/app-release.apk
```
