# Temas de CarLauncher

Un tema es una **carpeta** con un `theme.json` y, opcionalmente, imágenes y una fuente.
Se cargan sin recompilar: se copian al estéreo y se eligen en **Apps → Ajustes → Tema**.

```
mi-tema/
├── theme.json          (obligatorio)
├── background.webp     (opcional: fondo)
├── fonts/
│   └── MiFuente.ttf    (opcional)
└── icons/
    ├── maps.webp       (opcional: iconos por app)
    └── apps.webp
```

## Dónde se copian

| Origen | Ruta | Id |
|---|---|---|
| Del usuario | `/sdcard/Android/data/com.iappsan.carlauncher/files/themes/<carpeta>/` | `file:<carpeta>` |
| Embebidos en la APK | `app/src/main/assets/themes/<carpeta>/` | `builtin:<carpeta>` |

La carpeta del usuario no necesita permisos: es el almacenamiento propio de la app. La ruta exacta
aparece en la pantalla de Ajustes (la carpeta `themes` se crea la primera vez que abres Ajustes).

### Temas incluidos

Además de **Default**, el selector ofrece cinco paletas integradas, listas para usar sin copiar
archivos: **Bosque** (verde natural), **Océano** (azul sereno), **Grafito** (gris cálido con ámbar),
**Lavanda** (violeta suave) y **Tierra** (tonos terrosos). Son oscuras y usan acentos moderados para
mantener la lectura cómoda en el estéreo.

### Copiar por adb

```bash
adb shell mkdir -p /sdcard/Android/data/com.iappsan.carlauncher/files/themes
```
```bash
adb push docs/examples/atardecer /sdcard/Android/data/com.iappsan.carlauncher/files/themes/
```
```bash
adb shell chmod -R a+rwX /sdcard/Android/data/com.iappsan.carlauncher/files/themes
```

- El `mkdir` va primero: si `themes/` no existe, `adb push` copia el *contenido* de la carpeta en
  vez de la carpeta.
- El `chmod` hace falta en Android 11+: lo que copia adb queda con dueño `shell` y la app no puede
  leerlo. En Android 9/10 (lo habitual en head units) no suele hacer falta, pero no estorba.
- Copiando por USB con el explorador de archivos (MTP) no hacen falta ninguno de los dos.

Después pulsa **Recargar temas** en Ajustes (o vuelve a abrir Ajustes). Si editas el tema que está
activo, "Recargar temas" también lo vuelve a aplicar.

> Si desinstalas la app, Android borra esa carpeta. Guarda tus temas en otro sitio.

## Ejemplo completo

Este es el tema [`examples/atardecer`](examples/atardecer/theme.json). Se admiten comentarios `//`
y comas finales.

```jsonc
{
  "name": "Atardecer",
  "author": "CarLauncher",
  "colors": {
    "background": "#1B1026",
    "onBackground": "#FFF3E6",
    "surface": "#B32A1B3D",          // #AARRGGBB: B3 ≈ 70 % opaco, deja ver el fondo
    "onSurface": "#FFF3E6",
    "onSurfaceVariant": "#E8CBB8",
    "primary": "#FF8A3D",
    "onPrimary": "#2B1200",
    "primaryContainer": "#CCB8452A",
    "onPrimaryContainer": "#FFE6D6",
    "secondaryContainer": "#4A2D52",
    "onSecondaryContainer": "#F6DDF0"
  },
  "background": {
    "image": "background.png",
    "dim": 0.25
  },
  "font": {
    "family": "serif",
    "scale": 1.05
  },
  "layout": {
    "columns": 3,
    "cornerRadius": 32,
    "spacing": 20,
    "mediaPanelWeight": 0.4,
    "iconSize": 60,
    "showLabels": true
  },
  "icons": {
    "apps": "icons/apps.png"
  }
}
```

**Todos los campos son opcionales.** Lo que no pongas toma el valor del tema por defecto, así que un
tema puede ser tan corto como:

```json
{ "name": "Verde", "colors": { "primary": "#66BB6A", "primaryContainer": "#1B5E20" } }
```

## Referencia

### Raíz

| Campo | Tipo | Defecto | Descripción |
|---|---|---|---|
| `name` | texto | `"Sin nombre"` | Nombre que aparece en el selector. |
| `author` | texto | `""` | Autor (solo informativo). |
| `colors` | objeto | ver abajo | Paleta de noche (o la única). |
| `dayColors` | objeto \| `null` | `null` | Paleta para el modo día. Sin ella, el tema se ve igual a cualquier hora. |
| `background` | objeto | sin imagen | Fondo. |
| `dayBackground` | objeto \| `null` | `null` | Fondo para el modo día. Sin él, se usa `background`. |
| `font` | objeto | `sans-serif` | Tipografía. |
| `layout` | objeto | ver abajo | Distribución. |
| `icons` | mapa | `{}` | Iconos personalizados. |

### `colors`

Formato `#RRGGBB` o `#AARRGGBB` (el primer par es la opacidad: `FF` opaco, `00` transparente).
`dayColors` tiene los mismos campos.

