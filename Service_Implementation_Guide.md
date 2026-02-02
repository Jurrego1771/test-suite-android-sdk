# Guía de Implementación: Servicios de Reproducción en Background

## Resumen Ejecutivo

Los **servicios de reproducción** permiten que el contenido de audio (y video en segundo plano) continúe reproduciéndose incluso cuando la app no está visible, similar a Spotify, YouTube Music o cualquier app de audio profesional. El SDK de Mediastream proporciona dos servicios especializados:

1. **MediastreamPlayerService**: Servicio básico con notificaciones y controles multimedia
2. **MediastreamPlayerServiceWithSync**: Servicio avanzado con MediaSession, Android Auto y sincronización completa

Estos servicios son esenciales para apps de **radio, podcasts, música, audiolibros** y cualquier contenido que el usuario desee escuchar mientras usa otras aplicaciones.

## ¿Por Qué Usar Servicios?

### Sin Servicio (Activity Normal)

```
Usuario escucha audio
    ↓
Presiona HOME o cambia de app
    ↓
Activity pasa a background
    ↓
Sistema destruye Activity (después de tiempo/memoria)
    ↓
❌ Audio se detiene
```

### Con Servicio

```
Usuario escucha audio
    ↓
Presiona HOME o cambia de app
    ↓
Activity pasa a background
    ↓
Servicio continúa en foreground
    ↓
✅ Audio sigue reproduciéndose
✅ Notificación con controles
✅ Integración con sistema (lock screen, bluetooth)
```

## Características Principales

### Servicio Básico (MediastreamPlayerService)

✅ **Reproducción en background** continua  
✅ **Notificación multimedia** con controles (play/pause/next/prev)  
✅ **MediaSession** para integración con sistema  
✅ **Artwork dinámico** en la notificación  
✅ **Control desde lock screen**  
✅ **Control desde auriculares Bluetooth**  
✅ **Actualización dinámica** de metadatos (título, artista, imagen)  
✅ **Next Episode** compatible  

### Servicio Avanzado (MediastreamPlayerServiceWithSync)

✅ **Todo lo del servicio básico**  
✅ **MediaLibraryService** para apps de audio profesionales  
✅ **Android Auto** soporte completo  
✅ **Android TV/Google TV** integración  
✅ **Sincronización de estado** entre UI y servicio  
✅ **Navegación por contenido** (browse/search)  
✅ **Comandos personalizados** del sistema  
✅ **EventBus** para comunicación bidireccional  

## Arquitectura de los Servicios

### Componente 1: MediastreamPlayerService (Básico)

```
Activity
    ↓
initializeService() (configuración)
    ↓
startForegroundService()
    ↓
MediastreamPlayerService
    ├── onCreate()
    │   ├── MediastreamPlayer (interno)
    │   ├── MediaSession
    │   └── PlayerNotificationManager
    ├── Notificación en foreground
    │   ├── Artwork (Glide)
    │   ├── Título/Descripción
    │   └── Controles (play/pause/next/prev)
    └── Callbacks a la Activity
```

### Componente 2: MediastreamPlayerServiceWithSync (Avanzado)

```
Activity
    ↓
initializeService() + MediaController.Builder
    ↓
MediastreamPlayerServiceWithSync (MediaLibraryService)
    ├── MediaLibrarySession
    │   ├── Callback personalizado
    │   ├── Comandos personalizados
    │   └── Android Auto integration
    ├── MediastreamPlayer (interno)
    ├── PlayerNotificationManager
    ├── EventBus (comunicación)
    └── Android Auto Library Browser
        ├── Live streams
        ├── Podcasts
        └── Episodes
```

## Implementación: Servicio Básico

### Paso 1: Declarar el Servicio en AndroidManifest.xml

```xml
<manifest>
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    
    <application>
        <!-- Servicio básico -->
        <service 
            android:name="am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerService"
            android:foregroundServiceType="mediaPlayback" />
    </application>
</manifest>
```

**Permisos requeridos:**
- `FOREGROUND_SERVICE`: Permite servicios en primer plano
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Específico para reproducción de medios (Android 10+)
- `WAKE_LOCK`: Mantiene el dispositivo activo durante reproducción

### Paso 2: Configurar en la Activity

#### 2.1 Layout XML

```xml
<!-- activity_audioplayer.xml -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">
    
    <!-- Contenedor principal -->
    <FrameLayout
        android:id="@+id/main_media_frame"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
    
    <!-- Contenedor del player (puede estar oculto) -->
    <FrameLayout
        android:id="@+id/playerContainer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:visibility="gone" />
</LinearLayout>
```

#### 2.2 Código de la Activity

