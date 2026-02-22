# Tests Automatizados (androidTest) — SDK QA

Este documento describe **cómo funcionan** los tests de instrumentación que estamos construyendo en este repositorio y **qué herramientas** estamos usando.

### Objetivo

- **Smoke tests** por Activity (audio/video) para detectar rápido:
  - Pantallas que no cargan / crashean.
  - Flujos de reproducción que no llegan a estados mínimos (ej. `onReady`, `onPlay`).
- Validar comportamiento del SDK **sin depender de logcat**, usando callbacks reales del player.

### Dónde están los tests

- **Instrumentación (device/emulator)**: `app/src/androidTest/java/com/example/sdkqa/`
  - Suites “por Activity”:
    - `AudioActivitiesBasicSmokeTest.kt`
    - `VideoActivitiesBasicSmokeTest.kt`
  - Tests dedicados:
    - `AudioLiveSmokeTest.kt`
    - `VideoLiveSmokeTest.kt`
    - `VideoReelsGestureSmokeTest.kt` (gestos)
  - Utilidades compartidas:
    - `SmokeTestUtils.kt`

### Herramientas/Librerías usadas

- **JUnit4 / AndroidJUnit4**: runner de tests instrumentados.
- **ActivityScenario** (`androidx.test.core`) para **lanzar Activities directamente** (más robusto que navegar por la UI principal).
- **Espresso** para interacciones/gestos UI:
  - `onView(...).perform(click())`, `swipeUp()`, etc.
  - `espresso-contrib` para `RecyclerViewActions` cuando hace falta interactuar con listas.
- **GrantPermissionRule** (`androidx.test:rules`) para pre-otorgar permisos y evitar prompts (ej. `POST_NOTIFICATIONS`).

### Idea central: validar por eventos (callbacks) con `TestEventBus`

En vez de “leer logcat”, los Activities registran eventos en un bus in-memory dentro del proceso de la app.

- **Bus**: `app/src/main/java/com/example/sdkqa/testing/TestEventBus.kt`
- **Emisión de eventos**: en los callbacks de `MediastreamPlayerCallback` dentro de cada Activity (audio/video), por ejemplo:
  - `VideoLive.onBuffering`, `VideoLive.onReady`, `VideoLive.onPlay`, `VideoLive.playerViewReady`
  - `AudioLive.onReady`, `AudioLive.onPlay`, etc.

En los tests:

- Se hace `TestEventBus.clear()` antes del paso que quieres medir.
- Se espera con `TestEventBus.awaitAll(expectedNames = ..., timeoutMs = ...)`.
- Si falta algo, se falla mostrando qué eventos llegaron y cuáles no.

### “Basic events” (contrato mínimo)

Para la mayoría de Activities (audio/video) usamos un set mínimo esperado (helper):

- `${prefix}.onBuffering`
- `${prefix}.onReady`
- `${prefix}.onPlay`
- `${prefix}.playerViewReady`

El helper vive en `SmokeTestUtils.kt` y se usa desde las suites:

- `AudioActivitiesBasicSmokeTest.kt`: un `@Test` por Activity de audio.
- `VideoActivitiesBasicSmokeTest.kt`: un `@Test` por Activity de video.

### Excepciones: Reels (no siempre hay `playerViewReady` / `onBuffering`)

En `VideoReelActivity` (Reels) vimos en ejecución real que **a veces no se emite** `VideoReel.playerViewReady` y también puede no aparecer `VideoReel.onBuffering`.

Por eso, para Reels el “mínimo” en arranque es:

- `VideoReel.onReady`
- `VideoReel.onPlay`

Esto está reflejado tanto en:

- `VideoActivitiesBasicSmokeTest.kt` (caso `videoReels_basic_events`)
- `VideoReelsGestureSmokeTest.kt` (arranque del test)

### Tests de gestos: Reels “swipe up” para cambiar de reel

`VideoReelsGestureSmokeTest.kt` valida un comportamiento típico de Reels:

- Arranca la Activity y espera `VideoReel.onReady` + `VideoReel.onPlay`.
- Luego repite **3 veces**:
  - Limpia eventos (`TestEventBus.clear()`).
  - Ejecuta un gesto `swipeUp()` sobre el content root.
  - Espera que aparezcan `VideoReel.onPause` y `VideoReel.onPlay`.
  - Valida además el **orden**: `onPause` debe ocurrir **antes** que `onPlay`.

### (Opcional) Validaciones básicas de UI con Espresso

Además de eventos, a veces conviene afirmar que “la UI existe”:

- Contenedor visible (`android.R.id.content`) o un `View` clave por `id`.
- Botones habilitados/visibles si aplica.

Regla práctica: mantener estas validaciones **mínimas y estables** para evitar flakiness (no asertar pixeles/posiciones).

### Cómo ejecutar los tests

En Android Studio:

- Panel **Gradle** → `app` → `Tasks` → `verification` → `connectedDebugAndroidTest`

Por consola (requiere device/emulator conectado):

```bash
./gradlew :app:connectedDebugAndroidTest
```

Para solo compilar el APK de tests (sin ejecutar):

```bash
./gradlew :app:assembleAndroidTest
```

### Cómo extender estos tests (escalabilidad)

- **Añadir más asserts por Activity**:
  - Opción A: crear un test dedicado (ej. `VideoReelsGestureSmokeTest`) cuando hay gestos/flujos especiales.
  - Opción B: mantener en las suites solo el “mínimo” (basic events) y dejar lo complejo a tests específicos.
- **Nuevos eventos**:
  - Registrar en el callback de la Activity (prefijo consistente).
  - Esperarlos en el test con `TestEventBus.awaitAll(...)`.

### Nota importante sobre procesos (instrumentation)

`TestEventBus` es in-memory. En algunos setups, el proceso de los tests puede no compartir memoria con el proceso de la app; si eso ocurriera, habría que mover el “bus” a un mecanismo persistente/IPC (por ejemplo, storage compartido) para que el test pueda observar los eventos.

