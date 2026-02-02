# Guía de Implementación: Reels (Videos Cortos Verticales)

## Resumen Ejecutivo

La funcionalidad **Reels** del SDK de Mediastream permite crear experiencias de video tipo **TikTok, Instagram Reels o YouTube Shorts**: videos cortos verticales con scroll infinito, autoplay, y una UI optimizada para navegación rápida entre contenidos.

Esta característica está diseñada para aplicaciones de contenido de video corto, redes sociales, plataformas de entretenimiento y cualquier app que necesite una experiencia de consumo rápido de contenido multimedia.

## Características Principales

✅ **Scroll vertical** tipo TikTok/Instagram Reels  
✅ **Autoplay** automático al llegar a un video  
✅ **Preload inteligente** de videos siguientes  
✅ **ViewPager2** para navegación fluida  
✅ **Pool de players** reutilizables para rendimiento óptimo  
✅ **Carga dinámica** de contenido desde API  
✅ **Soporte para anuncios** (VAST/VMAP) con auto-avance  
✅ **UI personalizable** con overlays y metadata  
✅ **Analytics integrados** con tracking de eventos  
✅ **Android TV support** con navegación DPAD  
✅ **Gestión automática** de memoria y lifecycle  
✅ **Mute/unmute global** persistente  
✅ **Tags y categorías** visuales  
✅ **Descripciones expandibles**  
✅ **Player callbacks** para eventos personalizados  

## Arquitectura del Sistema

### Componentes Principales

```
ReelsV2Handler (Orquestador principal)
    ├── ViewPager2 (Scroll vertical)
    ├── ViewPagerMediaAdapter
    │   └── ViewPagerMediaHolder (por cada reel)
    │       ├── PlayerView con ExoPlayer
    │       ├── IMA SDK (para anuncios)
    │       └── UI overlay (metadata, controles)
    ├── DynamicMediaProvider (cola de contenido)
    ├── ReelsContentManager (carga API)
    ├── PlayerPool (reutilización de players)
    ├── ReelsPreferencesManager (estado persistente)
    └── ReelsPlayerCollector (analytics)
```

### Flujo de Datos

```
API de Mediastream
    ↓
ReelsContentManager (fetch content)
    ↓
DynamicMediaProvider (cola dinámica)
    ↓
ViewPagerMediaAdapter (UI)
    ↓
ViewPagerMediaHolder (render)
    ├─> PlayerView (video)
    ├─> IMA SDK (ads)
    └─> UI Overlay (metadata)
```

## Modelo de Datos: ReelItem

Cada reel se representa con el modelo `ReelItem`:

```kotlin
data class ReelItem(
    val id: String,                         // ID único del reel
    val title: String,                      // Título del video
    val thumbnail: String,                  // URL del thumbnail
    val videoUrl: String,                   // URL del video
    val mediaItem: MediaItem,               // MediaItem de Media3
    val description: String? = null,        // Descripción (expandible)
    val date: String? = null,               // Fecha de publicación
    val tags: List<String>? = null,         // Tags/categorías
    val isAd: Boolean = false,              // Si es un anuncio
    val showAdTitle: Boolean = false,       // Mostrar título en ad
    val showAdDescription: Boolean = false, // Mostrar descripción en ad
    val config: ConfigMain? = null,         // Config del player
    val msConfig: MediastreamPlayerConfig? = null  // Config de Mediastream
)
```

## Implementación Paso a Paso

### Paso 1: Configurar el Activity/Fragment

#### Layout XML

```xml
<!-- activity_reel.xml -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/main_media_frame"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/black">
    
    <!-- El ViewPager2 se inyectará aquí dinámicamente -->
    
</FrameLayout>
```

#### Activity Kotlin

