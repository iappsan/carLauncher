# Desarrollo y pruebas

## Compilar
```bash
gradlew.bat assembleDebug
```
Si Gradle elige un JRE sin `jlink` (p. ej. el de la extensión Java de VS Code):
```bash
gradlew.bat assembleDebug "-Dorg.gradle.java.installations.auto-detect=false" "-Dorg.gradle.java.installations.paths=C:\Program Files\Java\jdk-25.0.5.0.2"
```

### Build optimizado (release)
Para probar en el estéreo usa el release: pesa ~1,5 MB (frente a ~13 MB el debug) y arranca y consume
menos. Está firmado con la clave debug, así que vale para instalar por adb pero **no para distribuir**.
```bash
gradlew.bat assembleRelease --no-daemon "-Dorg.gradle.java.installations.auto-detect=false" "-Dorg.gradle.java.installations.paths=C:\Program Files\Java\jdk-25.0.5.0.2"
adb install -r app/build/outputs/apk/release/app-release.apk
```
`--no-daemon` evita reutilizar un daemon de Gradle abierto por VS Code con su JRE (sin `jlink`).

Medidas en el emulador `Car_HeadUnit_1024x600` (orientativas; el estéreo real será más lento):

| | Debug | Release |
|---|---|---|
| APK | 12,9 MB | 1,5 MB |
| Arranque en frío (`am start -W`) | 1,2–1,4 s | 0,95–1,2 s |
| Heap Java en reposo | ~11 MB | ~4–5 MB |

Para medir el arranque hay que poner temporalmente otro launcher como inicio: si CarLauncher es el
launcher, Android lo relanza solo al hacer `force-stop` y la medida sale 0.

`profileinstaller` compila los perfiles de arranque de Compose en el dispositivo tras la instalación,
así que el arranque mejora a partir de la segunda o tercera vez.

## Emulador
AVD `Car_HeadUnit_1024x600` (1024×600, 160 dpi, horizontal, Android 37 con Play Store).
```bash
%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe -avd Car_HeadUnit_1024x600
```
En Android 16+ el sistema ignora la orientación fija en pantallas de ≥600dp (el emulador lo es):
no es un problema del launcher; en los head units habituales (Android 9–12) sí se respeta.

Probar 1280×720 sin otro AVD: `adb shell wm size 1280x720` / `adb shell wm density 213` (y `reset` para volver).

## Instalar y fijar como launcher
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity com.iappsan.carlauncher/.MainActivity
```

## Temas
Ver [THEMES.md](THEMES.md). Copiar un tema al emulador:
```bash
adb shell mkdir -p /sdcard/Android/data/com.iappsan.carlauncher/files/themes
adb push docs/examples/atardecer /sdcard/Android/data/com.iappsan.carlauncher/files/themes/
adb shell chmod -R a+rwX /sdcard/Android/data/com.iappsan.carlauncher/files/themes
```

Ojo: reinstalar la APK cuando cambian sus componentes (p. ej. un servicio nuevo) puede hacer que
Android olvide el launcher preferido; vuelve a fijarlo con `set-home-activity`.

## Acceso a notificaciones (widget de música)
Normalmente se concede desde el botón "Dar permiso" del widget. Si el head unit no tiene esa
pantalla de ajustes, se puede conceder por adb:
```bash
adb shell cmd notification allow_listener com.iappsan.carlauncher/com.iappsan.carlauncher.data.media.MediaListenerService
```
Revocar: `disallow_listener` con el mismo argumento.

En Android 13+, si la APK se instaló desde un gestor de archivos (no por adb ni tienda), Android
bloquea este permiso como "ajuste restringido": Información de la app → menú ⋮ → "Permitir ajustes restringidos".
