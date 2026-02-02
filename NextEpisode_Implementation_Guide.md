# Guía de Implementación: Next Episode (Siguiente Episodio)

## Resumen Ejecutivo

La funcionalidad **Next Episode** permite la reproducción automática o semi-automática de episodios consecutivos, similar a Netflix o Prime Video. El sistema muestra un overlay con opciones para ver el siguiente episodio o ver los créditos finales, con transición automática después de 5 segundos si el usuario no interactúa.

Esta característica mejora significativamente la experiencia de usuario en contenido serializado (series, podcasts, cursos, etc.) al eliminar la fricción de navegación entre episodios.

## Características Principales

✅ **Overlay visual** con botones "Ver Créditos" y "Siguiente Episodio"  
✅ **Transición automática** después de 5 segundos  
✅ **Animación de progreso** visual en el botón de siguiente episodio  
✅ **Dos modos de operación**: Automático (API) y Manual (Custom)  
✅ **Callback anticipado** 3 segundos antes del overlay  
✅ **Tiempo configurable** para mostrar el overlay  
✅ **Cancelación por usuario** (botón "Ver Créditos")  
✅ **Soporte para Android TV** con navegación por foco  

## Arquitectura del Sistema

### Componentes Principales

```
MediastreamPlayer
    ├── NextEpisodeState (estado interno)
    ├── NextEpisodeUI (elementos visuales)
    ├── nextEpisodeConfig (configuración del siguiente episodio)
    └── isManualNextEpisodeConfig (modo: API vs Manual)

MediastreamPlayerCallback
    └── nextEpisodeIncoming(nextEpisodeId: String)

Activity
    └── Implementa callback y maneja lógica custom
```

### Flujo de Datos

```
API/Configuración
    ↓
configureNextEpisodeConfig()
    ↓
nextEpisodeConfig creado
    ↓
initializeNextEpisodePreview()
    ↓
startNextEpisodeMonitoring()
    ↓
checkNextEpisodePosition() [cada frame]
    ↓
¿3s antes? → nextEpisodeIncoming() callback
    ↓
¿Tiempo alcanzado? → showNextEpisodeOverlay()
    ↓
5 segundos de espera (con animación)
    ↓
loadNextEpisode() o usuario cancela
```

## Modos de Operación

### Modo 1: Automático (API-Driven)

**Uso típico**: Series donde la API proporciona el ID del siguiente episodio.

**Características:**
- El SDK obtiene automáticamente el ID del siguiente episodio desde la API
- No requiere intervención de la app
- Transición automática sin confirmación
- El overlay se muestra automáticamente

**Flujo:**

```
Usuario reproduce episodio (type = EPISODE)
    ↓
API responde con: mediaInfo.next = "id_next_episode"
    ↓
SDK configura nextEpisodeConfig automáticamente
    ↓
Al llegar al tiempo configurado → Overlay se muestra
    ↓
5 segundos después → Carga siguiente episodio
    ↓
[REPETIR para cada episodio]
```

**Configuración:**

```kotlin
val config = MediastreamPlayerConfig().apply {
    id = "episode_id_here"
    type = MediastreamPlayerConfig.VideoTypes.EPISODE
    environment = MediastreamPlayerConfig.Environment.DEV
    // nextEpisodeTime = 15 (opcional, por defecto es 15 segundos antes del final)
}

player = MediastreamPlayer(this, config, container, playerView, supportFragmentManager)
```

**Respuesta API esperada:**

```json
{
    "next": "id_del_siguiente_episodio",
    "nextEpisodeTime": 20
}
```

### Modo 2: Manual (Custom/App-Controlled)

**Uso típico**: Playlists custom, orden no-lineal, lógica de negocio compleja.

**Características:**
- La app proporciona el ID del siguiente episodio
- Requiere confirmación explícita mediante `updateNextEpisode()`
- Callback `nextEpisodeIncoming()` se ejecuta 3 segundos antes
- El overlay solo se muestra después de confirmación
- Control total sobre qué reproducir a continuación

**Flujo:**

```
App configura nextEpisodeId manualmente
    ↓
SDK marca como "modo manual"
    ↓
3 segundos antes del tiempo → nextEpisodeIncoming() callback
    ↓
App prepara siguiente config y llama updateNextEpisode()
    ↓
SDK confirma y muestra overlay
    ↓
5 segundos después → Carga siguiente episodio
```

**Implementación en la App:**

```kotlin
// 1. Configuración inicial con nextEpisodeId
val config = MediastreamPlayerConfig().apply {
    id = "first_video_id"
    type = MediastreamPlayerConfig.VideoTypes.VOD
    nextEpisodeId = "second_video_id"  // ID del siguiente
    nextEpisodeTime = 15  // Mostrar 15s antes del final
}

// 2. Implementar callback
val callback = object : MediastreamPlayerCallback {
    override fun nextEpisodeIncoming(nextEpisodeId: String) {
        // Se ejecuta 3 segundos antes del overlay
        Log.d(TAG, "Preparando siguiente episodio: $nextEpisodeId")
        
        // Preparar configuración del siguiente video
        val nextConfig = MediastreamPlayerConfig().apply {
            id = nextEpisodeId
            type = MediastreamPlayerConfig.VideoTypes.VOD
            nextEpisodeId = "third_video_id"  // ID del subsiguiente
        }
        
        // Confirmar al SDK
        player?.updateNextEpisode(nextConfig)
    }
    
    // ... otros callbacks
}

player?.addPlayerCallback(callback)
```

## Configuración Detallada

### Parámetros de Configuración

#### 1. `nextEpisodeId` (String, opcional)

Activa el modo manual especificando el ID del siguiente episodio.