```kotlin
class ReelActivity : AppCompatActivity() {
    
    private lateinit var container: FrameLayout
    private var player: MediastreamPlayer? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reel)
        
        container = findViewById(R.id.main_media_frame)
        
        // Configuración específica para Reels
        val config = MediastreamPlayerConfig().apply {
            // Player ID de Reels (configurado en plataforma Mediastream)
            playerId = "677ee96edbb8fa932f3433cc"
            
            // ID del primer video (punto de entrada)
            id = "6772da3c808e6ac7b86edb06"
            
            // Tipo de contenido
            type = MediastreamPlayerConfig.VideoTypes.VOD
            
            // Ambiente
            environment = MediastreamPlayerConfig.Environment.DEV
            
            // Configuraciones importantes para Reels
            autoplay = true  // CRÍTICO: Auto-reproducir al llegar
            isDebug = true   // Para desarrollo
            trackEnable = true  // Habilitar analytics
            
            // UI settings
            pauseOnScreenClick = FlagStatus.DISABLE  // No pausar con tap
            showDismissButton = false  // Sin botón de cerrar
        }
        
        // Crear player (automáticamente detecta Reels y activa el modo)
        player = MediastreamPlayer(
            this,
            config,
            container,
            container,  // Puede ser el mismo container
            supportFragmentManager
        )
        
        // Opcional: Agregar callbacks
        player?.addPlayerCallback(createReelsCallback())
    }
    
    private fun createReelsCallback() = object : MediastreamPlayerCallback {
        override fun onPlay() {
            Log.d("Reels", "Video reproduciendo")
        }
        
        override fun onPause() {
            Log.d("Reels", "Video pausado")
        }
        
        override fun onEnd() {
            Log.d("Reels", "Video terminado")
            // Auto-avance manejado internamente
        }
        
        override fun onError(error: String?) {
            Log.e("Reels", "Error: $error")
            // Auto-avance al siguiente reel
        }
        
        // ... otros callbacks
    }
    
    override fun onDestroy() {
        super.onDestroy()
        player?.releasePlayer()
    }
}
```

### Paso 2: Configuración en la Plataforma Mediastream

Para que Reels funcione, necesitas configurar un **Player ID** específico en la plataforma Mediastream con:

1. **Configuración de Reels habilitada**
2. **Preload settings** (cantidad de videos a precargar)
3. **Keep in memory** (cantidad de videos en memoria)
4. **Ads configuration** (si usarás anuncios)
5. **Related content API** (endpoint para cargar más videos)

#### Estructura de Configuración (API Response)

```json
{
  "player": {
    "reels": {
      "preload": 2,           // Precargar 2 videos adelante
      "keepInMemory": 2,      // Mantener 2 en memoria
      "ads": {
        "enabled": true,
        "frequency": 3,       // Cada 3 reels
        "vastUrl": "https://..."
      },
      "relatedContentUrl": "https://api.example.com/reels/related",
      "showMetadata": true,
      "maxTags": 5,
      "autoAdvanceOnError": true
    }
  }
}
```

## Características del UI

### Layout de un Reel Individual

```
┌─────────────────────────────────────┐
│ [🔇] [👁️]                         │ ← Controles (mute, visibility)
│                                     │
│                                     │
│         VIDEO VERTICAL              │
│         (Portrait Mode)             │
│                                     │
│                                     │
│                                     │
│                                     │
│ ━━━━━━━━━━━━━━━━━ (progress)       │
│                                     │
│ 📝 Título del Reel                 │
│ 👤 @usuario • hace 2 días          │
│ 📄 Descripción expandible...       │
│ 🏷️ #tag1 #tag2 #tag3              │
└─────────────────────────────────────┘
       ↑                      ↑
  Swipe UP          Swipe DOWN
  (siguiente)       (anterior)
```

### Elementos del UI

#### 1. **Botón de Mute/Unmute**
- Ubicación: Esquina superior izquierda
- Estado persistente entre reels
- Icono cambia: 🔊 (con sonido) / 🔇 (muted)

#### 2. **Botón de Visibilidad de Metadata**
- Toggle para mostrar/ocultar información
- Estado persistente
- Útil para ver video sin distracciones

#### 3. **Indicador de Play/Pause**
- Aparece brevemente al tocar
- Feedback visual de estado
- Fade in/out animado

#### 4. **Progress Bar**
- Barra horizontal en la parte inferior
- Muestra progreso del video
- Color personalizable

#### 5. **Overlay de Metadata**
- Título del video
- Descripción expandible (tap para expandir)
- Tags con límite configurable
- Fecha de publicación
- Gradiente para legibilidad

#### 6. **Loading Indicator**
- Spinner durante carga
- Se oculta al iniciar reproducción

#### 7. **Ad Overlay** (cuando es anuncio)
- Título del anuncio
- Descripción
- "Anuncio" badge
- Cuenta regresiva opcional

## Configuración Avanzada

### Personalización del Player