```kotlin
class LiveAudioAsServiceActivity : AppCompatActivity() {
    
    private val TAG = "AudioService"
    private lateinit var container: FrameLayout
    private lateinit var playerContainer: FrameLayout
    private lateinit var miniPlayerConfig: MediastreamMiniPlayerConfig
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audioplayer)
        
        // 1. Referencias a los contenedores
        container = findViewById(R.id.main_media_frame)
        playerContainer = findViewById(R.id.playerContainer)
        
        // 2. Configurar el player
        val config = MediastreamPlayerConfig().apply {
            id = "632c9b89aa9ace684913b815"
            accountID = "6271a4d5d206c3172f3c9a9c"
            type = MediastreamPlayerConfig.VideoTypes.LIVE
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
            environment = MediastreamPlayerConfig.Environment.PRODUCTION
            isDebug = true
            trackEnable = false
            showControls = true
            appName = "MyAudioApp"
            
            // Configuración de notificación
            notificationSongName = "Radio en Vivo"
            notificationDescription = "Escuchando ahora"
            notificationImageUrl = "https://example.com/logo.png"
            notificationHasNext = true
            notificationHasPrevious = false
        }
        
        // 3. Iniciar el servicio
        startService(config)
    }
    
    private fun startService(config: MediastreamPlayerConfig) {
        // Configuración del mini player (notificación)
        miniPlayerConfig = MediastreamMiniPlayerConfig().apply {
            songName = "Nombre de la canción"
            description = "Artista o descripción"
            imageUrl = "https://example.com/cover.jpg"
            albumName = "Nombre del álbum"
            color = android.graphics.Color.parseColor("#1DB954") // Verde tipo Spotify
            setStateNext = true  // Habilitar botón Next
            setStatePrev = false // Deshabilitar botón Previous
        }
        
        // Callback para eventos del player
        val playerCallback = object : MediastreamPlayerCallback {
            override fun playerViewReady(msplayerView: PlayerView?) {
                Log.d(TAG, "Player listo")
            }
            
            override fun onPlay() {
                Log.d(TAG, "Reproduciendo")
            }
            
            override fun onPause() {
                Log.d(TAG, "Pausado")
            }
            
            override fun onReady() {
                Log.d(TAG, "Contenido listo")
            }
            
            override fun onEnd() {
                Log.d(TAG, "Reproducción finalizada")
            }
            
            override fun onPlayerClosed() {
                Log.d(TAG, "Player cerrado")
                finish()
            }
            
            override fun onError(error: String?) {
                Log.e(TAG, "Error: $error")
                Toast.makeText(this@LiveAudioAsServiceActivity, error, Toast.LENGTH_LONG).show()
            }
            
            override fun onNext() {
                Log.d(TAG, "Next presionado")
                // Implementar lógica para siguiente canción
            }
            
            override fun onPrevious() {
                Log.d(TAG, "Previous presionado")
                // Implementar lógica para canción anterior
            }
            
            override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {
                // Actualizar notificación con nueva canción
                data?.let {
                    val songName = it.optString("song", "")
                    val artist = it.optString("artist", "")
                    val poster = it.optString("poster", "")
                    
                    Log.d(TAG, "Nueva canción: $songName - $artist")
                    
                    // Actualizar mini player
                    miniPlayerConfig.songName = songName
                    miniPlayerConfig.description = artist
                    miniPlayerConfig.imageUrl = poster
                }
            }
            
            // ... otros callbacks
        }
        
        // Inicializar el servicio con la configuración
        MediastreamPlayerService.initializeService(
            context = this,
            activity = this,
            config = config,
            container = container,
            playerContainer = playerContainer,
            miniPlayerConfig = miniPlayerConfig,
            trackEnable = false,
            accountId = config.accountID ?: "",
            playerCallback = playerCallback
        )
        
        // Iniciar servicio en foreground
        val intent = Intent(this, MediastreamPlayerService::class.java)
        intent.action = "$packageName.action.startforeground"
        
        try {
            ContextCompat.startForegroundService(this, intent)
            Log.d(TAG, "Servicio iniciado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar servicio: ${e.message}")
        }
    }
    
    override fun onBackPressed() {
        // Detener el servicio al salir
        try {
            val stopIntent = Intent(this, MediastreamPlayerService::class.java)
            stopService(stopIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener servicio: ${e.message}")
        }
        super.onBackPressed()
    }
}
```

### Paso 3: Personalización de la Notificación

#### Configuración de Colores y Estilos

```kotlin
miniPlayerConfig.apply {
    // Metadatos
    songName = "Título de la Canción"
    description = "Nombre del Artista"
    albumName = "Nombre del Álbum"
    imageUrl = "https://cdn.example.com/album-cover.jpg"
    
    // Colores (opcional)
    color = Color.parseColor("#FF5722") // Color de acento
    imageIconUrl = R.drawable.custom_notification_icon // Icono personalizado
    
    // Botones de navegación
    setStateNext = true  // Mostrar botón "Siguiente"
    setStatePrev = true  // Mostrar botón "Anterior"
}

// Configuración adicional en MediastreamPlayerConfig
config.apply {
    notificationSongName = "Override del título"
    notificationDescription = "Override de la descripción"
    notificationImageUrl = "Override de la imagen"
    notificationColor = Color.parseColor("#FF5722")
    notificationHasNext = true
    notificationHasPrevious = true
    notificationIconUrl = R.drawable.ic_notification
    
    // Actualización automática para audio en vivo
    fillAutomaticallyAudioNotification = true
}
```

