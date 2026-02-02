# Guía de Implementación: Picture-in-Picture (PIP)

## Resumen Ejecutivo

Esta guía documenta la implementación completa del modo Picture-in-Picture (PIP) en el SDK de Mediastream para Android. El PIP permite que los usuarios continúen viendo contenido de video en una ventana flotante mientras navegan por otras aplicaciones, mejorando significativamente la experiencia de usuario y la multitarea.

## ¿Qué es Picture-in-Picture?

Picture-in-Picture es una característica de Android (API 26+) que permite reproducir video en una pequeña ventana flotante superpuesta sobre otras aplicaciones. El usuario puede:

- Ver contenido mientras usa otras apps
- Mover y redimensionar la ventana PIP
- Acceder a controles básicos de reproducción
- Cerrar o expandir el video cuando lo desee

## Requisitos Técnicos

### Versión de Android
- **Mínimo**: Android 8.0 (API 26 / Oreo)
- **Anotación requerida**: `@RequiresApi(Build.VERSION_CODES.O)`

### Permisos y Configuración
No se requieren permisos especiales en el `AndroidManifest.xml`, solo la declaración de soporte en la actividad.

## Arquitectura de la Implementación

### 1. Estructura de Componentes

```
MediastreamPlayer
    ├── pipHandler: MediastreamPlayerPip
    ├── startPiP()
    └── onPictureInPictureModeChanged()
         
MediastreamPlayerPip
    ├── enterPictureInPictureMode()
    ├── updatePipParams()
    └── onPictureInPictureModeChanged()
         
Activity (VideoOnDemandActivity)
    ├── onUserLeaveHint()
    └── onPictureInPictureModeChanged()
```

### 2. Flujo de Operación

```
┌─────────────────────────────────────────┐
│  Usuario reproduce video VOD            │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  Usuario presiona botón HOME            │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  onUserLeaveHint() se ejecuta           │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  player?.startPiP() verifica config     │
└──────────────┬──────────────────────────┘
               │
         ┌─────┴─────┐
         │           │
         ▼           ▼
    HABILITADO   DESHABILITADO
         │           │
         │           └──> Sale de la app
         │
         ▼
┌─────────────────────────────────────────┐
│  pipHandler.enterPictureInPictureMode() │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  Video se muestra en ventana 16:9       │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│  onPictureInPictureModeChanged(true)    │
│  - Oculta controles innecesarios        │
│  - Ajusta UI para modo compacto         │
└─────────────────────────────────────────┘
```

## Implementación Detallada

### Paso 1: Configuración del AndroidManifest.xml

La actividad que soportará PIP debe declararlo explícitamente:

```xml
<activity
    android:name=".video.VideoOnDemandActivity"
    android:supportsPictureInPicture="true"
    android:configChanges="screenSize|smallestScreenSize|screenLayout|orientation"
    android:exported="false" />
```

**Atributos clave:**

- **`android:supportsPictureInPicture="true"`**: Habilita el soporte PIP para esta actividad
- **`android:configChanges`**: Evita que la actividad se destruya y recree cuando cambia el tamaño o la orientación
  - `screenSize`: Cambios de tamaño de pantalla
  - `smallestScreenSize`: Cambios en el tamaño mínimo
  - `screenLayout`: Cambios en el layout de pantalla
  - `orientation`: Cambios de orientación

### Paso 2: Implementación en la Activity

#### 2.1 Activación Automática al Salir

```kotlin
@RequiresApi(Build.VERSION_CODES.O)
override fun onUserLeaveHint() {
    player?.startPiP()
}
```

**¿Qué hace `onUserLeaveHint()`?**

Este método del ciclo de vida de Android se invoca cuando el usuario está a punto de dejar la actividad de forma voluntaria, típicamente al:
- Presionar el botón HOME
- Cambiar a otra aplicación desde el selector de apps recientes
- Abrir una notificación

**No se invoca cuando:**
- Se abre un diálogo
- Llega una llamada telefónica
- Se abre otra actividad de la misma app