```kotlin
val config = MediastreamPlayerConfig().apply {
    // === REELS ESPECÍFICOS ===
    playerId = "your_reels_player_id"
    id = "initial_video_id"
    type = VideoTypes.VOD
    
    // === COMPORTAMIENTO ===
    autoplay = true              // CRÍTICO para reels
    loop = false                 // No loop individual
    showControls = false         // Controles custom en overlay
    
    // === UI ===
    pauseOnScreenClick = FlagStatus.DISABLE  // Tap = play/pause custom
    showFullScreenButton = false  // No fullscreen en reels
    showTitle = FlagStatus.DISABLE  // Título en overlay custom
    initialHideController = true   // No mostrar controles de ExoPlayer
    
    // === ANUNCIOS ===
    adURL = "https://vastserver.com/tag"  // VAST/VMAP URL
    muteAds = FlagStatus.NONE  // Respetar mute global
    
    // === ANALYTICS ===
    trackEnable = true
    appName = "MyReelsApp"
    customerID = "user_id_here"
    
    // === CALIDAD ===
    isMaxResolutionBasedOnScreenSize = true
    isForceHighestSupportedBitrateEnabled = false  // Ahorrar datos
    
    // === DEBUGGING ===
    isDebug = BuildConfig.DEBUG
}
```

### Configuración del ReelsContentManager

El `ReelsContentManager` maneja la carga dinámica de contenido:

```kotlin
// Configurado automáticamente por ReelsV2Handler, pero puedes personalizar:
val contentManager = ReelsContentManager(
    context = context,
    playerId = "player_id",
    baseUrl = "https://mdstrm.com",  // Base URL de Mediastream
    mediaProvider = dynamicMediaProvider,
    adsConfig = adsConfig,
    msConfig = config,
    preloadDistance = 2  // Cargar más contenido cuando quedan 2 videos
)
```

#### Endpoint de Related Content

El SDK llama automáticamente al endpoint:
```
GET {baseUrl}/embed/reels/{playerId}/related?videoId={currentVideoId}
```

**Respuesta esperada:**

```json
{
  "videos": [
    {
      "id": "video_id_1",
      "title": "Título del Video",
      "thumbnail": "https://cdn.example.com/thumb.jpg",
      "description": "Descripción del video",
      "date": "2026-02-01T12:00:00Z",
      "tags": ["deportes", "futbol", "goles"],
      "url": "https://mdstrm.com/video/video_id_1"
    },
    {
      "id": "video_id_2",
      // ...
    }
  ]
}
```

## Funcionalidades Avanzadas

### 1. Pool de Players

Para optimizar rendimiento, Reels usa un **pool de players reutilizables**:

```kotlin
// Configuración interna (no requiere intervención)
val playerPool = PlayerPool(
    context = context,
    numberOfPlayers = 6  // 6 instancias de ExoPlayer
)

// Cuando se necesita un player:
val player = playerPool.acquire()

// Cuando ya no se usa:
playerPool.release(player)
```

**Ventajas:**
- No crear/destruir players constantemente
- Reducción de stuttering
- Mejor uso de memoria
- Transiciones más suaves

### 2. Preload Inteligente

Los videos se precargan basándose en la posición actual:

```
Posición actual: 5
Preload distance: 2

Videos en memoria:
├── Posición 3 (anterior)
├── Posición 4 (anterior)
├── Posición 5 (ACTUAL) ← Usuario aquí
├── Posición 6 (siguiente) ← Precargando
└── Posición 7 (siguiente) ← Precargando

Cuando el usuario llega a posición 7:
→ Se carga más contenido desde API
→ Se liberan posiciones 3-4
```

**Configuración:**

```kotlin
// En la configuración de reels del player
{
  "reels": {
    "preload": 2,        // Distancia de precarga
    "keepInMemory": 2    // Videos a mantener en memoria
  }
}
```

### 3. Anuncios en Reels

Los anuncios se insertan automáticamente en el feed:

#### Configuración

```kotlin
// En la respuesta de configuración:
{
  "reels": {
    "ads": {
      "enabled": true,
      "frequency": 3,  // Cada 3 reels orgánicos
      "vastUrl": "https://vastserver.com/tag?ppid={ppid}",
      "showTitle": true,
      "showDescription": true
    }
  }
}
```

#### Flujo de Anuncios