```kotlin
config.nextEpisodeId = "id_del_siguiente_episodio"
```

**Notas:**
- Si se especifica, activa modo manual automáticamente
- Sobrescribe el valor de la API si existe
- Requiere llamar a `updateNextEpisode()` en el callback

#### 2. `nextEpisodeTime` (Int, opcional, en segundos)

Tiempo antes del final del video para mostrar el overlay.

```kotlin
config.nextEpisodeTime = 20  // Mostrar 20 segundos antes del final
```

**Valores típicos:**
- `10`: Para videos cortos (< 5 minutos)
- `15`: Valor por defecto (recomendado)
- `20-30`: Para videos largos (> 30 minutos)
- `45-60`: Para películas

**Notas:**
- Si no se especifica, usa 15 segundos por defecto
- Se puede configurar desde la API o localmente
- Configuración local tiene prioridad sobre API

#### 3. `loadNextAutomatically` (Boolean, interno)

Controla si el siguiente episodio se carga automáticamente o requiere interacción.

```kotlin
// Este valor se configura internamente por el SDK
config.loadNextAutomatically = true  // Para tipos EPISODE y LIVE
```

**Comportamiento:**
- `true`: Para `VideoTypes.EPISODE` y `VideoTypes.LIVE`
- `false`: Para otros tipos de video

### Prioridades de Configuración

El SDK sigue esta jerarquía de configuración:

```
1. nextEpisodeId (configuración local)
   ├─> Activa modo manual
   └─> Sobrescribe API
   
2. nextEpisodeTime (configuración local)
   ├─> Sobrescribe API
   └─> Si no existe, usa 15s por defecto
   
3. API next (respuesta del servidor)
   ├─> Solo si no hay nextEpisodeId local
   └─> Activa modo automático
   
4. API nextEpisodeTime (respuesta del servidor)
   └─> Solo si no hay configuración local
```

## Implementación del Callback

### Callback: `nextEpisodeIncoming(nextEpisodeId: String)`

Este callback se ejecuta **3 segundos antes** de mostrar el overlay, dando tiempo a la app para preparar el siguiente video.

#### Firma del Método

```kotlin
fun nextEpisodeIncoming(nextEpisodeId: String) {}
```

#### Parámetros

- **`nextEpisodeId`**: ID del siguiente episodio que está por cargarse

#### ¿Cuándo se ejecuta?

**Timeline típica (con nextEpisodeTime=15):**

```
Video: 0s ────────────────────────────────────────────> 300s (5 min)
                                           ↓            ↓
                                        282s         285s
                                          ↓            ↓
                            nextEpisodeIncoming()  Overlay visible
                                (callback)         (UI mostrada)
                                          ↓            ↓
                                     3s antes     15s antes del final
```

**Cálculos internos:**

```kotlin
// Ejemplo con video de 300s y nextEpisodeTime=15
val duration = 300000L  // 300 segundos = 5 minutos
val nextTime = 15       // segundos

val appearTime = duration - (nextTime * 1000L)
// appearTime = 300000 - 15000 = 285000ms (285s)

val callbackTime = appearTime - 3000L
// callbackTime = 285000 - 3000 = 282000ms (282s)
```

#### Implementación en Modo Manual

```kotlin
private val nextEpisodeIds = listOf(
    "video_1_id",
    "video_2_id", 
    "video_3_id"
)
private var currentIndex = 0

override fun nextEpisodeIncoming(nextEpisodeId: String) {
    Log.d(TAG, "Next episode incoming: $nextEpisodeId")
    
    // Verificar que hay más episodios
    if (currentIndex < nextEpisodeIds.size) {
        val nextConfig = MediastreamPlayerConfig().apply {
            id = nextEpisodeIds[currentIndex]
            type = MediastreamPlayerConfig.VideoTypes.VOD
            environment = MediastreamPlayerConfig.Environment.PRODUCTION
            
            // Si hay otro más después, configurarlo
            val nextNextIndex = currentIndex + 1
            if (nextNextIndex < nextEpisodeIds.size) {
                nextEpisodeId = nextEpisodeIds[nextNextIndex]
            }
        }
        
        currentIndex++
        
        // CRÍTICO: Confirmar al SDK
        player?.updateNextEpisode(nextConfig)
    } else {
        Log.d(TAG, "No hay más episodios")
        // No llamar updateNextEpisode() = no se mostrará overlay
    }
}
```

#### Implementación en Modo Automático

```kotlin
override fun nextEpisodeIncoming(nextEpisodeId: String) {
    // En modo automático, el callback es solo informativo
    Log.d(TAG, "Siguiente episodio se cargará: $nextEpisodeId")
    
    // Opcional: Tracking de analytics
    analytics.trackEvent("next_episode_preview", mapOf(
        "current_id" to currentVideoId,
        "next_id" to nextEpisodeId
    ))
    
    // NO es necesario llamar updateNextEpisode() en modo automático
}
```

## Método: `updateNextEpisode(config: MediastreamPlayerConfig)`

Este método es **crítico en modo manual**. Confirma al SDK que la app está lista para cargar el siguiente episodio.

### Firma

```kotlin
fun updateNextEpisode(config: MediastreamPlayerConfig)
```

### Parámetros

- **`config`**: Configuración completa del siguiente video, incluyendo:
  - `id`: ID del video
  - `type`: Tipo de video
  - `environment`: Entorno (DEV/PRODUCTION)
  - `nextEpisodeId`: ID del episodio subsiguiente (si aplica)
  - `nextEpisodeTime`: Tiempo para el siguiente (si aplica)

### ¿Cuándo llamar?