#### 2.2 Manejo de Cambios de Estado PIP

```kotlin
@RequiresApi(Build.VERSION_CODES.O)
override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean, 
    newConfig: Configuration
) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    player?.onPictureInPictureModeChanged(isInPictureInPictureMode)
}
```

**Parámetros:**

- **`isInPictureInPictureMode`**: `true` cuando entra en PIP, `false` cuando sale
- **`newConfig`**: Nueva configuración del sistema (tamaño de pantalla, orientación, etc.)

**Usos típicos:**

- Ocultar/mostrar controles según el modo
- Ajustar el layout de la UI
- Pausar/reanudar funcionalidades no esenciales
- Actualizar la visualización de subtítulos

### Paso 3: Lógica del SDK - MediastreamPlayer

#### 3.1 Método `startPiP()`

```kotlin
fun startPiP() {
    val shouldEnterPiP = when (msConfig?.pip) {
        MediastreamPlayerConfig.FlagStatus.ENABLE -> true
        MediastreamPlayerConfig.FlagStatus.DISABLE -> false
        MediastreamPlayerConfig.FlagStatus.NONE -> mediaInfo?.player?.pip == true
        else -> false
    }
    if (shouldEnterPiP) {
        pipHandler?.enterPictureInPictureMode()
    }
}
```

**Sistema de Prioridades:**

1. **Configuración Local (Máxima prioridad)**
   ```kotlin
   config.pip = MediastreamPlayerConfig.FlagStatus.ENABLE  // Fuerza habilitado
   config.pip = MediastreamPlayerConfig.FlagStatus.DISABLE // Fuerza deshabilitado
   ```

2. **Configuración desde API (Prioridad media)**
   ```kotlin
   config.pip = MediastreamPlayerConfig.FlagStatus.NONE
   // Se usa el valor de: mediaInfo?.player?.pip
   ```

3. **Valor por defecto (Prioridad baja)**
   ```kotlin
   // Si no hay configuración: false (deshabilitado)
   ```

#### 3.2 Método de Notificación

```kotlin
fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    pipHandler?.onPictureInPictureModeChanged(isInPictureInPictureMode)
}
```

Delega el manejo al `MediastreamPlayerPip` para mantener la separación de responsabilidades.

### Paso 4: Clase MediastreamPlayerPip

Esta clase encapsula toda la lógica específica de PIP:

```kotlin
class MediastreamPlayerPip(private val activity: Activity) {

    var TAGDEBUG = "MP-Debug"

    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPictureInPictureMode() {
        val pipParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()
        activity.enterPictureInPictureMode(pipParams)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePipParams(): PictureInPictureParams {
        val aspectRadio = Rational(16, 9)
        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRadio)
            .build()
    }

    fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        if (isInPictureInPictureMode) {
            Log.d(TAGDEBUG, "Entró en modo PIP")
        } else {
            Log.d(TAGDEBUG, "Salió de modo PIP")
        }
    }
}
```

#### Parámetros de PIP

**Aspect Ratio (Relación de Aspecto):**
```kotlin
.setAspectRatio(Rational(16, 9))
```

- Define las proporciones de la ventana PIP
- `16:9` es el estándar para video widescreen
- Puede ser `4:3` para contenido clásico
- Rango válido: entre `0.4` y `2.39` aproximadamente

**Otros parámetros disponibles:**

```kotlin
PictureInPictureParams.Builder()
    .setAspectRatio(Rational(16, 9))
    .setSourceRectHint(sourceRectHint) // Área de origen para animación
    .setActions(actions) // Acciones personalizadas (play, pause, etc.)
    .setAutoEnterEnabled(true) // Auto-entrar en PIP (API 31+)
    .setSeamlessResizeEnabled(true) // Redimensionamiento suave (API 31+)
    .build()
```

## Configuración del Player

### Habilitación en la Configuración Local