```
Reel 1 (orgánico)
    ↓
Reel 2 (orgánico)
    ↓
Reel 3 (orgánico)
    ↓
[ANUNCIO]  ← Insertado automáticamente
    ↓
Reel 4 (orgánico)
    ↓
Reel 5 (orgánico)
    ↓
Reel 6 (orgánico)
    ↓
[ANUNCIO]
    ...
```

#### Auto-avance después de Anuncio

```kotlin
// Configurado automáticamente en ReelsV2Handler
adapter?.onAdComplete = { position ->
    viewPager.postDelayed({
        val nextPosition = position + 1
        if (nextPosition < mediaProvider.getItemCount()) {
            viewPager.setCurrentItem(nextPosition, true)
        }
    }, 300)  // Delay de 300ms para transición suave
}
```

### 4. Gestión de Estado con Preferences

El estado del usuario se guarda automáticamente:

```kotlin
class ReelsPreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("reels_prefs", Context.MODE_PRIVATE)
    
    // Estado de mute (global)
    var isMuted: Boolean
        get() = prefs.getBoolean("is_muted", false)
        set(value) = prefs.edit().putBoolean("is_muted", value).apply()
    
    // Visibilidad de metadata
    var isMetadataVisible: Boolean
        get() = prefs.getBoolean("metadata_visible", true)
        set(value) = prefs.edit().putBoolean("metadata_visible", value).apply()
    
    // Última posición (para continuar)
    var lastPosition: Int
        get() = prefs.getInt("last_position", 0)
        set(value) = prefs.edit().putInt("last_position", value).apply()
}
```

**Usos:**
- Recordar estado de mute entre sesiones
- Continuar desde donde se quedó el usuario
- Preferencias de visualización

### 5. Android TV Support

Reels incluye soporte completo para Android TV con navegación DPAD:

```kotlin
// Detección automática en ReelsV2Handler
if (isAndroidTV(context)) {
    // Habilitar controles DPAD
    viewPager.isFocusable = true
    viewPager.requestFocus()
}

// Mapeo de teclas:
// DPAD_UP    → Reel anterior
// DPAD_DOWN  → Reel siguiente
// DPAD_CENTER → Play/Pause
// DPAD_LEFT  → Seek backward (en video)
// DPAD_RIGHT → Seek forward (en video)
```

## Analytics y Tracking

### Eventos Automáticos

El sistema envía automáticamente eventos de analytics:

```kotlin
// ReelsPlayerCollector maneja tracking
class ReelsPlayerCollector(
    private val msConfig: MediastreamPlayerConfig?,
    private val callbacks: List<MediastreamPlayerCallback?>?
) {
    fun trackEvent(event: String, data: Map<String, Any>) {
        // Enviar a plataforma de analytics
        
        // Notificar callbacks
        callbacks?.forEach { callback ->
            callback?.onReelEvent(event, data)
        }
    }
}
```

**Eventos rastreados:**

| Evento | Descripción | Datos |
|--------|-------------|-------|
| `reel_view` | Reel visible en pantalla | `reel_id`, `position` |
| `reel_play` | Reproducción iniciada | `reel_id`, `duration` |
| `reel_pause` | Reproducción pausada | `reel_id`, `current_time` |
| `reel_complete` | Video completado al 95% | `reel_id`, `duration` |
| `reel_swipe` | Usuario hizo swipe | `direction`, `from_position`, `to_position` |
| `reel_error` | Error de reproducción | `reel_id`, `error_code`, `error_message` |
| `ad_impression` | Anuncio mostrado | `ad_id`, `position` |
| `ad_click` | Click en anuncio | `ad_id`, `click_url` |
| `ad_complete` | Anuncio completado | `ad_id`, `duration` |

### Callback Personalizado (Opcional)

```kotlin
interface MediastreamPlayerCallback {
    // ... callbacks existentes ...
    
    // Nuevo callback para eventos de Reels
    fun onReelEvent(event: String, data: Map<String, Any>) {
        when (event) {
            "reel_view" -> {
                val reelId = data["reel_id"] as String
                val position = data["position"] as Int
                // Tu lógica de analytics
                analyticsService.trackView(reelId, position)
            }
            "reel_complete" -> {
                val reelId = data["reel_id"] as String
                // Tracking de completitud
                analyticsService.trackCompletion(reelId)
            }
            // ... otros eventos
        }
    }
}
```

## Gestión del Ciclo de Vida

### Lifecycle Observer

