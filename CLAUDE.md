# CarLauncher

Launcher Android ligero para un **estéreo/head unit Android genérico** del carro (NO Android Auto ni Android Automotive OS).
Package `com.iappsan.carlauncher`, minSdk 26, Kotlin + Jetpack Compose, Gradle Kotlin DSL con version catalog (`gradle/libs.versions.toml`).

El autor es desarrollador Java/Spring Boot con poca experiencia en Kotlin: al escribir código, explica brevemente las partes idiomáticas de Kotlin/Compose que no sean obvias.

## Objetivos
1. **Ligero y fluido**: pocas dependencias (nada de Firebase ni librerías pesadas), arranque rápido, poca RAM (el estéreo tiene 2–4 GB).
2. **Temas personalizados**: cada tema es una carpeta con `theme.json` (colores, fondo, fuente, radio de botones, columnas, iconos por package) + imágenes WebP. Se cargan sin recompilar desde `getExternalFilesDir("themes")`; tema por defecto embebido en `assets/themes/default/`.
3. **Botones grandes (≥80dp)** usables sin mirar: navegación, música, teléfono/Bluetooth, ajustes, cajón de apps. Configurables.
4. **Widget de música** vía `NotificationListenerService` + `MediaSessionManager.getActiveSessions()`.
5. **Modo día/noche** (por hora al inicio).
6. **Horizontal fija**; resoluciones típicas 1024×600 y 1280×720. El layout debe adaptarse sin romperse.

## Convenciones
- MVVM ligero: `ViewModel` + `StateFlow`; coroutines para trabajo en segundo plano (apps, iconos); caché de iconos (`LruCache`).
- Inyección manual: `AppContainer` creado en `CarLauncherApp` (Application). Sin Hilt/Dagger.
- Configuración del usuario con DataStore Preferences (`settings`, en `data/SettingsStore.kt`).
- Navegación sin librería: overlays sobre Home (`Overlay` en `MainActivity`: cajón, ajustes); "atrás" o Home los cierran.
- Probar en el emulador `Car_HeadUnit_1024x600` (AVD 1024×600, 160dpi, horizontal).
- Si Gradle elige el JRE de VS Code (sin `jlink`), compilar con `--no-daemon -Dorg.gradle.java.installations.auto-detect=false -Dorg.gradle.java.installations.paths="C:\Program Files\Java\jdk-25.0.5.0.2"`.
- Probar siempre también el **release** (R8): hay errores que solo aparecen ahí.
- No cambiar versiones de AGP/Kotlin/Compose salvo necesidad. AGP 9 trae soporte Kotlin integrado (no hay plugin `kotlin-android`).
- Paquetes:
  - `data` — modelos, apps instaladas, iconos, configuración.
  - `ui/home` — pantalla principal.
  - `ui/drawer` — cajón de apps (fase 2).
  - `data/media` — servicio de notificaciones y sesiones multimedia.
  - `ui/media` — widget de música.
  - `ui/theme` — aplicación del tema en Compose (`CarLauncherTheme`, `LauncherTheme`, `ThemeViewModel`).
  - `ui/settings` — ajustes del launcher.
  - `ui/driving` — velocímetro, indicador Bluetooth, tarjeta de maniobra (`DrivingViewModel`).
  - `data/location`, `data/navigation`, `data/bluetooth` — GPS/sol, guía de Maps/Waze, Bluetooth.
  - `ui/icons` — iconos vectoriales propios (sin librería de iconos).
  - `theme` — motor de temas `theme.json`.
- Textos de UI en español.
- Comandos de adb, emulador y permisos: `docs/DEVELOPMENT.md`.
- Compilar: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
- Un commit de git al final de cada fase. Esperar confirmación del usuario antes de empezar la siguiente fase.

## Plan por fases
- **Fase 1 – Base** ✅: manifest como launcher (HOME/DEFAULT + LAUNCHER durante desarrollo, singleTask, landscape, `<queries>`), Home oscuro con rejilla de accesos grandes (Maps, Waze, Spotify, YouTube Music, Ajustes) + botón "Apps", Toast si la app no está instalada, "atrás" bloqueado.
- **Fase 2 – Cajón de apps** ✅: `LauncherApps` (con callback de instalación/desinstalación → `AppRepository.changes`), orden alfabético con `Collator`, búsqueda sin acentos, iconos asíncronos con caché (`IconLoader`), pulsación larga → "Agregar a Home" / "Información de la app"; en Home, pulsación larga → "Quitar de Home" (el botón "Apps" no se puede quitar). Accesos persistidos en DataStore (`ShortcutRepository`).
- **Fase 3 – Widget de música** ✅: `data/media/MediaListenerService` (vacío, solo para el permiso) + `MediaRepository` (sesiones activas, elige la que suena, carátula reducida), escucha solo entre onStart/onStop. Widget a la izquierda de Home (38% del ancho) con panel de permiso, estado vacío y controles grandes; iconos vectoriales propios (`MediaIcons`). Carátulas solo por Bitmap (no `ART_URI`) por ahora.
- **Fase 4 – Motor de temas** ✅: `theme/ThemeSpec` (kotlinx.serialization, todo opcional) → `ThemeRepository` (assets `builtin:<dir>` + `getExternalFilesDir("themes")` `file:<dir>`, imágenes reducidas, fuente, avisos no fatales) → `LoadedTheme` (ColorScheme + `ThemeLayout` + iconos). Compose: `CarLauncherTheme(theme)` + `LauncherTheme.layout/icons` (CompositionLocal) + `ThemeBackground`. Selector en `ui/settings/SettingsScreen` (Apps → Ajustes). Formato en `docs/THEMES.md`, ejemplo en `docs/examples/atardecer`.
- **Fase 6 – Conducción** ✅: velocímetro GPS (`data/location/LocationRepository`, LocationManager sin Play Services, solo con Home visible), día/noche **por sol** (`SunCalculator` + tests, última posición guardada en DataStore; `DayNightRepository`), tarjeta de próxima maniobra leyendo notificaciones de Maps/Waze (`data/navigation/NavigationRepository`, vía el mismo `MediaListenerService`), vista de navegación automática (panel de maniobra + `MediaBar` + lista de accesos `ShortcutList.Navigation`), Bluetooth (`data/bluetooth/BluetoothRepository`: teléfono conectado, batería best-effort, reanudar música con KEYCODE_MEDIA_PLAY al conectar, app a abrir). Permisos pedidos una vez al primer arranque y desde Ajustes → Conexiones.
- **Dock** ✅: ajuste «Disposición de accesos» (`HomeLayout.Grid`/`Dock`). En modo dock los accesos van en una fila inferior solo con iconos (`ui/home/Dock.kt`, 88dp, se desliza si no caben) y el hueco de arriba (`Stage` en `HomeScreen`) lleva el reloj; ahí irá el modelo 3D del carro (siguiente fase: Filament + `.glb`, llantas según velocidad GPS, luces según modo noche/manual).
- **Fase 5 – Pulido** ✅: `LauncherSettingsRepository` (reloj, columnas, modo día/noche + horario; `isNight` se reevalúa en onStart). Temas con `dayColors`/`dayBackground` (colores nulos → `DarkPalette`/`LightPalette` según luminancia del fondo). Reloj grande (`ui/home/Clock.kt`, tick por minuto). Ajustes por secciones (Pantalla, Día y noche, Accesos con subir/bajar/quitar/restaurar, Tema). Rendimiento: `profileinstaller`, R8 completo (se quitó `packageScope`, rompía el release), no se pinta hasta cargar el tema, barra de estado clara/oscura según fondo. Release firmado con clave debug solo para pruebas.