**✅ Siempre llamar desde `nextEpisodeIncoming()` en modo manual:**

```kotlin
override fun nextEpisodeIncoming(nextEpisodeId: String) {
    val nextConfig = createConfigFor(nextEpisodeId)
    player?.updateNextEpisode(nextConfig)  // AQUÍ
}
```

**❌ NO llamar en modo automático:**

```kotlin
// En modo automático, el SDK maneja todo internamente
// Solo implementar nextEpisodeIncoming() para logging/analytics
```

### Comportamiento

Cuando se llama `updateNextEpisode()`:

1. **Guarda la configuración confirmada** internamente
2. **Si está en tiempo de overlay** → Muestra el overlay inmediatamente
3. **Si el overlay ya está visible** → Actualiza la config que se usará
4. **Si se presionó botón "Next"** → Carga inmediatamente sin esperar

### Ejemplo Completo

```kotlin
class MyPlayerActivity : AppCompatActivity() {
    
    private var player: MediastreamPlayer? = null
    private val playlist = listOf("id1", "id2", "id3", "id4")
    private var currentIndex = 0
    
    private fun setupPlayer() {
        val config = MediastreamPlayerConfig().apply {
            id = playlist[currentIndex]
            type = MediastreamPlayerConfig.VideoTypes.VOD
            nextEpisodeId = playlist.getOrNull(currentIndex + 1)
            nextEpisodeTime = 15
        }
        
        player = MediastreamPlayer(this, config, container, playerView, supportFragmentManager)
        player?.addPlayerCallback(createCallback())
    }
    
    private fun createCallback() = object : MediastreamPlayerCallback {
        
        override fun nextEpisodeIncoming(nextEpisodeId: String) {
            Log.d(TAG, "Preparando siguiente: $nextEpisodeId")
            
            // Incrementar índice para el siguiente
            currentIndex++
            
            // Verificar que no sea el último
            if (currentIndex < playlist.size) {
                val nextConfig = MediastreamPlayerConfig().apply {
                    id = playlist[currentIndex]
                    type = MediastreamPlayerConfig.VideoTypes.VOD
                    environment = MediastreamPlayerConfig.Environment.PRODUCTION
                    
                    // Configurar el subsiguiente si existe
                    val nextNextIndex = currentIndex + 1
                    if (nextNextIndex < playlist.size) {
                        nextEpisodeId = playlist[nextNextIndex]
                        nextEpisodeTime = 15
                    }
                }
                
                // Confirmar
                player?.updateNextEpisode(nextConfig)
            }
        }
        
        override fun onEnd() {
            if (currentIndex >= playlist.size - 1) {
                Log.d(TAG, "Playlist completada")
                finish()
            }
        }
        
        // ... otros callbacks ...
    }
}
```

## Overlay Visual

### Diseño del Overlay

El overlay se muestra sobre el video con un fondo semi-transparente oscuro y dos botones en la esquina inferior derecha.

**Componentes:**

```
┌─────────────────────────────────────────────┐
│                                             │
│           [Video reproduciéndose]           │
│                                             │
│                                             │
│                   [Fondo semi-transparente] │
│                                             │
│                                             │
│                          ┌──────┐ ┌───────┐│
│                          │ Ver  │ │ Next  ││
│                          │Créd. │ │Episode││
│                          └──────┘ └───────┘│
└─────────────────────────────────────────────┘
                           [Gris]  [Verde+Anim]
```

### Elementos del Overlay

#### 1. Fondo Semi-Transparente

```xml
android:background="#CC000000"
```

- Color: Negro con 80% de opacidad
- Cubre toda la pantalla
- Permite ver el video de fondo

#### 2. Botón "Ver Créditos" / "Watch Credits"

```xml
<Button
    android:id="@+id/watch_credits_container"
    android:layout_width="150dp"
    android:layout_height="56dp"
    android:text="@string/watch_credits" />
```

**Comportamiento:**
- Al hacer clic: Cancela la transición automática
- El overlay se oculta
- El video continúa hasta el final
- El usuario ve los créditos completos

#### 3. Botón "Siguiente Episodio" / "Next Episode"

```xml
<FrameLayout android:id="@+id/next_episode_container">
    <!-- Animación de relleno verde -->
    <View android:id="@+id/watch_credits_animation_fill" />
    
    <!-- Texto del botón -->
    <TextView android:text="@string/next_episode" />
</FrameLayout>
```

**Comportamiento:**
- Muestra una animación de relleno verde de 0% a 100% en 5 segundos
- Al hacer clic: Carga el siguiente episodio inmediatamente
- Si no se hace clic en 5s: Transición automática

### Animación Visual

```
Tiempo: 0s ──────────────────────────────────> 5s
        │                                      │
Botón:  │░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│
        │                                      │
Relleno:│█                                     │  (0%)
        │████                                  │  (20%)
        │████████                              │  (40%)
        │████████████                          │  (60%)
        │████████████████                      │  (80%)
        │████████████████████████████████████│  (100%)
        │                                      │
        └──────────────────────────────────────┘
        Overlay visible                  Auto-load
```

**Implementación de la animación:**

```kotlin
val targetWidth = nextEpisodeButton.width
nextEpisodeUI.fillAnimator = ValueAnimator.ofInt(0, targetWidth).apply {
    duration = 5000L  // 5 segundos
    interpolator = LinearInterpolator()
    addUpdateListener { animator ->
        val value = animator.animatedValue as Int
        animationFill.layoutParams.width = value
        animationFill.requestLayout()
    }
    start()
}
```

### Soporte para Android TV

El overlay incluye navegación por foco optimizada para TV:

```xml
<Button
    android:id="@+id/watch_credits_container"
    android:nextFocusRight="@id/next_episode_container"
    android:nextFocusLeft="@id/watch_credits_container" />

<FrameLayout
    android:id="@+id/next_episode_container"
    android:nextFocusRight="@id/next_episode_container"
    android:nextFocusLeft="@id/watch_credits_container"
    android:focusable="true"
    android:clickable="true" />
```

**Comportamiento en TV:**
- El foco se asigna automáticamente al botón "Ver Créditos" (100ms después)
- D-pad izquierda/derecha: Navega entre botones
- D-pad select/center: Activa el botón enfocado

## Estados Internos del Sistema

### NextEpisodeState

Gestiona el estado de monitoreo y transición:

```kotlin
private data class NextEpisodeState(
    var appearTime: Long = 0L,          // Tiempo para mostrar overlay
    var callbackTime: Long = 0L,        // Tiempo para ejecutar callback
    var monitoringActive: Boolean = false, // Monitoreo activo
    var overlayVisible: Boolean = false,   // Overlay visible
    var callbackEmitted: Boolean = false,  // Callback ya ejecutado
    var userDismissed: Boolean = false     // Usuario canceló
)
```

**Transiciones de estado:**

```
IDLE → MONITORING → CALLBACK_EMITTED → OVERLAY_VISIBLE → TRANSITIONING
  ↓         ↓              ↓                  ↓                ↓
Start    Position     nextEpisodeIncoming() Show overlay   Load next
playback  tracking    se ejecuta            (UI visible)   episode

                                                    ↓
                                            USER_DISMISSED
                                                    ↓
                                            IDLE (overlay hidden)
```

### NextEpisodeUI

Gestiona los elementos visuales:

```kotlin
private data class NextEpisodeUI(
    var overlay: View? = null,                  // Vista del overlay
    var fillAnimator: ValueAnimator? = null,    // Animación de relleno
    var autoTransitionRunnable: Runnable? = null, // Timer de 5s
    var focusRunnable: Runnable? = null,        // Asignar foco (TV)
    var confirmedConfig: MediastreamPlayerConfig? = null // Config confirmada (modo manual)
)
```

## Timeline Completa de Ejecución

### Ejemplo: Video de 5 minutos (300s) con nextEpisodeTime=15

```
Tiempo (s):  0 ──── 60 ──── 120 ──── 180 ──── 240 ──── 282 ─ 285 ──── 290 ─ 300
             │      │       │        │        │        │     │      │     │
Reproducción:├──────┴───────┴────────┴────────┴────────┴─────┴──────┴─────┤
             │                                          │     │      │     │
Estado:      PLAYING ───────────────────────────────────┤     │      │     END
                                                        │     │      │
Eventos:                                                │     │      │
                                    nextEpisodeIncoming()    │      │
                                    (282s - 3s antes)  │     │      │
                                                        │     │      │
                                                        │  Overlay   │
                                                        │  visible   │
                                                        │  (285s)    │
                                                        │     │      │
                                                        │     │  Animación
                                                        │     │  completa
                                                        │     │  (290s)
                                                        │     │      │
                                                        │     │   Auto-load
                                                        │     │   next (290s)
                                                        │     │      │
                                                        └─────┴──────┴───>
                                                        [5 segundos]
```

### Detalle de Cada Fase

#### Fase 1: Reproducción Normal (0s - 282s)

```kotlin
// Estado
monitoringActive = true
callbackEmitted = false
overlayVisible = false
userDismissed = false

// El SDK verifica la posición cada frame
checkNextEpisodePosition(currentPosition)
```

#### Fase 2: Callback Anticipado (282s)

```kotlin
// Se ejecuta el callback
if (currentPosition >= callbackTime && !callbackEmitted) {
    callbackEmitted = true
    callback?.nextEpisodeIncoming(nextEpisodeId)
    
    // En modo manual: App debe llamar updateNextEpisode()
    // En modo automático: No se requiere acción
}
```

#### Fase 3: Mostrar Overlay (285s)

```kotlin
// Se muestra el overlay
if (currentPosition >= appearTime) {
    // Modo automático: Mostrar inmediatamente
    // Modo manual: Solo si updateNextEpisode() fue llamado
    
    overlay.visibility = View.VISIBLE
    overlayVisible = true
    
    // Iniciar animación de relleno (5 segundos)
    fillAnimator.start()
    
    // Programar transición automática
    handler.postDelayed({
        loadNextEpisode()
    }, 5000L)
}
```

#### Fase 4: Espera de Usuario (285s - 290s)

**Opción A: Usuario hace clic en "Next Episode"**

```kotlin
nextEpisodeButton.onClick {
    // Cancelar timer automático
    handler.removeCallbacks(autoTransitionRunnable)
    
    // Cargar inmediatamente
    loadNextEpisode()
}
```

**Opción B: Usuario hace clic en "Watch Credits"**

```kotlin
watchCreditsButton.onClick {
    // Cancelar todo
    handler.removeCallbacks(autoTransitionRunnable)
    fillAnimator.cancel()
    
    // Ocultar overlay
    overlay.visibility = View.GONE
    overlayVisible = false
    userDismissed = true
    
    // Video continúa hasta el final
}
```

**Opción C: No hay interacción (espera 5s)**

```kotlin
// Después de 5 segundos
handler.postDelayed({
    loadNextEpisode()
}, 5000L)
```

#### Fase 5: Carga del Siguiente (290s o antes si click)

```kotlin
private fun loadNextEpisode() {
    monitoringActive = false
    
    // Obtener configuración
    val config = confirmedConfig ?: nextEpisodeConfig
    
    // Ocultar overlay
    overlay.visibility = View.GONE
    overlayVisible = false
    
    // Cargar siguiente episodio
    reloadPlayer(config)
    
    // El ciclo se reinicia para el nuevo episodio
}
```