El `ReelsV2Handler` implementa `DefaultLifecycleObserver`:

```kotlin
class ReelsV2Handler(...) : DefaultLifecycleObserver {
    
    override fun onPause(owner: LifecycleOwner) {
        // Pausar player actual
        adapter?.pauseCurrentPlayer()
    }
    
    override fun onResume(owner: LifecycleOwner) {
        // Reanudar player si estaba reproduciendo
        adapter?.resumeCurrentPlayerIfNeeded()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        // Liberar recursos
        adapter?.releaseAll()
        playerPool?.release()
        contentManager?.cleanup()
    }
}
```

**Registro automático:**

```kotlin
// En ReelsV2Handler.activate()
if (context is LifecycleOwner) {
    lifecycleOwner = context
    context.lifecycle.addObserver(this)
}
```

### Gestión de Memoria

```kotlin
// Limpieza automática de videos fuera de rango
fun cleanupOutOfRangeVideos(currentPosition: Int, keepInMemory: Int) {
    val startRange = (currentPosition - keepInMemory).coerceAtLeast(0)
    val endRange = currentPosition + keepInMemory
    
    // Liberar players fuera de rango
    holders.forEach { (position, holder) ->
        if (position < startRange || position > endRange) {
            holder.releasePlayer()
            holders.remove(position)
        }
    }
}
```

## Personalización del UI

### Custom Controller Layout

```xml
<!-- reelsv2_custom_controller.xml -->
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
    
    <!-- Progress bar -->
    <ProgressBar
        android:id="@+id/exo_progress"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="4dp"
        android:layout_gravity="bottom"
        android:layout_marginBottom="80dp"
        android:progressDrawable="@drawable/reel_progress_drawable" />
    
    <!-- Metadata overlay con gradiente -->
    <LinearLayout
        android:id="@+id/metadata_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="@drawable/reels_overlay_gradient">
        
        <!-- Título -->
        <TextView
            android:id="@+id/reel_title"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="@color/white"
            android:textSize="16sp"
            android:textStyle="bold"
            android:maxLines="2"
            android:ellipsize="end" />
        
        <!-- Usuario y fecha -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="4dp">
            
            <TextView
                android:id="@+id/reel_username"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/white"
                android:textSize="14sp"
                android:alpha="0.8" />
            
            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text=" • "
                android:textColor="@color/white"
                android:alpha="0.8" />
            
            <TextView
                android:id="@+id/reel_date"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:textColor="@color/white"
                android:textSize="14sp"
                android:alpha="0.8" />
        </LinearLayout>
        
        <!-- Descripción expandible -->
        <TextView
            android:id="@+id/reel_description"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:textColor="@color/white"
            android:textSize="14sp"
            android:maxLines="2"
            android:ellipsize="end" />
        
        <!-- Tags -->
        <com.google.android.flexbox.FlexboxLayout
            android:id="@+id/tags_container"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            app:flexWrap="wrap"
            app:justifyContent="flex_start" />
            
    </LinearLayout>
</FrameLayout>
```

### Estilos Personalizados

```xml
<!-- res/values/reels_styles.xml -->
<resources>
    <!-- Colores -->
    <color name="reels_background">#000000</color>
    <color name="reels_overlay_gradient_start">#00000000</color>
    <color name="reels_overlay_gradient_end">#CC000000</color>
    <color name="reels_accent">#FF5722</color>
    
    <!-- Dimensiones -->
    <dimen name="reels_control_button_size">48dp</dimen>
    <dimen name="reels_control_icon_size">24dp</dimen>
    <dimen name="reels_mute_button_margin">16dp</dimen>
    <dimen name="reels_tag_padding">8dp</dimen>
    <dimen name="reels_tag_margin">4dp</dimen>
</resources>
```

## Casos de Uso

### Caso 1: App de Entretenimiento

**Escenario:** App tipo TikTok con videos cortos de usuarios.