#### Actualización Dinámica de la Notificación

```kotlin
// Método 1: Callback onLiveAudioCurrentSongChanged
override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {
    data?.let {
        miniPlayerConfig.songName = it.optString("song", "")
        miniPlayerConfig.description = it.optString("artist", "")
        miniPlayerConfig.imageUrl = it.optString("poster", "")
        
        // El servicio actualizará automáticamente la notificación
    }
}

// Método 2: Actualización manual
val service = MediastreamPlayerService.getMsPlayer()
service?.let {
    // Actualizar configuración
    val newMiniConfig = MediastreamMiniPlayerConfig().apply {
        songName = "Nueva Canción"
        description = "Nuevo Artista"
        imageUrl = "nueva-url.jpg"
    }
    
    // Forzar actualización
    // (requiere acceso al servicio)
}
```

### Paso 4: Interacción con el Servicio

#### Obtener Instancia del Player

```kotlin
// Desde cualquier parte de la app
val player = MediastreamPlayerService.getMsPlayer()

player?.let {
    // Controlar reproducción
    it.play()
    it.pause()
    it.seekTo(30000) // Seek a 30 segundos
    
    // Obtener información
    val isPlaying = it.isPlaying()
    val currentPosition = it.getCurrentPosition()
    val duration = it.getDuration()
    
    // Cambiar velocidad
    it.changeSpeed(1.5f) // 1.5x
    
    // Cargar nuevo contenido
    val newConfig = MediastreamPlayerConfig().apply {
        id = "nuevo_audio_id"
        type = MediastreamPlayerConfig.VideoTypes.VOD
    }
    it.reloadPlayer(newConfig)
}
```

#### Control de Volumen

```kotlin
// El servicio respeta el volumen del sistema
// Los controles de volumen del dispositivo afectarán el audio
```

## Implementación: Servicio Avanzado (WithSync)

### Paso 1: Declarar el Servicio en AndroidManifest.xml

```xml
<manifest>
    <!-- Permisos -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <application>
        <!-- Servicio con sincronización -->
        <service
            android:name="am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerServiceWithSync"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
                <action android:name="androidx.media3.session.MediaLibraryService" />
                <action android:name="android.media.browse.MediaBrowserService" />
                <action android:name="android.intent.action.MEDIA_BUTTON" />
                <action android:name="android.media.action.MEDIA_PLAY_FROM_SEARCH" />
            </intent-filter>
        </service>
        
        <!-- Metadata para Android Auto (opcional) -->
        <meta-data 
            android:name="com.google.android.gms.car.application"
            android:resource="@xml/automotive_app_desc" />
    </application>
</manifest>
```

### Paso 2: Solicitar Permisos (Android 13+)

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Solicitar permiso de notificaciones en Android 13+
    if (Build.VERSION.SDK_INT >= 33 &&
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != 
        PackageManager.PERMISSION_GRANTED
    ) {
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATIONS
        )
    }
}