```kotlin
val config = MediastreamPlayerConfig()
config.id = "video_id_here"
config.type = MediastreamPlayerConfig.VideoTypes.VOD
config.pip = MediastreamPlayerConfig.FlagStatus.ENABLE // Habilitar PIP

player = MediastreamPlayer(this, config, container, playerView, supportFragmentManager)
```

### Deshabilitación Explícita

```kotlin
config.pip = MediastreamPlayerConfig.FlagStatus.DISABLE // Deshabilitar PIP
```

### Uso de Configuración desde API

```kotlin
config.pip = MediastreamPlayerConfig.FlagStatus.NONE
// El SDK usará el valor que venga del endpoint de configuración
```

## Casos de Uso

### Caso 1: VOD con PIP Automático

**Escenario:**
Usuario reproduce un video bajo demanda y presiona el botón HOME para revisar un mensaje.

**Flujo:**
1. Usuario carga video VOD en `VideoOnDemandActivity`
2. Video se reproduce normalmente
3. Usuario presiona HOME
4. `onUserLeaveHint()` detecta la salida
5. `player?.startPiP()` verifica configuración
6. PIP está habilitado → entra en modo PIP
7. Video continúa en ventana flotante
8. Usuario puede revisar mensajes mientras mira

**Resultado:**
✅ Experiencia sin interrupciones

### Caso 2: PIP Deshabilitado

**Escenario:**
Contenido premium que no debe reproducirse en PIP por políticas de licenciamiento.

**Flujo:**
1. Configuración: `config.pip = FlagStatus.DISABLE`
2. Usuario presiona HOME
3. `startPiP()` verifica configuración
4. PIP está deshabilitado → no activa PIP
5. Video se pausa y app va al background

**Resultado:**
✅ Respeta políticas de contenido

### Caso 3: Configuración desde API

**Escenario:**
Control centralizado de PIP desde el backend.

**Flujo:**
1. Configuración: `config.pip = FlagStatus.NONE`
2. SDK consulta API de configuración
3. API responde: `mediaInfo.player.pip = true`
4. Usuario presiona HOME
5. Se activa PIP según valor de API

**Resultado:**
✅ Control remoto de funcionalidades

### Caso 4: Transición PIP → Pantalla Completa

**Escenario:**
Usuario quiere volver a ver el video en pantalla completa.

**Flujo:**
1. Video en modo PIP
2. Usuario toca la ventana PIP
3. Activity se restaura automáticamente
4. `onPictureInPictureModeChanged(false)` se ejecuta
5. Controles completos se muestran nuevamente
6. Video continúa desde la misma posición

**Resultado:**
✅ Transición fluida entre modos

## Mejores Prácticas

### 1. Gestión de Estado

**✅ Recomendado:**
```kotlin
override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    
    if (isInPictureInPictureMode) {
        // Ocultar UI no esencial
        hideControls()
        hideTitle()
        hideSubtitles() // O ajustar tamaño
    } else {
        // Restaurar UI completa
        showControls()
        showTitle()
        showSubtitles()
    }
    
    player?.onPictureInPictureModeChanged(isInPictureInPictureMode)
}
```

**❌ No Recomendado:**
```kotlin
// No asumir que siempre se debe entrar en PIP
override fun onUserLeaveHint() {
    enterPictureInPictureMode() // Ignora configuración
}
```

### 2. Verificación de Disponibilidad

```kotlin
@RequiresApi(Build.VERSION_CODES.O)
private fun isPipSupported(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    } else {
        false
    }
}
```

### 3. Manejo de Errores

```kotlin
@RequiresApi(Build.VERSION_CODES.O)
override fun onUserLeaveHint() {
    try {
        if (isPipSupported()) {
            player?.startPiP()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error al entrar en modo PIP: ${e.message}")
        // Continuar con comportamiento normal
    }
}
```

### 4. Consideraciones de UX

**En modo PIP:**
- ❌ No mostrar diálogos (no son visibles)
- ❌ No mostrar notificaciones intrusivas
- ✅ Mantener controles básicos (play/pause)
- ✅ Continuar reproducción sin interrupciones