## Casos de Uso Completos

### Caso 1: Serie con API (Modo Automático)

**Escenario**: Plataforma de streaming con series donde el backend gestiona el orden.

**Configuración:**

```kotlin
// Episodio 1 de la serie
val config = MediastreamPlayerConfig().apply {
    id = "serie_temporada1_episodio1"
    type = MediastreamPlayerConfig.VideoTypes.EPISODE
    environment = MediastreamPlayerConfig.Environment.PRODUCTION
}

player = MediastreamPlayer(this, config, container, playerView, supportFragmentManager)

// Callback solo para tracking
player?.addPlayerCallback(object : MediastreamPlayerCallback {
    override fun nextEpisodeIncoming(nextEpisodeId: String) {
        // Analytics
        analytics.track("preview_next_episode", mapOf(
            "current" to config.id,
            "next" to nextEpisodeId
        ))
    }
    
    override fun onNewSourceAdded(config: MediastreamPlayerConfig) {
        // Nuevo episodio cargado
        Log.d(TAG, "Reproduciendo: ${config.id}")
    }
    
    // ... otros callbacks ...
})
```

**Respuesta API:**

```json
{
    "id": "serie_temporada1_episodio1",
    "next": "serie_temporada1_episodio2",
    "nextEpisodeTime": 20,
    "title": "Episodio 1: El Comienzo"
}
```

**Resultado:**
- El SDK gestiona todo automáticamente
- Después de cada episodio, carga el siguiente
- Sin intervención de la app

### Caso 2: Playlist Custom (Modo Manual)

**Escenario**: App de cursos educativos con progreso del usuario.

**Implementación completa:**

```kotlin
class CoursePlayerActivity : AppCompatActivity() {
    
    private var player: MediastreamPlayer? = null
    
    // Playlist de lecciones del curso
    private val courseLessons = listOf(
        CourseLesson("lesson_1", "Introducción"),
        CourseLesson("lesson_2", "Conceptos Básicos"),
        CourseLesson("lesson_3", "Práctica 1"),
        CourseLesson("lesson_4", "Conceptos Avanzados"),
        CourseLesson("lesson_5", "Proyecto Final")
    )
    
    private var currentLessonIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_player)
        
        setupPlayer()
    }
    
    private fun setupPlayer() {
        val firstLesson = courseLessons[currentLessonIndex]
        
        val config = MediastreamPlayerConfig().apply {
            id = firstLesson.id
            type = MediastreamPlayerConfig.VideoTypes.VOD
            environment = MediastreamPlayerConfig.Environment.PRODUCTION
            
            // Configurar siguiente si existe
            if (currentLessonIndex + 1 < courseLessons.size) {
                nextEpisodeId = courseLessons[currentLessonIndex + 1].id
                nextEpisodeTime = 10  // 10s antes para lecciones cortas
            }
        }
        
        player = MediastreamPlayer(
            this, 
            config, 
            findViewById(R.id.player_container),
            findViewById(R.id.player_view),
            supportFragmentManager
        )
        
        player?.addPlayerCallback(createCallback())
    }
    
    private fun createCallback() = object : MediastreamPlayerCallback {
        
        override fun nextEpisodeIncoming(nextEpisodeId: String) {
            Log.d(TAG, "Preparando siguiente lección: $nextEpisodeId")
            
            // Guardar progreso del usuario
            saveUserProgress(courseLessons[currentLessonIndex].id)
            
            // Preparar siguiente lección
            currentLessonIndex++
            
            if (currentLessonIndex < courseLessons.size) {
                val nextLesson = courseLessons[currentLessonIndex]
                
                val nextConfig = MediastreamPlayerConfig().apply {
                    id = nextLesson.id
                    type = MediastreamPlayerConfig.VideoTypes.VOD
                    environment = MediastreamPlayerConfig.Environment.PRODUCTION
                    
                    // Si hay otra lección después
                    val nextNextIndex = currentLessonIndex + 1
                    if (nextNextIndex < courseLessons.size) {
                        nextEpisodeId = courseLessons[nextNextIndex].id
                        nextEpisodeTime = 10
                    }
                }
                
                // CRÍTICO: Confirmar
                player?.updateNextEpisode(nextConfig)
                
                // Actualizar UI de la app
                updateCourseProgress()
            } else {
                Log.d(TAG, "Curso completado!")
                // No llamar updateNextEpisode() = no habrá overlay
            }
        }
        
        override fun onNewSourceAdded(config: MediastreamPlayerConfig) {
            // Nueva lección cargada
            val currentLesson = courseLessons[currentLessonIndex]
            Log.d(TAG, "Reproduciendo: ${currentLesson.title}")
            
            // Actualizar título en UI
            tvLessonTitle.text = currentLesson.title
            tvProgress.text = "${currentLessonIndex + 1} / ${courseLessons.size}"
        }
        
        override fun onEnd() {
            if (currentLessonIndex >= courseLessons.size - 1) {
                // Última lección completada
                showCourseCompletionDialog()
            }
        }
        
        // ... otros callbacks ...
    }
    
    private fun saveUserProgress(lessonId: String) {
        // Guardar en BD o API
        courseRepository.markLessonCompleted(lessonId)
    }
    
    private fun updateCourseProgress() {
        val progress = ((currentLessonIndex + 1).toFloat() / courseLessons.size) * 100
        progressBar.progress = progress.toInt()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.releasePlayer()
    }
}

data class CourseLesson(
    val id: String,
    val title: String
)
```