override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    
    if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
        if (grantResults.isEmpty() || 
            grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(
                this, 
                "Permiso de notificaciones denegado", 
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
```

### Paso 3: Configurar en la Activity

```kotlin
class AudioWithSyncServiceActivity : AppCompatActivity() {
    
    private val TAG = "AudioSyncService"
    private lateinit var container: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var miniPlayerConfig: MediastreamMiniPlayerConfig
    
    // MediaController para sincronización
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone && !controllerFuture.isCancelled) {
            controllerFuture.get()
        } else null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_sync_service)
        
        container = findViewById(R.id.main_media_frame)
        playerView = findViewById(R.id.player_view)
        
        val config = MediastreamPlayerConfig().apply {
            id = "646e3d4d5c910108b684a2b0"
            accountID = "5fbfd5b96660885379e1a129"
            type = MediastreamPlayerConfig.VideoTypes.VOD
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
            videoFormat = MediastreamPlayerConfig.AudioVideoFormat.MP3
            isDebug = true
            trackEnable = false
            showControls = true
            appName = "MyAudioApp"
        }
        
        startService(config)
    }
    
    private fun startService(config: MediastreamPlayerConfig) {
        miniPlayerConfig = MediastreamMiniPlayerConfig()
        
        val playerCallback = object : MediastreamPlayerCallback {
            override fun onPlay() {
                Log.d(TAG, "Reproduciendo")
                // Actualizar UI si es necesario
            }
            
            override fun onPause() {
                Log.d(TAG, "Pausado")
            }
            
            override fun onPlayerClosed() {
                finish()
            }
            
            // ... otros callbacks
        }
        
        // Inicializar servicio
        MediastreamPlayerServiceWithSync.initializeService(
            context = this,
            activity = this,
            config = config,
            container = container,
            playerContainer = playerView,
            miniPlayerConfig = miniPlayerConfig,
            trackEnable = false,
            accountId = config.accountID ?: "",
            playerCallback = playerCallback
        )
        
        // Crear MediaController para sincronización
        controllerFuture = MediaController.Builder(
            this,
            SessionToken(
                this, 
                ComponentName(this, MediastreamPlayerServiceWithSync::class.java)
            )
        ).buildAsync()
        
        controllerFuture.addListener(
            { setController(config) }, 
            MoreExecutors.directExecutor()
        )
    }
    
    private fun setController(config: MediastreamPlayerConfig) {
        val controller = this.controller ?: return
        
        // Sincronizar PlayerView con el servicio
        playerView.player = controller
        playerView.useController = true
        
        // Enviar evento de inicialización (EventBus)
        EventBus.getDefault().post(MessageEvent(controller, config))
        
        Log.d(TAG, "Controller sincronizado")
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        releaseService()
    }
    
    private fun releaseService() {
        // Liberar controller
        playerView.player?.release()
        playerView.player = null
        
        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
        }
        
        // Detener servicio
        val stopIntent = Intent(this, MediastreamPlayerServiceWithSync::class.java)
        stopIntent.action = "STOP_SERVICE"
        startService(stopIntent)
    }
}
```

### Paso 4: Controles Personalizados con Sincronización

```kotlin
class AudioWithSyncServiceActivity : AppCompatActivity() {
    
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    
    private fun setupControls() {
        btnPlayPause = findViewById(R.id.playOrpause)
        btnNext = findViewById(R.id.btnNext)
        btnPrevious = findViewById(R.id.btnPrevious)
        
        btnPlayPause.setOnClickListener {
            MediastreamPlayerServiceWithSync.getMsPlayer()?.let { player ->
                if (player.isPlaying()) {
                    player.pause()
                } else {
                    player.play()
                }
            }
        }
        
        btnNext.setOnClickListener {
            val config = MediastreamPlayerConfig().apply {
                id = "next_audio_id"
                type = MediastreamPlayerConfig.VideoTypes.VOD
                playerType = MediastreamPlayerConfig.PlayerType.AUDIO
                needReload = true
            }
            MediastreamPlayerServiceWithSync.getMsPlayer()?.reloadPlayer(config)
        }
        
        btnPrevious.setOnClickListener {
            val config = MediastreamPlayerConfig().apply {
                id = "previous_audio_id"
                type = MediastreamPlayerConfig.VideoTypes.VOD
                playerType = MediastreamPlayerConfig.PlayerType.AUDIO
                needReload = true
            }
            MediastreamPlayerServiceWithSync.getMsPlayer()?.reloadPlayer(config)
        }
    }
    
    // Actualizar UI basado en callbacks
    private val playerCallback = object : MediastreamPlayerCallback {
        override fun onPlay() {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        }
        
        override fun onPause() {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }
        
        override fun onBuffering() {
            btnPlayPause.setImageResource(R.drawable.ic_loading)
        }
        
        // ... otros callbacks
    }
}
```

## Android Auto Integration

### Configuración de Android Auto

El servicio `MediastreamPlayerServiceWithSync` incluye soporte completo para Android Auto.

#### 1. Archivo automotive_app_desc.xml

```xml
<!-- res/xml/automotive_app_desc.xml -->
<automotiveApp>
    <uses name="media" />
</automotiveApp>
```

#### 2. Estructura de Navegación en Android Auto

```
Root
├── Live (Radio en vivo)
│   ├── Radio 1
│   ├── Radio 2
│   └── Radio 3
├── Podcasts
│   ├── Podcast A
│   │   ├── Temporada 1
│   │   │   ├── Episodio 1
│   │   │   └── Episodio 2
│   │   └── Temporada 2
│   └── Podcast B
└── ... otros contenidos
```

El servicio maneja automáticamente:
- Navegación por categorías
- Búsqueda de contenido
- Reproducción desde Android Auto
- Controles en el volante del auto
- Artwork en pantalla del vehículo

## Casos de Uso

### Caso 1: App de Radio en Vivo

**Escenario**: Estación de radio que transmite 24/7.

```kotlin
class RadioStationActivity : AppCompatActivity() {
    
    private fun setupRadio() {
        val config = MediastreamPlayerConfig().apply {
            id = "live_radio_stream_id"
            accountID = "account_id"
            type = MediastreamPlayerConfig.VideoTypes.LIVE
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
            isDebug = false
            
            // Configuración de notificación
            notificationSongName = "Radio Station FM 99.9"
            notificationDescription = "Escuchando en vivo"
            notificationImageUrl = "https://example.com/station-logo.png"
            notificationHasNext = false  // No hay "siguiente" en radio en vivo
            notificationHasPrevious = false
            
            // Auto-rellenar con metadata del stream
            fillAutomaticallyAudioNotification = true
        }
        
        miniPlayerConfig = MediastreamMiniPlayerConfig().apply {
            songName = "Radio Station FM 99.9"
            description = "En vivo"
            imageUrl = "https://example.com/station-logo.png"
            color = Color.parseColor("#E91E63")
        }
        
        MediastreamPlayerService.initializeService(
            this, this, config, container, playerContainer,
            miniPlayerConfig, false, config.accountID ?: "",
            createRadioCallback()
        )
        
        startForegroundService()
    }
    