**Al salir de PIP:**
- ✅ Restaurar estado completo de UI
- ✅ Sincronizar posición de reproducción
- ✅ Reactivar funcionalidades pausadas

## Testing y Validación

### Test Cases Críticos

#### 1. Entrada Básica en PIP
**Pasos:**
1. Iniciar reproducción de video VOD
2. Presionar botón HOME
3. Verificar que entra en modo PIP
4. Verificar que video continúa reproduciéndose

**Resultado esperado:**
✅ Video se muestra en ventana flotante 16:9
✅ Reproducción continúa sin interrupciones

#### 2. Salida de PIP
**Pasos:**
1. Estar en modo PIP
2. Tocar la ventana PIP
3. Verificar que vuelve a pantalla completa

**Resultado esperado:**
✅ Activity se restaura completamente
✅ Controles completos visibles
✅ Posición de reproducción se mantiene

#### 3. PIP Deshabilitado
**Pasos:**
1. Configurar `config.pip = FlagStatus.DISABLE`
2. Presionar HOME durante reproducción
3. Verificar comportamiento

**Resultado esperado:**
✅ No entra en modo PIP
✅ Video se pausa
✅ Activity va al background normal

#### 4. Configuración desde API
**Pasos:**
1. Configurar `config.pip = FlagStatus.NONE`
2. Simular respuesta API con `pip = true`
3. Presionar HOME

**Resultado esperado:**
✅ Entra en modo PIP según valor de API

#### 5. Rotación en PIP
**Pasos:**
1. Entrar en modo PIP
2. Rotar dispositivo
3. Verificar ventana PIP

**Resultado esperado:**
✅ Ventana PIP se adapta a nueva orientación
✅ Reproducción continúa sin problemas

### Métricas de Validación

- **Tiempo de transición**: < 300ms al entrar/salir de PIP
- **Continuidad de reproducción**: 0 interrupciones durante transición
- **Sincronización de estado**: 100% de precisión en posición de reproducción
- **Estabilidad de UI**: Sin crashes durante cambios de modo
- **Compatibilidad**: Funciona en 100% de dispositivos API 26+

## Debugging

### Logs Importantes

```kotlin
// En MediastreamPlayerPip
Log.d("MP-Debug", "Entrando en modo PIP")
Log.d("MP-Debug", "PIP activado: isInPictureInPictureMode = $isInPictureInPictureMode")

// En MediastreamPlayer
Log.d("MP-Debug", "startPiP() - shouldEnterPiP = $shouldEnterPiP")
Log.d("MP-Debug", "Config local pip = ${msConfig?.pip}")
Log.d("MP-Debug", "API pip = ${mediaInfo?.player?.pip}")

// En Activity
Log.d("VideoOnDemand", "onUserLeaveHint() llamado")
Log.d("VideoOnDemand", "PIP mode changed: $isInPictureInPictureMode")
```

### Comandos ADB para Testing

```bash
# Forzar entrada en PIP
adb shell am broadcast -a android.intent.action.ENTER_PICTURE_IN_PICTURE

# Verificar si PIP está soportado
adb shell pm list features | grep "feature:android.software.picture_in_picture"

# Simular presión de botón HOME
adb shell input keyevent KEYCODE_HOME

# Ver logs relacionados con PIP
adb logcat | grep -i "pip\|picture"
```

### Problemas Comunes

#### Problema 1: PIP No Se Activa

**Síntomas:**
- Presionar HOME no activa PIP
- Video se pausa en lugar de entrar en PIP

**Causas posibles:**
1. `supportsPictureInPicture` no está en el manifest
2. `config.pip = FlagStatus.DISABLE`
3. Versión de Android < 8.0
4. Dispositivo no soporta PIP

**Solución:**
```kotlin
// Verificar soporte antes de intentar
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
        player?.startPiP()
    } else {
        Log.w(TAG, "PIP no soportado en este dispositivo")
    }
}
```

#### Problema 2: Ventana PIP Tamaño Incorrecto

**Síntomas:**
- Ventana PIP muy pequeña o muy grande
- Aspect ratio incorrecto

