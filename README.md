# SDK QA - Mediastream SDK Test Suite

Aplicación Android de pruebas para validar la integración del SDK de Mediastream.

## 📱 Descripción

Esta aplicación proporciona una suite de pruebas para verificar las diferentes funcionalidades del SDK de Mediastream en Android, incluyendo reproducción de audio y video en múltiples modalidades.

## 🎯 Casos de Prueba

### Audio (10 casos)
| Caso | Descripción |
|------|-------------|
| **AOD Simple** | Audio On Demand básico sin servicio de background |
| **AOD with Service** | Audio On Demand con servicio para reproducción en background |
| **Episode** | Reproducción de episodios de audio con carga automática del siguiente |
| **Local Audio** | Reproducción de archivo de audio local sin servicio |
| **Local Audio with Service** | Reproducción de archivo de audio local con servicio de background |
| **Live Audio** | Streaming de audio en vivo |
| **Live Audio with Service** | Streaming de audio en vivo con servicio de background |
| **Live Audio DVR** | Audio en vivo con soporte DVR (Live, DVR, DVR Start, DVR VOD) |
| **Mixed Audio** | Selector dinámico entre Local, AOD, Episode y Live sin servicio |
| **Mixed Audio with Service** | Selector dinámico entre Local, AOD, Episode y Live con servicio |

### Video (8 casos)
| Caso | Descripción |
|------|-------------|
| **VOD Simple** | Video On Demand básico |
| **Local Video** | Reproducción de archivo de video local sin servicio |
| **Local Video with Service** | Reproducción de archivo de video local con servicio de background |
| **Episode** | Reproducción de episodios de video con carga automática del siguiente |
| **Live Video** | Streaming de video en vivo |
| **Live Video DVR** | Video en vivo con soporte DVR (Live, DVR, DVR Start, DVR VOD) |
| **Mixed Video** | Selector dinámico entre Local, VOD, Live y Episode sin servicio |
| **Mixed Video with Service** | Selector dinámico entre Local, VOD, Live y Episode con servicio |

## 🛠 Requisitos

- Android Studio Ladybug o superior
- Android SDK 36 (compileSdk)
- Android 7.0+ (minSdk 24)
- Kotlin 2.0.21

## 📦 Dependencias Principales

```kotlin
implementation("io.github.mediastream:mediastreamplatformsdkandroid:9.6.5-alpha01")
implementation("org.greenrobot:eventbus:3.3.1")
implementation("androidx.media3:media3-session:1.4.0")
implementation("androidx.media3:media3-ui:1.4.0")
```

## 🚀 Instalación

1. Clona el repositorio:
```bash
git clone https://github.com/user/SDKQA.git
```

2. Abre el proyecto en Android Studio

3. Sincroniza con Gradle

4. Ejecuta la aplicación en un dispositivo o emulador

## 📁 Estructura del Proyecto

```
app/src/main/java/com/example/sdkqa/
├── MainActivity.kt          # Pantalla principal con lista de casos
├── TestCase.kt              # Modelo de datos para casos de prueba
├── TestCaseAdapter.kt       # Adapter para RecyclerView
├── audio/
│   ├── AudioAodSimpleActivity.kt
│   ├── AudioAodWithServiceActivity.kt
│   ├── AudioEpisodeActivity.kt
│   ├── AudioLocalActivity.kt
│   ├── AudioLocalWithServiceActivity.kt
│   ├── AudioLiveActivity.kt
│   ├── AudioLiveWithServiceActivity.kt
│   ├── AudioLiveDvrActivity.kt
│   ├── AudioMixedActivity.kt
│   └── AudioMixedWithServiceActivity.kt
└── video/
    ├── VideoVodSimpleActivity.kt
    ├── VideoLocalActivity.kt
    ├── VideoLocalWithServiceActivity.kt
    ├── VideoEpisodeActivity.kt
    ├── VideoLiveActivity.kt
    ├── VideoLiveDvrActivity.kt
    ├── VideoMixedActivity.kt
    └── VideoMixedWithServiceActivity.kt

app/src/main/res/raw/
├── sample_audio             # Archivo de audio local para pruebas
└── sample_video             # Archivo de video local para pruebas
```

## 🔧 Configuración

Los IDs de contenido y configuraciones se encuentran en cada Activity correspondiente. Para cambiar el entorno de desarrollo:

```kotlin
// Descomentar para usar entorno de desarrollo
// environment = MediastreamPlayerConfig.Environment.DEV
```

## 📝 Logs

Todos los eventos del player se registran con el TAG `SDK-QA` para facilitar el debugging:

```bash
adb logcat -s SDK-QA
```

## 🎨 Diseño

La aplicación utiliza un tema oscuro moderno con:
- Fondo azul oscuro (#0D1B2A)
- Acentos cyan (#00D9FF) para Audio
- Acentos coral (#FF6B6B) para Video
- Cards con Material Design

## 📄 Licencia

Uso interno para QA del SDK de Mediastream.

---

Desarrollado para pruebas de calidad del SDK de Mediastream.