    private fun createRadioCallback() = object : MediastreamPlayerCallback {
        override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {
            // Actualizar notificación con canción actual
            data?.let {
                val song = it.optString("song", "")
                val artist = it.optString("artist", "")
                val poster = it.optString("poster", "")
                
                miniPlayerConfig.songName = song
                miniPlayerConfig.description = artist
                miniPlayerConfig.imageUrl = poster
                
                // Opcional: Notificar a la UI
                updateUI(song, artist, poster)
            }
        }
        
        override fun onError(error: String?) {
            // Intentar reconectar
            Handler(Looper.getMainLooper()).postDelayed({
                MediastreamPlayerService.getMsPlayer()?.play()
            }, 3000)
        }
        
        // ... otros callbacks
    }
}
```

### Caso 2: App de Podcasts con Episodios

**Escenario**: App que reproduce podcasts con navegación entre episodios.

```kotlin
class PodcastPlayerActivity : AppCompatActivity() {
    
    private val episodes = listOf(
        Episode("ep1", "Episodio 1", "Intro al tema"),
        Episode("ep2", "Episodio 2", "Profundizando"),
        Episode("ep3", "Episodio 3", "Conclusiones")
    )
    private var currentIndex = 0
    
    private fun setupPodcastPlayer() {
        val currentEpisode = episodes[currentIndex]
        
        val config = MediastreamPlayerConfig().apply {
            id = currentEpisode.id
            type = MediastreamPlayerConfig.VideoTypes.EPISODE
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
            loadNextAutomatically = true
            
            // Configurar botones de navegación
            notificationHasNext = currentIndex < episodes.size - 1
            notificationHasPrevious = currentIndex > 0
        }
        
        miniPlayerConfig = MediastreamMiniPlayerConfig().apply {
            songName = currentEpisode.title
            description = currentEpisode.description
            albumName = "Mi Podcast"
            setStateNext = currentIndex < episodes.size - 1
            setStatePrev = currentIndex > 0
        }
        
        MediastreamPlayerService.initializeService(
            this, this, config, container, playerContainer,
            miniPlayerConfig, false, config.accountID ?: "",
            createPodcastCallback()
        )
        
        startForegroundService()
    }
    
    private fun createPodcastCallback() = object : MediastreamPlayerCallback {
        override fun onNext() {
            if (currentIndex < episodes.size - 1) {
                currentIndex++
                loadEpisode(episodes[currentIndex])
            }
        }
        
        override fun onPrevious() {
            if (currentIndex > 0) {
                currentIndex--
                loadEpisode(episodes[currentIndex])
            }
        }
        
        override fun onEnd() {
            // Auto-avanzar al siguiente episodio
            if (currentIndex < episodes.size - 1) {
                currentIndex++
                loadEpisode(episodes[currentIndex])
            }
        }
        
        // ... otros callbacks
    }
    
    private fun loadEpisode(episode: Episode) {
        val config = MediastreamPlayerConfig().apply {
            id = episode.id
            type = MediastreamPlayerConfig.VideoTypes.EPISODE
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
        }
        
        miniPlayerConfig.songName = episode.title
        miniPlayerConfig.description = episode.description
        miniPlayerConfig.setStateNext = currentIndex < episodes.size - 1
        miniPlayerConfig.setStatePrev = currentIndex > 0
        
        MediastreamPlayerService.getMsPlayer()?.reloadPlayer(config)
    }
}

data class Episode(
    val id: String,
    val title: String,
    val description: String
)
```

### Caso 3: App de Música con Playlist

**Escenario**: Reproductor de música con playlist y shuffle.

```kotlin
class MusicPlayerActivity : AppCompatActivity() {
    
    private lateinit var playlist: MutableList<Track>
    private var currentTrackIndex = 0
    private var isShuffleEnabled = false
    
    private fun setupMusicPlayer() {
        // Cargar playlist
        playlist = loadPlaylist().toMutableList()
        
        val currentTrack = playlist[currentTrackIndex]
        
        val config = MediastreamPlayerConfig().apply {
            id = currentTrack.id
            type = MediastreamPlayerConfig.VideoTypes.VOD
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
            videoFormat = MediastreamPlayerConfig.AudioVideoFormat.MP3
            
            notificationHasNext = true
            notificationHasPrevious = true
        }
        
        miniPlayerConfig = MediastreamMiniPlayerConfig().apply {
            songName = currentTrack.title
            description = currentTrack.artist
            albumName = currentTrack.album
            imageUrl = currentTrack.coverUrl
            color = currentTrack.dominantColor
            setStateNext = true
            setStatePrev = true
        }
        
        // Usar servicio con sincronización para mejor experiencia
        MediastreamPlayerServiceWithSync.initializeService(
            this, this, config, container, playerView,
            miniPlayerConfig, false, config.accountID ?: "",
            createMusicCallback()
        )
        
        setupMediaController()
        setupUIControls()
    }
    
