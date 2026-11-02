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
- Configuración del usuario con DataStore (a partir de la fase 2).
- No cambiar versiones de AGP/Kotlin/Compose salvo necesidad. AGP 9 trae soporte Kotlin integrado (no hay plugin `kotlin-android`).
- Paquetes:
  - `data` — modelos, apps instaladas, iconos, configuración.
  - `ui/home` — pantalla principal.
  - `ui/drawer` — cajón de apps (fase 2).
  - `ui/media` — widget de música (fase 3).
  - `ui/theme` — tema Compose base (colores/tipografía).
  - `theme` — motor de temas `theme.json` (fase 4).
- Textos de UI en español.
- Compilar: `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
- Un commit de git al final de cada fase. Esperar confirmación del usuario antes de empezar la siguiente fase.

## Plan por fases
- **Fase 1 – Base** ✅: manifest como launcher (HOME/DEFAULT + LAUNCHER durante desarrollo, singleTask, landscape, `<queries>`), Home oscuro con rejilla de accesos grandes (Maps, Waze, Spotify, YouTube Music, Ajustes) + botón "Apps", Toast si la app no está instalada, "atrás" bloqueado.
- **Fase 2 – Cajón de apps**: todas las apps (`LauncherApps`/`PackageManager`), orden alfabético, búsqueda, iconos asíncronos con caché, mantener presionado → "Agregar a Home" (persistido en DataStore).
- **Fase 3 – Widget de música**: `NotificationListenerService` + `MediaSessionManager`, pantalla para pedir acceso a notificaciones, controles y carátula.
- **Fase 4 – Motor de temas**: esquema `theme.json` + data class (kotlinx.serialization), carga desde almacenamiento externo de la app, aplicación a `ColorScheme` y layout, selector en ajustes, documentación en `docs/THEMES.md`.
- **Fase 5 – Pulido**: día/noche, reloj grande, ajustes del launcher (columnas, accesos, tema), revisión de rendimiento.