```kotlin
class HomeReelsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_reels)
        
        val config = MediastreamPlayerConfig().apply {
            // Player configurado para "For You" feed
            playerId = "for_you_player_id"
            id = getRecommendedVideoId()  // Primer video basado en algoritmo
            type = VideoTypes.VOD
            autoplay = true
            trackEnable = true
            customerID = getCurrentUserId()
            
            // Analytics personalizados
            analyticsCustom = buildJsonString {
                put("user_id", getCurrentUserId())
                put("feed_type", "for_you")
                put("session_id", getSessionId())
            }
        }
        
        player = MediastreamPlayer(this, config, container, container, supportFragmentManager)
        player?.addPlayerCallback(createAnalyticsCallback())
    }
    
    private fun createAnalyticsCallback() = object : MediastreamPlayerCallback {
        override fun onReelEvent(event: String, data: Map<String, Any>) {
            when (event) {
                "reel_complete" -> {
                    // Mejorar algoritmo de recomendaciones
                    recommendationEngine.trackCompletion(
                        userId = getCurrentUserId(),
                        reelId = data["reel_id"] as String
                    )
                }
                "reel_swipe" -> {
                    // Trackear engagement
                    val direction = data["direction"] as String
                    if (direction == "up") {
                        engagementTracker.trackSkip()
                    }
                }
            }
        }
        
        // ... otros callbacks
    }
}
```

### Caso 2: Noticias en Video Corto

**Escenario:** App de noticias con resúmenes en video vertical.

```kotlin
class NewsReelsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val category = intent.getStringExtra("category") ?: "general"
        
        val config = MediastreamPlayerConfig().apply {
            playerId = "news_reels_player_id"
            id = getLatestNewsVideo(category)
            type = VideoTypes.VOD
            autoplay = true
            
            // Configurar metadata visible por defecto
            // (importante para noticias)
        }
        
        player = MediastreamPlayer(this, config, container, container, supportFragmentManager)
        
        // Callbacks para noticias
        player?.addPlayerCallback(object : MediastreamPlayerCallback {
            override fun onReelEvent(event: String, data: Map<String, Any>) {
                if (event == "reel_view") {
                    // Tracking de noticias vistas
                    newsAnalytics.trackNewsView(
                        articleId = data["reel_id"] as String,
                        category = category
                    )
                }
            }
            
            override fun onEnd() {
                // Preguntar si quiere leer artículo completo
                showReadMoreDialog()
            }
            
            // ... otros callbacks
        })
    }
    
    private fun showReadMoreDialog() {
        AlertDialog.Builder(this)
            .setTitle("Leer más")
            .setMessage("¿Quieres leer el artículo completo?")
            .setPositiveButton("Sí") { _, _ ->
                openFullArticle()
            }
            .setNegativeButton("Continuar viendo") { _, _ ->
                // Continuar con siguiente reel
            }
            .show()
    }
}
```

### Caso 3: E-commerce con Product Reels

**Escenario:** Videos cortos de productos para compra rápida.

```kotlin
class ProductReelsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val config = MediastreamPlayerConfig().apply {
            playerId = "product_showcase_player_id"
            id = getFeaturedProductVideo()
            type = VideoTypes.VOD
            autoplay = true
            
            // Personalizar overlay con botón CTA
            // (manejado en custom controller)
        }
        
        player = MediastreamPlayer(this, config, container, container, supportFragmentManager)
        
        // Agregar botón "Comprar Ahora" al overlay
        setupBuyButton()
        
        player?.addPlayerCallback(object : MediastreamPlayerCallback {
            override fun onReelEvent(event: String, data: Map<String, Any>) {
                if (event == "reel_view") {
                    val productId = extractProductId(data["reel_id"] as String)
                    
                    // Tracking de producto visto
                    ecommerceAnalytics.trackProductView(productId)
                    
                    // Actualizar botón con info de producto
                    updateBuyButton(productId)
                }
            }
            
            // ... otros callbacks
        })
    }
    
    private fun setupBuyButton() {
        val buyButton = findViewById<Button>(R.id.buy_now_button)
        buyButton.setOnClickListener {
            val currentProductId = getCurrentProductId()
            addToCart(currentProductId)
            showAddedToCartAnimation()
        }
    }
}
```

## Mejores Prácticas

### 1. Optimización de Performance

**✅ Hacer:**

```kotlin
// Usar pool de players (automático)
val config = MediastreamPlayerConfig().apply {
    // Configuración óptima
    isMaxResolutionBasedOnScreenSize = true  // Adaptar calidad a pantalla
    isForceHighestSupportedBitrateEnabled = false  // No forzar máxima calidad
}

// Preload inteligente
{
  "reels": {
    "preload": 1,        // Solo 1 video adelante en conexiones lentas
    "keepInMemory": 1    // Solo 1 en memoria para dispositivos con RAM limitada
  }
}
```