    private fun createMusicCallback() = object : MediastreamPlayerCallback {
        override fun onNext() {
            playNextTrack()
        }
        
        override fun onPrevious() {
            playPreviousTrack()
        }
        
        override fun onEnd() {
            // Auto-avanzar al siguiente
            playNextTrack()
        }
        
        override fun onPlay() {
            updatePlayPauseButton(isPlaying = true)
        }
        
        override fun onPause() {
            updatePlayPauseButton(isPlaying = false)
        }
        
        // ... otros callbacks
    }
    
    private fun playNextTrack() {
        if (isShuffleEnabled) {
            currentTrackIndex = (0 until playlist.size).random()
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % playlist.size
        }
        loadTrack(playlist[currentTrackIndex])
    }
    
    private fun playPreviousTrack() {
        currentTrackIndex = if (currentTrackIndex > 0) {
            currentTrackIndex - 1
        } else {
            playlist.size - 1
        }
        loadTrack(playlist[currentTrackIndex])
    }
    
    private fun loadTrack(track: Track) {
        val config = MediastreamPlayerConfig().apply {
            id = track.id
            type = MediastreamPlayerConfig.VideoTypes.VOD
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
        }
        
        miniPlayerConfig.apply {
            songName = track.title
            description = track.artist
            albumName = track.album
            imageUrl = track.coverUrl
        }
        
        MediastreamPlayerServiceWithSync.getMsPlayer()?.reloadPlayer(config)
        
        // Actualizar UI
        updateNowPlaying(track)
    }
    
    private fun setupUIControls() {
        findViewById<ImageButton>(R.id.btnShuffle).setOnClickListener {
            isShuffleEnabled = !isShuffleEnabled
            it.alpha = if (isShuffleEnabled) 1.0f else 0.5f
        }
    }
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val dominantColor: Int
)
```

## Gestión del Ciclo de Vida

### Mejores Prácticas

#### 1. Inicio del Servicio

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // SIEMPRE inicializar antes de usar
    MediastreamPlayerService.initializeService(...)
    
    // Luego iniciar el servicio
    val intent = Intent(this, MediastreamPlayerService::class.java)
    intent.action = "$packageName.action.startforeground"
    ContextCompat.startForegroundService(this, intent)
}
```

#### 2. Detención del Servicio

```kotlin
override fun onDestroy() {
    super.onDestroy()
    
    // Solo detener si realmente queremos que pare
    // (no detener si queremos que continúe en background)
    
    if (shouldStopPlayback) {
        stopService(Intent(this, MediastreamPlayerService::class.java))
    }
}

// Para servicio con sync
private fun releaseService() {
    playerView.player?.release()
    playerView.player = null
    
    if (::controllerFuture.isInitialized) {
        MediaController.releaseFuture(controllerFuture)
    }
    
    val stopIntent = Intent(this, MediastreamPlayerServiceWithSync::class.java)
    stopIntent.action = "STOP_SERVICE"
    startService(stopIntent)
}
```

#### 3. Manejo de onBackPressed

```kotlin
override fun onBackPressed() {
    // Opción A: Mantener servicio activo (mover a background)
    moveTaskToBack(true)
    
    // Opción B: Detener servicio y salir
    stopService(Intent(this, MediastreamPlayerService::class.java))
    super.onBackPressed()
    
    // Opción C: Preguntar al usuario
    showExitDialog()
}

private fun showExitDialog() {
    AlertDialog.Builder(this)
        .setTitle("Salir")
        .setMessage("¿Deseas detener la reproducción?")
        .setPositiveButton("Detener") { _, _ ->
            stopService(Intent(this, MediastreamPlayerService::class.java))
            finish()
        }
        .setNegativeButton("Continuar en background") { _, _ ->
            moveTaskToBack(true)
        }
        .show()
}
```

#### 4. Reconexión a Servicio Existente

```kotlin
override fun onResume() {
    super.onResume()
    
    // Verificar si el servicio ya está corriendo
    if (isServiceRunning(MediastreamPlayerService::class.java)) {
        // Reconectar a la instancia existente
        val player = MediastreamPlayerService.getMsPlayer()
        
        player?.let {
            // Actualizar UI con estado actual
            updateUIWithCurrentState(it)
        }
    }
}

private fun isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}
```

## Notificación Personalizada

### Estructura de la Notificación

```
┌─────────────────────────────────────┐
│  [App Icon]  Título de la Canción  │
│              Artista - Álbum        │
│                                     │
│  [Album Art - 64x64]                │
│                                     │
│  [◀◀]  [▶/⏸]  [▶▶]                │
│  Prev  Play   Next                  │
└─────────────────────────────────────┘
```

### Configuración Completa

```kotlin
// En MediastreamPlayerConfig
config.apply {
    // Texto
    notificationSongName = "Never Gonna Give You Up"
    notificationDescription = "Rick Astley - Whenever You Need Somebody"
    
    // Imágenes
    notificationImageUrl = "https://example.com/album-cover.jpg"
    notificationIconUrl = R.drawable.ic_notification_small
    
    // Color de acento (Android 8+)
    notificationColor = Color.parseColor("#1DB954") // Verde Spotify
    
    // Botones de navegación
    notificationHasNext = true
    notificationHasPrevious = true
    
    // Auto-actualización (para streams en vivo)
    fillAutomaticallyAudioNotification = true
}
```