### Caso 3: Playlist con Ramificación

**Escenario**: App de videos interactivos donde el usuario elige el camino.

```kotlin
class InteractivePlayerActivity : AppCompatActivity() {
    
    private var player: MediastreamPlayer? = null
    private var currentPath = "main"
    
    private val videoTree = mapOf(
        "intro" to VideoNode(
            id = "intro_video",
            choices = listOf("path_a", "path_b")
        ),
        "path_a" to VideoNode(
            id = "path_a_video",
            choices = listOf("path_a_ending")
        ),
        "path_b" to VideoNode(
            id = "path_b_video",
            choices = listOf("path_b_ending")
        )
    )
    
    override fun nextEpisodeIncoming(nextEpisodeId: String) {
        // Pausar y mostrar diálogo de elección
        player?.pause()
        
        showChoiceDialog { selectedPath ->
            val nextNode = videoTree[selectedPath]!!
            
            val nextConfig = MediastreamPlayerConfig().apply {
                id = nextNode.id
                type = MediastreamPlayerConfig.VideoTypes.VOD
                
                if (nextNode.choices.isNotEmpty()) {
                    // Usar el primer choice como next por defecto
                    // El usuario puede elegir otro en el callback
                    nextEpisodeId = videoTree[nextNode.choices.first()]!!.id
                }
            }
            
            player?.updateNextEpisode(nextConfig)
            player?.play()
        }
    }
}
```

## Mejores Prácticas

### 1. Gestión de Estado

**✅ Hacer:**

```kotlin
// Resetear índice al cambiar de modo/playlist
fun switchToNewPlaylist(newPlaylist: List<String>) {
    currentIndex = 0
    playlist = newPlaylist
    
    val config = createConfigForIndex(0)
    player?.reloadPlayer(config)
}

// Validar que el índice no se salga de rango
if (currentIndex < playlist.size) {
    val nextConfig = createConfigForIndex(currentIndex)
    player?.updateNextEpisode(nextConfig)
}
```

**❌ Evitar:**

```kotlin
// No asumir que siempre hay siguiente
currentIndex++  // Puede causar IndexOutOfBoundsException
val nextId = playlist[currentIndex]  // ❌ Sin validación
```

### 2. Timing Apropiado

**✅ Hacer:**

```kotlin
// Ajustar según duración del contenido
val nextEpisodeTime = when {
    videoDuration < 300 -> 10      // Videos cortos: 10s
    videoDuration < 1800 -> 15     // Videos medianos: 15s
    videoDuration < 3600 -> 20     // Videos largos: 20s
    else -> 30                      // Películas: 30s
}

config.nextEpisodeTime = nextEpisodeTime
```

**❌ Evitar:**

```kotlin
// Siempre 15s sin considerar duración
config.nextEpisodeTime = 15  // Puede ser muy corto o muy largo
```

### 3. Callback Responsivo

**✅ Hacer:**

```kotlin
override fun nextEpisodeIncoming(nextEpisodeId: String) {
    // Procesamiento rápido y síncrono
    val nextConfig = prepareNextConfig(nextEpisodeId)
    player?.updateNextEpisode(nextConfig)
    
    // Operaciones pesadas en background
    lifecycleScope.launch {
        saveProgress()
        updateAnalytics()
    }
}
```

**❌ Evitar:**

```kotlin
override fun nextEpisodeIncoming(nextEpisodeId: String) {
    // NO hacer operaciones lentas síncronas
    Thread.sleep(2000)  // ❌ Bloquea el hilo principal
    val data = fetchDataFromServer()  // ❌ Red en hilo principal
    player?.updateNextEpisode(config)
}
```

### 4. Manejo de Errores

**✅ Hacer:**

```kotlin
override fun nextEpisodeIncoming(nextEpisodeId: String) {
    try {
        val nextConfig = createNextConfig(nextEpisodeId)
        player?.updateNextEpisode(nextConfig)
    } catch (e: Exception) {
        Log.e(TAG, "Error preparando siguiente episodio", e)
        // Notificar al usuario
        Toast.makeText(this, "No se pudo cargar el siguiente video", Toast.LENGTH_SHORT).show()
        // No llamar updateNextEpisode() = no habrá transición
    }
}

override fun onError(error: String?) {
    Log.e(TAG, "Error del player: $error")
    // Implementar lógica de retry o fallback
}
```

### 5. Limpieza de Recursos

**✅ Hacer:**

```kotlin
override fun onDestroy() {
    super.onDestroy()
    
    // Limpiar referencias
    player?.releasePlayer()
    player = null
    
    // Cancelar coroutines
    lifecycleScope.coroutineContext.cancelChildren()
}

override fun onPause() {
    super.onPause()
    
    // En modo manual, considerar pausar
    if (isFinishing) {
        player?.pause()
    }
}
```

## Testing y Validación

### Test Cases Críticos

#### Test 1: Transición Automática en Modo API

**Objetivo**: Verificar que el siguiente episodio se carga automáticamente.

**Pasos:**
1. Configurar player con `type = EPISODE`
2. Cargar video con API que incluye campo `next`
3. Reproducir hasta 15 segundos antes del final
4. Verificar que aparece el overlay
5. No interactuar (esperar 5 segundos)
6. Verificar que carga el siguiente episodio automáticamente

**Resultado esperado:**
✅ Overlay aparece en el tiempo correcto  
✅ Animación de relleno funciona  
✅ Transición automática después de 5s  
✅ Siguiente episodio comienza a reproducirse  

#### Test 2: Confirmación Manual

**Objetivo**: Verificar que modo manual requiere confirmación.