**❌ Evitar:**

```kotlin
// NO crear múltiples instancias de MediastreamPlayer
// NO precargar demasiados videos en dispositivos low-end
// NO forzar máxima resolución en todas las condiciones
```

### 2. Gestión de Memoria

```kotlin
// Implementar detection de memoria baja
override fun onLowMemory() {
    super.onLowMemory()
    
    // Reducir keepInMemory dinámicamente
    adapter?.reduceMemoryFootprint()
}

// Limpiar recursos al salir
override fun onDestroy() {
    super.onDestroy()
    player?.releasePlayer()
    
    // Liberar cache si es necesario
    clearReelsCache()
}
```

### 3. Manejo de Errores

```kotlin
// Configurar auto-avance en errores
adapter?.onError = { position ->
    Log.e(TAG, "Error en reel position $position")
    
    // Trackear error
    analyticsService.trackError(position)
    
    // Auto-avanzar al siguiente
    viewPager.postDelayed({
        val next = position + 1
        if (next < mediaProvider.getItemCount()) {
            viewPager.setCurrentItem(next, true)
        }
    }, 300)
}
```

### 4. Experiencia de Usuario

```kotlin
// Mantener contexto de navegación
fun navigateToReels(fromScreen: String, initialVideoId: String) {
    val config = MediastreamPlayerConfig().apply {
        id = initialVideoId
        
        // Analytics contextuales
        analyticsCustom = buildJsonString {
            put("source_screen", fromScreen)
            put("entry_point", "tap_video_thumbnail")
        }
    }
    
    startActivity(Intent(this, ReelsActivity::class.java).apply {
        putExtra("config", config)
    })
}

// Permitir salir fácilmente
override fun onBackPressed() {
    // Mostrar confirmación si ha visto varios reels
    if (viewedReelsCount > 5) {
        showExitConfirmation()
    } else {
        super.onBackPressed()
    }
}
```

## Testing y Validación

### Test Cases Críticos

#### 1. Scroll Vertical Básico

**Pasos:**
1. Abrir reels
2. Hacer swipe up (siguiente)
3. Verificar transición suave
4. Verificar autoplay

**Resultado esperado:**
✅ Transición animada
✅ Video siguiente se reproduce automáticamente
✅ Video anterior se pausa

#### 2. Preload de Contenido

**Pasos:**
1. Abrir reels
2. Esperar a que cargue posición 0
3. Verificar que posición 1 y 2 se precargan
4. Monitorear logs de carga

**Resultado esperado:**
✅ Videos siguientes precargan en background
✅ No lag al llegar a siguiente reel

#### 3. Anuncios

**Pasos:**
1. Configurar ads con frequency=3
2. Ver 3 reels orgánicos
3. Verificar anuncio en posición 4
4. Esperar fin de anuncio

**Resultado esperado:**
✅ Anuncio se muestra en frecuencia correcta
✅ Auto-avance al terminar anuncio
✅ Overlay de "Anuncio" visible

#### 4. Estado de Mute

**Pasos:**
1. Iniciar con audio activado
2. Tocar botón mute
3. Navegar a siguiente reel
4. Verificar audio

**Resultado esperado:**
✅ Estado muted persiste entre reels
✅ Icono actualizado correctamente
✅ Estado se guarda en SharedPreferences

#### 5. Manejo de Errores

**Pasos:**
1. Forzar error de red
2. Verificar comportamiento
3. Verificar auto-avance

**Resultado esperado:**
✅ Error no bloquea UI
✅ Auto-avanza al siguiente reel
✅ Mensaje de error loggeado

## Debugging

### Logs Importantes

```kotlin
// Activar logs detallados
config.isDebug = true

// Logs generados automáticamente:
[ReelsV2Handler] activate: playerId=xxx mediaId=xxx
[ReelsV2Handler] MediaProvider initialized with 1 items
[ReelsContentManager] Fetching related content for videoId=xxx
[ReelsContentManager] Added 10 new items to provider
[ViewPagerMediaHolder] onBind: position=0 reelId=xxx
[ViewPagerMediaHolder] Starting playback for position=0
[ViewPagerMediaHolder] onPageSelected: position=1
[ViewPagerMediaAdapter] Preloading position=2
[ViewPagerMediaHolder] Ad started at position=3
[ViewPagerMediaHolder] Ad completed at position=3
[ReelsV2Handler] Auto-swiping to position=4 after ad
```