**Solución:**
```kotlin
// Asegurar aspect ratio correcto
val aspectRatio = Rational(16, 9)
val params = PictureInPictureParams.Builder()
    .setAspectRatio(aspectRatio)
    .build()
activity.setPictureInPictureParams(params)
```

#### Problema 3: UI No Se Actualiza al Cambiar de Modo

**Síntomas:**
- Controles no se ocultan/muestran correctamente
- Layout incorrecto después de salir de PIP

**Solución:**
```kotlin
override fun onPictureInPictureModeChanged(
    isInPictureInPictureMode: Boolean,
    newConfig: Configuration
) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    
    // Forzar actualización de UI
    window.decorView.requestLayout()
    
    player?.onPictureInPictureModeChanged(isInPictureInPictureMode)
}
```

## Extensiones Futuras

### Posibles Mejoras

#### 1. Controles Personalizados en PIP

```kotlin
// API 26+: Agregar botones de acción
val actions = arrayListOf(
    // Play/Pause
    RemoteAction(
        Icon.createWithResource(this, R.drawable.ic_pause),
        "Pausar",
        "Pausar reproducción",
        pausePendingIntent
    ),
    // Siguiente
    RemoteAction(
        Icon.createWithResource(this, R.drawable.ic_next),
        "Siguiente",
        "Siguiente episodio",
        nextPendingIntent
    )
)

val params = PictureInPictureParams.Builder()
    .setActions(actions)
    .build()
```

#### 2. Auto-entrada en PIP

```kotlin
// API 31+: Entrar automáticamente en PIP sin esperar onUserLeaveHint
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val params = PictureInPictureParams.Builder()
        .setAutoEnterEnabled(true)
        .build()
    setPictureInPictureParams(params)
}
```

#### 3. PIP Expandible

```kotlin
// API 31+: Permitir expansión sin salir de PIP
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val params = PictureInPictureParams.Builder()
        .setExpandedAspectRatio(Rational(1, 1))
        .build()
}
```

#### 4. Analytics de Uso PIP

```kotlin
fun trackPipUsage() {
    analytics.logEvent("pip_entered", Bundle().apply {
        putString("content_id", currentVideoId)
        putLong("position_ms", player?.currentPosition ?: 0)
        putString("content_type", "vod")
    })
}
```

## Referencias Técnicas

### Documentación Android
- [Picture-in-Picture Support](https://developer.android.com/develop/ui/views/picture-in-picture)
- [PictureInPictureParams](https://developer.android.com/reference/android/app/PictureInPictureParams)
- [Activity.onUserLeaveHint()](https://developer.android.com/reference/android/app/Activity#onUserLeaveHint())

### Archivos del Proyecto
- `mediastreamplatformsdkandroid/src/main/java/am/mediastre/mediastreamplatformsdkandroid/MediastreamPlayerPip.kt`
- `mediastreamplatformsdkandroid/src/main/java/am/mediastre/mediastreamplatformsdkandroid/MediastreamPlayer.kt`
- `app/src/main/java/am/mediastre/mediastreamsampleapp/video/VideoOnDemandActivity.kt`
- `app/src/main/AndroidManifest.xml`

## Conclusión

La implementación de PIP en el SDK de Mediastream proporciona:

✅ **Experiencia de usuario mejorada** con reproducción continua en multitarea  
✅ **Flexibilidad de configuración** con prioridad local sobre API  
✅ **Implementación robusta** con manejo adecuado de estados  
✅ **Fácil integración** para desarrolladores que usan el SDK  
✅ **Compatibilidad** con Android 8.0+

El equipo de desarrollo debe enfocarse en:
1. Validar todos los casos de prueba documentados
2. Monitorear analytics de uso de PIP
3. Considerar implementar controles personalizados (API 26+)
4. Evaluar auto-entrada en PIP para dispositivos Android 12+ (API 31+)

---

**Documento preparado para el equipo de desarrollo**  
**Fecha**: Febrero 2026  
**Versión**: 1.0  
**Estado**: Documentado e implementado