**Pasos:**
1. Configurar con `nextEpisodeId = "video_2"`
2. Reproducir hasta tiempo del callback
3. Verificar que `nextEpisodeIncoming()` se ejecuta
4. NO llamar `updateNextEpisode()`
5. Verificar que NO aparece el overlay

**Resultado esperado:**
✅ Callback se ejecuta 3s antes  
✅ Overlay NO aparece sin confirmación  
✅ Video termina normalmente  

#### Test 3: Cancelación por Usuario

**Objetivo**: Verificar que usuario puede cancelar.

**Pasos:**
1. Llegar al punto donde aparece el overlay
2. Hacer clic en "Ver Créditos"
3. Verificar que overlay desaparece
4. Verificar que video continúa hasta el final
5. Verificar que NO se carga el siguiente

**Resultado esperado:**
✅ Overlay desaparece inmediatamente  
✅ Animación se cancela  
✅ Video continúa normalmente  
✅ No hay transición automática  

#### Test 4: Click Inmediato en Next

**Objetivo**: Verificar carga inmediata al hacer clic.

**Pasos:**
1. Llegar al overlay
2. Hacer clic inmediatamente en "Siguiente Episodio"
3. Verificar transición sin esperar 5s

**Resultado esperado:**
✅ Transición inmediata (< 1s)  
✅ Timer de 5s cancelado  
✅ Animación cancelada  
✅ Siguiente video comienza  

#### Test 5: Playlist Completa

**Objetivo**: Verificar comportamiento al final de playlist.

**Pasos:**
1. Configurar playlist de 3 videos
2. Reproducir todos hasta el tercero
3. En el último, no configurar `nextEpisodeId`
4. Verificar que NO aparece overlay
5. Verificar que video termina normalmente

**Resultado esperado:**
✅ Overlay NO aparece en último video  
✅ Callback NO se ejecuta  
✅ Video termina y detiene reproducción  

#### Test 6: Seek Durante Countdown

**Objetivo**: Verificar comportamiento al hacer seek.

**Pasos:**
1. Llegar al overlay (animación en progreso)
2. Hacer seek hacia atrás (antes del tiempo del overlay)
3. Verificar que overlay desaparece
4. Avanzar nuevamente al tiempo del overlay
5. Verificar que overlay reaparece

**Resultado esperado:**
✅ Overlay se oculta al hacer seek atrás  
✅ Estado se resetea correctamente  
✅ Overlay reaparece al volver al tiempo  
✅ Animación se reinicia  

### Métricas de Validación

```kotlin
// Timing
val callbackDelay = 3000L  // 3 segundos antes del overlay
val overlayDuration = 5000L  // 5 segundos de countdown
val transitionTime = < 1000L  // Menos de 1s para cargar siguiente

// Precisión
val timingAccuracy = ± 200ms  // Tolerancia de timing
val positionAccuracy = ± 1s   // Precisión de posición

// Estabilidad
val crashRate = 0%  // Sin crashes
val transitionSuccessRate = 100%  // Todas las transiciones exitosas
```

## Debugging

### Logs Importantes

El SDK incluye logs detallados para debugging:

```kotlin
// Activar logs de Next Episode
config.isDebug = true

// Logs que verás
[MP-Debug] Next: Monitoring active (appear: 285000ms, callback: 282000ms)
[MP-Debug] Next: Callback emitted at 282500ms - nextId: video_2
[MP-Debug] Next (Manual): Waiting for app confirmation
[MP-Debug] Next: updateNextEpisode called - video_2
[MP-Debug] Next (Manual): Showing overlay at 285100ms
[MP-Debug] Next: Overlay visible, starting 5s countdown
[MP-Debug] Next: Auto-transition timeout reached
[MP-Debug] Next: Loading next episode - video_2
```

### Comandos ADB para Testing

```bash
# Ver logs filtrados por Next Episode
adb logcat | grep "Next:"

# Simular presión rápida de botones (TV)
adb shell input keyevent KEYCODE_DPAD_RIGHT
adb shell input keyevent KEYCODE_DPAD_CENTER

# Verificar estado del player
adb shell dumpsys media_session

# Forzar GC para probar memory leaks
adb shell am dumpheap <package> /data/local/tmp/heap.hprof
```

### Problemas Comunes

#### Problema 1: Overlay No Aparece

**Síntomas:**
- Video llega al final
- Callback se ejecuta
- Pero overlay no se muestra

**Causas posibles:**
1. Modo manual sin llamar `updateNextEpisode()`
2. `nextEpisodeConfig` es null
3. Usuario ya canceló (userDismissed = true)

**Solución:**

```kotlin
// Verificar logs
[MP-Debug] Next: shouldEnableNextEpisode = false

// En modo manual, asegurar llamar updateNextEpisode()
override fun nextEpisodeIncoming(nextEpisodeId: String) {
    val config = createNextConfig(nextEpisodeId)
    player?.updateNextEpisode(config)  // CRÍTICO
}
```

#### Problema 2: Transición Doble

**Síntomas:**
- Siguiente episodio carga dos veces
- Se salta un episodio

**Causa:**
- Callback registrado múltiples veces

**Solución:**

```kotlin
// Usar instancia única del callback
private val playerCallback by lazy { createPlayerCallback() }

fun setupPlayer() {
    player = MediastreamPlayer(...)
    player?.addPlayerCallback(playerCallback)  // Solo una vez
}

// NO hacer esto en reloadPlayer:
fun reloadPlayer() {
    player?.reloadPlayer(config)
    player?.addPlayerCallback(callback)  // ❌ Duplicado
}
```

#### Problema 3: Animación Entrecortada