### Comandos ADB

```bash
# Logs de Reels
adb logcat | grep -i "reels\|viewpager"

# Ver memoria
adb shell dumpsys meminfo <package>

# Forzar rotación (testear portrait lock)
adb shell settings put system user_rotation 1

# Simular memoria baja
adb shell am send-trim-memory <package> RUNNING_CRITICAL

# Ver actividades
adb shell dumpsys activity | grep "ReelsActivity"
```

## Troubleshooting

### Problema 1: Reels No Se Activa

**Síntomas:**
- Se muestra player normal en lugar de reels
- No aparece ViewPager2

**Causas:**
1. `playerId` no configurado o inválido
2. Configuración de reels no presente en API
3. Player type incorrecto

**Solución:**

```kotlin
// Verificar configuración
val config = MediastreamPlayerConfig().apply {
    playerId = "PLAYER_ID_VALIDO"  // CRÍTICO
    id = "initial_video_id"
    type = VideoTypes.VOD  // Debe ser VOD
}

// Verificar en logs:
// [ReelsV2Handler] activate: playerId=xxx
// Si no aparece este log, reels no se activó
```

### Problema 2: Videos No Precargan

**Síntomas:**
- Lag al cambiar de reel
- Cada video empieza desde cero

**Causa:**
- Preload distance muy bajo
- Problemas de red
- Configuración incorrecta

**Solución:**

```kotlin
// Aumentar preload
{
  "reels": {
    "preload": 2,  // Aumentar a 2-3
    "keepInMemory": 2
  }
}

// Verificar en logs:
// [ViewPagerMediaAdapter] Preloading position=X
```

### Problema 3: Crash por Memoria

**Síntomas:**
- App crashea después de varios reels
- OutOfMemoryError en logs

**Causa:**
- keepInMemory muy alto
- No se liberan recursos

**Solución:**

```kotlin
// Reducir keepInMemory
{
  "reels": {
    "preload": 1,
    "keepInMemory": 1  // Reducir en dispositivos low-end
  }
}

// Implementar onLowMemory
override fun onLowMemory() {
    super.onLowMemory()
    adapter?.releaseUnusedPlayers()
}
```

### Problema 4: Anuncios No Aparecen

**Síntomas:**
- Solo reels orgánicos, sin anuncios

**Causa:**
- Ads config missing
- VAST URL inválida
- Frequency incorrecta

**Solución:**

```kotlin
// Verificar configuración de ads
{
  "reels": {
    "ads": {
      "enabled": true,
      "frequency": 3,
      "vastUrl": "https://valid-vast-url.com/tag"
    }
  }
}

// Verificar en logs:
// [ViewPagerMediaHolder] Ad scheduled at position=X
// [ImaAdsLoader] Ad loaded successfully
```

## Comparación con Implementación Normal

| Característica | Player Normal | Reels |
|----------------|---------------|-------|
| **Orientación** | Landscape/Portrait | Portrait forzado |
| **Navegación** | Playlist tradicional | Scroll vertical infinito |
| **Preload** | Siguiente video | Múltiples videos |
| **UI** | Controles ExoPlayer | Overlay personalizado |
| **Anuncios** | Pre-roll/mid-roll | Insertados en feed |
| **Analytics** | Eventos estándar | Eventos de Reels específicos |
| **Pool de players** | Un player | Múltiples players reutilizables |
| **Memoria** | Player único | Gestión inteligente de memoria |

## Conclusión

La funcionalidad **Reels** del SDK de Mediastream proporciona:

✅ **Experiencia moderna** tipo TikTok/Instagram Reels  
✅ **Performance optimizado** con pool de players y preload inteligente  
✅ **Monetización** con anuncios integrados  
✅ **Analytics completos** para entender engagement  
✅ **Personalización total** del UI y comportamiento  
✅ **Gestión automática** de lifecycle y memoria  
✅ **Soporte multiplataforma** (móvil y TV)  

Esta implementación está diseñada para aplicaciones que necesitan experiencias de video corto de nivel profesional, con todas las optimizaciones necesarias para proporcionar scroll fluido y reproducción sin interrupciones.

---

**Documento preparado para el equipo de desarrollo**  
**Fecha**: Febrero 2026  
**Versión**: 1.0 (Feature Branch)  
**Estado**: En desarrollo activo