### Personalización Avanzada

```kotlin
// Actualización manual de contenido
val service = MediastreamPlayerService.getMsPlayer()

// Cambiar metadata
miniPlayerConfig.apply {
    songName = "Nueva Canción"
    description = "Nuevo Artista"
    albumName = "Nuevo Álbum"
    imageUrl = "nueva-imagen.jpg"
    
    // Controles dinámicos
    setStateNext = hasNextSong
    setStatePrev = hasPreviousSong
}

// La notificación se actualizará automáticamente
```

## Debugging y Testing

### Logs Importantes

```kotlin
// Activar logs en desarrollo
config.isDebug = true

// Los servicios generan logs como:
[MediastreamPlayerService] onCreate
[MediastreamPlayerService] setupPlayerNotificationManager
[MediastreamPlayerService] onStartCommand: action: startforeground
[MediastreamPlayerService] Player initialized successfully
[MediastreamPlayerService] Notification posted
```

### Testing en Diferentes Escenarios

#### 1. Test: Reproducción en Background

```
1. Iniciar reproducción
2. Presionar HOME
3. Verificar notificación visible
4. Verificar audio continúa
5. Controlar desde notificación
```

#### 2. Test: Reconexión

```
1. Iniciar reproducción
2. Presionar HOME
3. Cerrar app desde Recientes
4. Abrir app nuevamente
5. Verificar reconexión al servicio
6. Verificar estado sincronizado
```

#### 3. Test: Controles Bluetooth

```
1. Conectar auriculares Bluetooth
2. Iniciar reproducción
3. Usar controles de auriculares (play/pause/next)
4. Verificar respuesta correcta
```

#### 4. Test: Lock Screen

```
1. Iniciar reproducción
2. Bloquear dispositivo
3. Verificar controles en lock screen
4. Verificar artwork visible
5. Controlar desde lock screen
```

#### 5. Test: Interrupciones

```
1. Reproducir audio
2. Recibir llamada
3. Verificar pausa automática
4. Terminar llamada
5. Verificar reanudación (opcional)
```

### Comandos ADB para Testing

```bash
# Ver servicios en ejecución
adb shell dumpsys activity services | grep Mediastream

# Ver notificaciones activas
adb shell dumpsys notification

# Verificar MediaSession
adb shell dumpsys media_session

# Simular botones de media
adb shell input keyevent KEYCODE_MEDIA_PLAY
adb shell input keyevent KEYCODE_MEDIA_PAUSE
adb shell input keyevent KEYCODE_MEDIA_NEXT
adb shell input keyevent KEYCODE_MEDIA_PREVIOUS

# Ver logs del servicio
adb logcat | grep MediastreamPlayer

# Matar app pero mantener servicio
adb shell am force-stop <package> # Solo mata activity, no servicio
```

## Problemas Comunes y Soluciones

### Problema 1: Servicio No Inicia

**Síntomas:**
- No aparece notificación
- Audio no se reproduce
- Logs muestran error

**Causas posibles:**
1. `initializeService()` no llamado antes de `startForegroundService()`
2. Permisos faltantes en AndroidManifest
3. Container null

**Solución:**

```kotlin
// ORDEN CORRECTO:

// 1. Inicializar primero
MediastreamPlayerService.initializeService(...)

// 2. Luego iniciar servicio
val intent = Intent(this, MediastreamPlayerService::class.java)
intent.action = "$packageName.action.startforeground"
ContextCompat.startForegroundService(this, intent)

// Verificar en manifest:
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<service 
    android:name="am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerService"
    android:foregroundServiceType="mediaPlayback" />
```

### Problema 2: Notificación No Se Actualiza

**Síntomas:**
- Notificación muestra información vieja
- Cambios en `miniPlayerConfig` no reflejan

**Causa:**
- Falta callback `onConfigChange` o `onLiveAudioCurrentSongChanged`

**Solución:**

```kotlin
override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {
    data?.let {
        // Actualizar miniPlayerConfig
        miniPlayerConfig.songName = it.optString("song", "")
        miniPlayerConfig.description = it.optString("artist", "")
        miniPlayerConfig.imageUrl = it.optString("poster", "")
        
        // NO es necesario hacer nada más
        // El servicio detecta el cambio automáticamente
    }
}

// O actualización manual:
override fun onConfigChange(config: MediastreamMiniPlayerConfig?) {
    config?.let {
        // La notificación ya se actualizó
        Log.d(TAG, "Notificación actualizada: ${it.songName}")
    }
}
```

### Problema 3: Servicio Se Detiene Solo

**Síntomas:**
- Audio se detiene después de unos minutos
- Servicio desaparece del sistema

**Causa:**
- Sistema mata el servicio por recursos
- Falta `START_STICKY` o mal implementado

**Solución:**

```kotlin
// El servicio ya retorna START_STICKY internamente
// Asegurar no llamar stopSelf() incorrectamente

override fun onTaskRemoved(rootIntent: Intent?) {
    // NO llamar stopSelf() aquí si quieres que continúe
    // super.onTaskRemoved(rootIntent)
    
    // Solo si quieres detener cuando app se cierra:
    stopSelf()
}

// En el servicio, verificar:
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ...
    return START_STICKY  // Reiniciar servicio si muere
}
```