**Síntomas:**
- Relleno verde se actualiza de forma irregular
- Animación no es suave

**Causa:**
- Hilo principal bloqueado
- Operaciones pesadas en callback

**Solución:**

```kotlin
override fun nextEpisodeIncoming(nextEpisodeId: String) {
    // Operaciones rápidas en hilo principal
    val config = createConfig(nextEpisodeId)
    player?.updateNextEpisode(config)
    
    // Operaciones pesadas en background
    lifecycleScope.launch(Dispatchers.IO) {
        saveToDatabase()
        fetchAdditionalData()
    }
}
```

#### Problema 4: Memory Leak

**Síntomas:**
- App consume cada vez más memoria
- Eventualmente OutOfMemoryError

**Causa:**
- Player no liberado correctamente
- Referencias fuertes en callbacks

**Solución:**

```kotlin
class MyActivity : AppCompatActivity() {
    private var player: MediastreamPlayer? = null
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Liberar player
        player?.releasePlayer()
        player = null
        
        // Limpiar referencias
        playlist.clear()
    }
}
```

## Extensiones Futuras

### Posibles Mejoras

#### 1. Preview Thumbnail

Mostrar thumbnail del siguiente episodio en el overlay:

```kotlin
// Configuración
config.nextEpisodeThumbnail = "https://cdn.example.com/thumb_ep2.jpg"

// En el overlay
<ImageView
    android:id="@+id/next_episode_thumbnail"
    android:layout_width="200dp"
    android:layout_height="113dp"
    android:scaleType="centerCrop" />
```

#### 2. Información del Siguiente

Mostrar título y descripción:

```kotlin
// Configuración
config.nextEpisodeTitle = "Episodio 2: El Descubrimiento"
config.nextEpisodeDescription = "Los protagonistas encuentran una pista crucial..."

// UI
<TextView
    android:id="@+id/next_episode_title"
    android:text="Episodio 2: El Descubrimiento"
    android:textSize="18sp"
    android:textColor="#FFFFFF" />
```

#### 3. Countdown Visible

Mostrar tiempo restante numéricamente:

```kotlin
<TextView
    android:id="@+id/countdown_text"
    android:text="5"
    android:textSize="24sp" />

// Actualizar cada segundo
ValueAnimator.ofInt(5, 0).apply {
    duration = 5000L
    addUpdateListener { 
        countdownText.text = (it.animatedValue as Int).toString()
    }
}
```

#### 4. Skip Intro Integration

Combinar con botón de "Saltar Intro":

```kotlin
if (isIntroPlaying && nextEpisodeOverlayVisible) {
    // Priorizar overlay de Next Episode
    hideSkipIntroButton()
}
```

#### 5. Continue Watching Position

Recordar posición si el usuario no termina:

```kotlin
override fun onPause() {
    super.onPause()
    
    if (!isFinishing && player?.currentPosition ?: 0 > 30000) {
        // Guardar si vio más de 30s
        saveWatchPosition(
            videoId = currentVideoId,
            position = player?.currentPosition ?: 0
        )
    }
}
```

## Resumen de Configuraciones

### Tabla de Referencia Rápida

| Parámetro | Tipo | Por Defecto | Descripción |
|-----------|------|-------------|-------------|
| `nextEpisodeId` | String? | null | ID del siguiente episodio (activa modo manual) |
| `nextEpisodeTime` | Int? | 15 | Segundos antes del final para mostrar overlay |
| `type` | VideoTypes | - | EPISODE activa modo automático si hay API |
| `loadNextAutomatically` | Boolean | auto | Se activa para EPISODE y LIVE |

### Modos de Operación

| Modo | Activación | Callback | updateNextEpisode() | Auto-transición |
|------|------------|----------|---------------------|-----------------|
| **Automático** | API `next` presente | Informativo | NO requerido | Sí |
| **Manual** | `nextEpisodeId` configurado | Requerido | REQUERIDO | Solo después de confirmación |

### Estados del Overlay

| Estado | Visible | Monitoreo | Callback | Acción |
|--------|---------|-----------|----------|--------|
| IDLE | No | No | No ejecutado | - |
| MONITORING | No | Sí | No ejecutado | Verificando posición |
| CALLBACK_EMITTED | No | Sí | Ejecutado | Esperando confirmación (manual) |
| OVERLAY_VISIBLE | Sí | Sí | Ejecutado | Countdown 5s activo |
| TRANSITIONING | No | No | Ejecutado | Cargando siguiente |
| USER_DISMISSED | No | No | Ejecutado | Usuario canceló |

## Conclusión

La funcionalidad **Next Episode** proporciona:

✅ **Experiencia de usuario fluida** para contenido serializado  
✅ **Flexibilidad** con dos modos de operación (API y Manual)  
✅ **Control granular** sobre timing y transiciones  
✅ **UI atractiva** con animaciones y countdown visual  
✅ **Soporte multiplataforma** (móvil y TV)  
✅ **Callback anticipado** para preparación de datos  
✅ **Cancelación por usuario** respetando preferencias  

### Recomendaciones Finales

1. **Para apps con series**: Usa modo automático con tipo EPISODE
2. **Para playlists custom**: Usa modo manual con nextEpisodeId
3. **Ajusta nextEpisodeTime** según duración del contenido
4. **Implementa analytics** en el callback para tracking
5. **Testea escenarios de borde**: último episodio, errores de red, etc.
6. **Optimiza el callback**: Mantener operaciones rápidas
7. **Considera UX**: No abusar de transiciones automáticas

---

**Documento preparado para el equipo de desarrollo**  
**Fecha**: Febrero 2026  
**Versión**: 1.0  
**Estado**: Implementado y documentado