Los campos que falten se rellenan con la paleta oscura por defecto (la de la tabla) o, si el
`background` que has puesto es claro, con la paleta clara por defecto (la de `dayColors` del tema
Default). Así un `dayColors` con solo un par de colores también queda coherente.

| Campo | Defecto | Dónde se usa |
|---|---|---|
| `background` | `#0B0D10` | Fondo de Home (si no hay imagen), cajón y ajustes. |
| `onBackground` | `#ECEFF4` | Texto sobre el fondo. |
| `surface` | `#1A1E24` | Botones de Home, widget de música, filas de ajustes. Con transparencia deja ver la imagen de fondo. |
| `onSurface` | `#ECEFF4` | Texto e iconos sobre `surface`. |
| `onSurfaceVariant` | `#A9B1BC` | Texto secundario (artista, ayudas). |
| `primary` | `#4FC3F7` | Acento: botón play, botones principales, campo de búsqueda activo. |
| `onPrimary` | `#00222F` | Icono/texto sobre `primary`. |
| `primaryContainer` | `#0D4A63` | Botón "Apps" destacado. |
| `onPrimaryContainer` | `#CDEBFA` | Texto/icono del botón "Apps". |
| `secondaryContainer` | `#2C333D` | Botones anterior/siguiente, Ajustes/Cerrar, fondos de iconos que faltan. |
| `onSecondaryContainer` | `#DCE3EC` | Iconos/texto sobre `secondaryContainer`. |

### Modo día / noche

El launcher elige la variante en Ajustes → Día y noche: automático por horario (por defecto, día de
07:00 a 19:00), siempre noche o siempre día. En automático se decide al arrancar y cada vez que se
vuelve a Home, no en mitad del uso.

- **Noche** → `colors` + `background`.
- **Día** → `dayColors` + `dayBackground`; lo que falte se toma de la variante de noche.

Ejemplo mínimo de variante de día:

```jsonc
"dayColors": { "background": "#F4F1EA", "surface": "#FFFFFF", "primary": "#B8452A" },
"dayBackground": { "image": "background-day.webp", "dim": 0.0 }
```

### `background`

| Campo | Tipo | Defecto | Descripción |
|---|---|---|---|
| `image` | ruta \| `null` | `null` | Imagen relativa a la carpeta del tema. Se recorta para cubrir la pantalla. |
| `dim` | 0.0–1.0 | `0.0` | Capa negra encima de la imagen para que el texto se lea mejor. |

### `font`

| Campo | Tipo | Defecto | Descripción |
|---|---|---|---|
| `file` | ruta \| `null` | `null` | Fichero `.ttf`/`.otf` del tema. Tiene prioridad sobre `family`. |
| `family` | texto | `"sans-serif"` | Fuente del sistema: `sans-serif`, `serif`, `monospace`, `sans-serif-condensed`, `sans-serif-medium`… |
| `scale` | 0.8–1.4 | `1.0` | Multiplica el tamaño de todo el texto. |

Con un solo fichero de fuente, la negrita se sintetiza.

### `layout`

Los valores fuera de rango se ajustan al límite más cercano.

| Campo | Rango | Defecto | Descripción |
|---|---|---|---|
| `columns` | 1–6 | `3` | Columnas de la rejilla de accesos (el usuario puede forzar otro valor en Ajustes → Pantalla). |
| `cornerRadius` | 0–48 dp | `24` | Esquinas de botones y paneles. |
| `spacing` | 4–32 dp | `16` | Separación entre botones y márgenes. |
| `mediaPanelWeight` | 0.0–0.6 | `0.38` | Fracción del ancho para el widget de música. `0` lo oculta. |
| `iconSize` | 32–96 dp | `56` | Tamaño de los iconos en Home. |
| `showLabels` | bool | `true` | Mostrar el nombre bajo el icono en Home. |

### `icons`

Mapa de **clave → ruta de imagen**. La clave es:
- el **package** de una app (`com.google.android.apps.maps`, `com.spotify.music`, …): reemplaza su
  icono en Home y en el cajón;
- `settings`: botón de Ajustes de Home;
- `apps`: botón del cajón de apps.

Para saber el package de una app: pulsación larga en el cajón → "Información de la app", o
`adb shell pm list packages | findstr spotify`.

## Imágenes

- **WebP** es el formato recomendado (pesa menos). También valen PNG y JPG.
  Para convertir: Android Studio → clic derecho sobre la imagen → *Convert to WebP*, o
  `cwebp -q 85 fondo.png -o background.webp`.
- **Fondo**: del tamaño de la pantalla (1024×600 o 1280×720). Si es más grande se reduce al cargar.
- **Iconos**: cuadrados, 192×192 px con fondo transparente.
- Las rutas son relativas a la carpeta del tema; no se permiten `..` ni rutas absolutas.

## Errores

Un tema con errores no deja de funcionar:
- **`theme.json` mal formado** → el tema aparece en rojo en el selector y no se puede elegir.
- **Color inválido, imagen o fuente que falta** → se usa el valor por defecto de ese campo y el
  problema aparece en Ajustes como *Avisos del tema actual*.
- **El tema elegido ya no existe** (carpeta borrada) → se vuelve al tema por defecto.