### Problema 4: Controles de Notificación No Funcionan

**Síntomas:**
- Botones en notificación no responden
- No se ejecutan callbacks onNext/onPrevious

**Causa:**
- Botones no habilitados correctamente
- BroadcastReceiver no registrado

**Solución:**

```kotlin
// Verificar que los botones estén habilitados:
config.notificationHasNext = true
config.notificationHasPrevious = true

miniPlayerConfig.setStateNext = true
miniPlayerConfig.setStatePrev = true

// Implementar callbacks:
override fun onNext() {
    Log.d(TAG, "Next presionado desde notificación")
    // Tu lógica aquí
}

override fun onPrevious() {
    Log.d(TAG, "Previous presionado desde notificación")
    // Tu lógica aquí
}
```

### Problema 5: Crash en Android 13+

**Síntomas:**
- App crashea al iniciar servicio en Android 13
- SecurityException en logs

**Causa:**
- Falta permiso POST_NOTIFICATIONS

**Solución:**

```kotlin
if (Build.VERSION.SDK_INT >= 33) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != 
        PackageManager.PERMISSION_GRANTED) {
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS), 
            REQUEST_NOTIFICATIONS
        )
        return // Esperar permiso antes de iniciar servicio
    }
}

// Iniciar servicio solo después de tener permiso
startServiceWithPermission()
```

### Problema 6: Artwork No Se Carga

**Síntomas:**
- Notificación sin imagen
- Icono por defecto en lugar de carátula

**Causa:**
- URL inválida o red lenta
- Glide no puede cargar imagen

**Solución:**

```kotlin
// Verificar URL válida
miniPlayerConfig.imageUrl = "https://valid-url.com/cover.jpg"

// Fallback con imagen local:
if (miniPlayerConfig.imageUrl.isNullOrEmpty()) {
    miniPlayerConfig.imageUrl = "android.resource://${packageName}/${R.drawable.default_cover}"
}

// Verificar en logs:
[MediastreamPlayerService] getCurrentLargeIcon: <url>
```

## Comparación: Servicio Básico vs Avanzado

| Característica | MediastreamPlayerService | MediastreamPlayerServiceWithSync |
|----------------|-------------------------|----------------------------------|
| **Reproducción background** | ✅ Sí | ✅ Sí |
| **Notificación multimedia** | ✅ Sí | ✅ Sí |
| **Controles sistema** | ✅ Básico | ✅ Completo |
| **MediaSession** | ✅ Básico | ✅ Avanzado |
| **Sincronización UI** | ❌ Manual | ✅ Automática |
| **MediaController** | ❌ No | ✅ Sí |
| **Android Auto** | ❌ No | ✅ Sí |
| **EventBus** | ❌ No | ✅ Sí |
| **Library browsing** | ❌ No | ✅ Sí |
| **Comandos custom** | ❌ Limitado | ✅ Completo |
| **Complejidad** | 🟢 Baja | 🟡 Media |
| **Uso recomendado** | Radio simple, audio básico | Podcasts, música, apps complejas |

## Recomendaciones Finales

### ¿Cuándo Usar Cada Servicio?

**Usa MediastreamPlayerService si:**
- App simple de radio/audio
- No necesitas sincronización compleja
- No planeas soportar Android Auto
- Quieres implementación rápida

**Usa MediastreamPlayerServiceWithSync si:**
- App profesional de audio
- Necesitas UI sincronizada automáticamente
- Quieres soporte Android Auto
- Requieres navegación por contenido
- App con múltiples activities/fragments

### Mejores Prácticas Generales

1. **Siempre inicializar antes de iniciar servicio**
2. **Manejar permisos en Android 13+**
3. **Implementar todos los callbacks necesarios**
4. **Proporcionar artwork de calidad**
5. **Testear en diferentes versiones de Android**
6. **Verificar comportamiento con interrupciones (llamadas, alarmas)**
7. **Optimizar carga de imágenes (Glide cache)**
8. **Manejar errores de red gracefully**
9. **Considerar battery optimization**
10. **Documentar configuración para otros desarrolladores**

## Conclusión

Los servicios de reproducción en background del SDK de Mediastream proporcionan:

✅ **Reproducción continua** independiente del ciclo de vida de la Activity  
✅ **Notificaciones multimedia** profesionales con controles  
✅ **Integración completa** con el sistema operativo  
✅ **Soporte Android Auto** para apps de audio en vehículos  
✅ **Sincronización automática** entre UI y servicio (con sync)  
✅ **Experiencia de usuario** comparable a apps líderes del mercado  

Estos servicios son **esenciales** para cualquier app de audio profesional y proporcionan todas las herramientas necesarias para crear experiencias de reproducción de clase mundial.

---

**Documento preparado para el equipo de desarrollo**  
**Fecha**: Febrero 2026  
**Versión**: 1.0  
**Estado**: Implementado y documentado
