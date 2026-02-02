# Plan de Pruebas Esenciales: Funcionalidad Reels

## Información del Documento

**Proyecto**: MediastreamPlatformSDK Android - Reels Feature  
**Versión**: 1.0  
**Fecha**: Febrero 2026  
**Estado**: Feature Branch (feature/reels)  
**Prioridad**: Alta

> ⚠️ **Nota**: Este documento contiene únicamente las pruebas críticas y esenciales que deben ejecutarse antes de cualquier release.

## Índice

1. [Objetivos y Métricas](#objetivos-y-métricas)
2. [Configuración de Pruebas](#configuración-de-pruebas)
3. [Test Cases Críticos](#test-cases-críticos)
4. [Checklist de Regresión](#checklist-de-regresión)
5. [Criterios de Aceptación](#criterios-de-aceptación)
6. [Scripts de Testing](#scripts-de-testing)

---

## Objetivos y Métricas

### Métricas Críticas de Aceptación

| Métrica | Target | Criticidad |
|---------|--------|------------|
| **FPS durante scroll** | ≥ 55 FPS | 🔴 Bloqueante |
| **Memoria máxima** | < 300MB | 🔴 Bloqueante |
| **Tiempo de carga inicial** | < 2 segundos | 🔴 Bloqueante |
| **Crashes en 30 min** | 0 crashes | 🔴 Bloqueante |
| **Tiempo preload** | < 300ms | 🟡 Alta |

---

## Configuración de Pruebas

### Configuración Base

```kotlin
val testConfig = MediastreamPlayerConfig().apply {
    playerId = "TEST_PLAYER_ID_REELS"
    id = "TEST_INITIAL_VIDEO_ID"
    type = MediastreamPlayerConfig.VideoTypes.VOD
    environment = MediastreamPlayerConfig.Environment.DEV
    autoplay = true
    isDebug = true
    trackEnable = true
}
```

### Dispositivos Mínimos Requeridos

| Tier | Dispositivo Ejemplo | Android | RAM |
|------|---------------------|---------|-----|
| **Alto** | Samsung S23, Pixel 7 | 13+ | 8GB |
| **Medio** | Samsung A54, Redmi Note 11 | 11+ | 4GB |
| **Bajo** | Samsung A13, Moto G Power | 10+ | 3GB |

### Condiciones de Red

- **WiFi**: 50+ Mbps (testing principal)
- **4G**: 10-20 Mbps (testing secundario)
- **Sin conexión**: Offline mode (error handling)

---

## Test Cases Críticos

### TC-REELS-001: Inicialización Básica

**Prioridad**: 🔴 Crítica  
**Tipo**: Funcional  
**Tiempo estimado**: 2 minutos

#### Precondiciones
- App instalada
- Internet disponible
- Player ID válido configurado

#### Pasos
1. Abrir app
2. Navegar a sección Reels
3. Observar carga inicial

#### Resultado Esperado
- ✅ ViewPager2 se renderiza
- ✅ Primer video se carga
- ✅ Autoplay inicia en < 2 segundos
- ✅ UI overlay visible
- ✅ Loading indicator desaparece al iniciar

#### Criterios de Fallo
- ❌ ViewPager no se renderiza
- ❌ Video no carga
- ❌ Error mostrado
- ❌ Crash de la app

---

### TC-REELS-002: Scroll Vertical Básico

**Prioridad**: 🔴 Crítica  
**Tipo**: Funcional  
**Tiempo estimado**: 3 minutos

#### Precondiciones
- TC-REELS-001 pasado
- Al menos 3 videos cargados

#### Pasos
1. Ver primer video (posición 0)
2. Hacer swipe up (deslizar hacia arriba)
3. Observar transición
4. Verificar autoplay del siguiente video
5. Repetir para posiciones 1 → 2
6. Hacer swipe down (deslizar hacia abajo)
7. Verificar retorno a posición anterior

#### Resultado Esperado
- ✅ Transición suave (≥55 FPS)
- ✅ Video anterior se pausa
- ✅ Video siguiente inicia automáticamente
- ✅ No hay lag ni stuttering
- ✅ UI actualiza correctamente
- ✅ Progress bar se resetea

#### Criterios de Fallo
- ❌ Lag visible (< 30 FPS)
- ❌ Video anterior no se pausa
- ❌ Autoplay no funciona
- ❌ Scroll se atasca
- ❌ UI no actualiza

---

### TC-REELS-003: Preload de Contenido

**Prioridad**: 🔴 Crítica  
**Tipo**: Performance  
**Tiempo estimado**: 5 minutos

#### Precondiciones
- TC-REELS-002 pasado
- Configuración preload=2, keepInMemory=2

#### Pasos
1. Iniciar en posición 0
2. Verificar logs de preload
3. Avanzar a posición 1
4. Verificar que posición 2 y 3 se precargan
5. Avanzar a posición 2
6. Verificar tiempo de inicio (debe ser instantáneo)

#### Resultado Esperado
- ✅ Posiciones +1 y +2 precargan en background
- ✅ Logs muestran "Preloading position=X"
- ✅ Videos precargados inician sin delay
- ✅ No hay buffering al llegar a video precargado

#### Mediciones
- Tiempo de inicio video precargado: **< 300ms**
- Tiempo de inicio video NO precargado: **< 2s**

#### Criterios de Fallo
- ❌ Videos precargados no inician rápido
- ❌ Buffering en videos precargados
- ❌ No hay logs de preload

**Cómo verificar logs:**
```bash
adb logcat | grep -i "preload"
```

---

### TC-REELS-004: Gestión de Memoria

**Prioridad**: 🔴 Crítica  
**Tipo**: Performance  
**Tiempo estimado**: 10 minutos

#### Precondiciones
- TC-REELS-001 pasado
- Android Monitor o Profiler conectado

#### Pasos
1. Iniciar app y abrir Reels
2. Medir uso de memoria inicial
3. Navegar por 20 reels consecutivos
4. Medir uso de memoria después
5. Volver atrás 10 posiciones
6. Medir uso de memoria
7. Continuar por 10 reels más
8. Medir uso de memoria final

#### Resultado Esperado
- ✅ Memoria inicial: < 150MB
- ✅ Memoria después de 20 reels: < 250MB
- ✅ Memoria después de 30 reels: < 300MB
- ✅ No crece indefinidamente
- ✅ Garbage collection efectivo
- ✅ No memory leaks

#### Mediciones
| Métrica | Límite Esperado |
|---------|-----------------|
| Memoria inicial | < 150MB |
| Memoria en uso estable | < 250MB |
| Memoria máxima | < 350MB |
| Crecimiento por reel | < 5MB |

#### Criterios de Fallo
- ❌ Memoria > 400MB
- ❌ OutOfMemoryError
- ❌ Crecimiento constante sin estabilización

**Cómo verificar:**
```bash
# Monitorear memoria en tiempo real
adb shell dumpsys meminfo <package> | grep TOTAL

# Profiler en Android Studio
# View > Tool Windows > Profiler > Memory
```

---

### TC-REELS-005: Reproducción con Anuncios

**Prioridad**: 🔴 Crítica  
**Tipo**: Funcional  
**Tiempo estimado**: 5 minutos

#### Precondiciones
- Ads config habilitado
- Frequency configurado (ej: 3)
- VAST URL válida

#### Pasos
1. Ver 3 reels orgánicos (posiciones 0, 1, 2)
2. Verificar que posición 3 es un anuncio
3. Ver anuncio completo
4. Verificar auto-avance al terminar
5. Continuar por 3 reels más
6. Verificar siguiente anuncio

#### Resultado Esperado
- ✅ Anuncio aparece en frecuencia correcta
- ✅ Badge "Anuncio" visible
- ✅ Video de anuncio se reproduce
- ✅ Auto-avance después de 300ms
- ✅ Siguiente reel orgánico inicia
- ✅ Frecuencia se mantiene consistente

#### Criterios de Fallo
- ❌ Anuncios no aparecen
- ❌ Frecuencia incorrecta
- ❌ No hay auto-avance
- ❌ Crash al reproducir anuncio
- ❌ Anuncio no se puede saltar después del tiempo mínimo

---

### TC-REELS-006: Estado de Mute Persistente

**Prioridad**: 🟡 Alta  
**Tipo**: Funcional  
**Tiempo estimado**: 3 minutos

#### Precondiciones
- TC-REELS-001 pasado
- Audio inicialmente activado

#### Pasos
1. Verificar audio activo (icono 🔊)
2. Tocar botón de mute
3. Verificar cambio de icono a 🔇
4. Avanzar a siguiente reel
5. Verificar que audio sigue muted
6. Cerrar app completamente
7. Reabrir app y volver a Reels
8. Verificar estado de mute persiste

#### Resultado Esperado
- ✅ Botón cambia icono correctamente
- ✅ Audio se mutea/desmutea
- ✅ Estado persiste entre reels
- ✅ Estado persiste entre sesiones
- ✅ SharedPreferences guarda estado

#### Criterios de Fallo
- ❌ Estado no persiste entre reels
- ❌ Estado se resetea al reabrir app
- ❌ Icono no actualiza
- ❌ Audio no mutea/desmutea

**Verificar SharedPreferences:**
```bash
adb shell run-as <package> cat shared_prefs/reels_prefs.xml | grep is_muted
```

---

### TC-REELS-007: Manejo de Errores de Red

**Prioridad**: 🟡 Alta  
**Tipo**: Manejo de Errores  
**Tiempo estimado**: 5 minutos

#### Precondiciones
- TC-REELS-002 pasado
- Proxy/herramienta para simular errores de red

#### Pasos
1. Iniciar reproducción en posición 0
2. Durante reproducción, deshabilitar internet
3. Verificar comportamiento
4. Avanzar a siguiente reel
5. Verificar comportamiento sin internet
6. Reactivar internet
7. Verificar recuperación

#### Resultado Esperado
- ✅ Error loggeado pero no crash
- ✅ Mensaje de error amigable (opcional)
- ✅ Auto-avance a siguiente reel después de 300ms
- ✅ Al reactivar internet, carga funciona
- ✅ Reels ya cargados se reproducen offline

#### Criterios de Fallo
- ❌ App crashea
- ❌ UI se congela
- ❌ No hay auto-avance en error
- ❌ No recupera al reactivar internet

**Simular error de red:**
```bash
# Deshabilitar WiFi desde ADB
adb shell svc wifi disable

# Habilitar WiFi
adb shell svc wifi enable

# O usar Charles Proxy con Throttling/Breakpoints
```

---

### TC-REELS-008: Lifecycle Management

**Prioridad**: 🟡 Alta  
**Tipo**: Funcional  
**Tiempo estimado**: 5 minutos

#### Precondiciones
- TC-REELS-001 pasado
- Video reproduciéndose

#### Pasos
1. Reproducir reel en posición 2
2. Presionar HOME (app a background)
3. Esperar 5 segundos
4. Volver a la app
5. Verificar estado
6. Presionar botón Recientes
7. Cambiar a otra app
8. Volver a Reels
9. Bloquear pantalla durante reproducción
10. Desbloquear pantalla

#### Resultado Esperado
- ✅ Al ir a background: video se pausa
- ✅ Al volver: video se puede reanudar
- ✅ Posición se mantiene
- ✅ No hay crash al cambiar de app
- ✅ Al bloquear: reproducción se pausa
- ✅ Al desbloquear: UI responde correctamente

#### Criterios de Fallo
- ❌ Crash al volver de background
- ❌ Video no reanuda
- ❌ Posición se pierde
- ❌ Memory leak
- ❌ Players no se liberan

---

### TC-REELS-009: FPS Durante Scroll

**Prioridad**: 🔴 Crítica  
**Tipo**: Performance  
**Tiempo estimado**: 3 minutos

#### Precondiciones
- TC-REELS-002 pasado
- FPS counter habilitado en Developer Options

#### Pasos
1. Habilitar FPS counter en dispositivo
2. Hacer 20 scrolls consecutivos (up y down)
3. Observar FPS en tiempo real
4. Verificar drops de frame

#### Resultado Esperado
- ✅ FPS promedio ≥ 55
- ✅ No drops < 30 FPS durante transiciones
- ✅ Scroll visualmente suave sin stuttering

#### Criterios de Fallo
- ❌ FPS < 30 durante scroll
- ❌ Lag visible o stuttering
- ❌ UI se congela

---

## Checklist de Regresión

### Pre-Release Checklist (Obligatorio)

**Tiempo total**: ~30 minutos

#### Test Cases Bloqueantes (Must Pass)

- [ ] TC-REELS-001: Inicialización básica (2 min)
- [ ] TC-REELS-002: Scroll vertical básico (3 min)
- [ ] TC-REELS-003: Preload de contenido (5 min)
- [ ] TC-REELS-004: Gestión de memoria (10 min)
- [ ] TC-REELS-005: Reproducción con anuncios (5 min)
- [ ] TC-REELS-007: Manejo de errores de red (5 min)
- [ ] TC-REELS-009: FPS durante scroll (3 min)

#### Test Cases Importantes (Should Pass)

- [ ] TC-REELS-006: Estado de mute persistente (3 min)
- [ ] TC-REELS-008: Lifecycle management (5 min)

#### Dispositivos Mínimos

- [ ] 1 dispositivo high-end (Android 13+)
- [ ] 1 dispositivo mid-range (Android 11+)
- [ ] 1 dispositivo low-end (Android 10+, RAM 3GB)

---

## Criterios de Aceptación

### Release Blocker (NO SE PUEDE LANZAR si falla)

| Criterio | Métrica | Estado |
|----------|---------|--------|
| Test cases críticos | 100% pasados | [ ] |
| FPS durante scroll | ≥ 55 FPS | [ ] |
| Memoria máxima | < 300MB | [ ] |
| Tiempo de carga | < 2 segundos | [ ] |
| Crashes | 0 en 30 minutos | [ ] |
| Compatibilidad Android | 10-14 funcionando | [ ] |

### Aprobación Final

**Sign-off requerido de:**
- [ ] QA Lead
- [ ] Tech Lead
- [ ] Product Owner

---

## Scripts de Testing

### Monitoreo de Memoria

```bash
# Monitor memoria en tiempo real
adb shell dumpsys meminfo am.mediastre.mediastreamsampleapp | grep TOTAL

# O usar Android Studio Profiler
# View > Tool Windows > Profiler > Memory
```

### Verificación de Logs

```bash
# Logs de preload
adb logcat | grep -i "preload"

# Logs de Reels
adb logcat | grep -E "Reels|ViewPager|ReelsContentManager"

# Logs de analytics
adb logcat | grep -i "ANALYTICSREEL"
```

### Simulación de Condiciones

```bash
# Deshabilitar WiFi
adb shell svc wifi disable

# Habilitar WiFi
adb shell svc wifi enable

# Verificar SharedPreferences
adb shell run-as am.mediastre.mediastreamsampleapp \
  cat shared_prefs/reels_prefs.xml | grep is_muted
```

---

## Resumen

**Test Cases Críticos**: 9  
**Tiempo de testing completo**: ~30 minutos  
**Dispositivos mínimos**: 3 (high/mid/low-end)  

**Criterios de éxito**:
- ✅ 100% de tests críticos pasados
- ✅ FPS ≥ 55 durante scroll
- ✅ Memoria < 300MB
- ✅ 0 crashes en 30 minutos
- ✅ Funciona en Android 10-14

---

**Preparado por**: Equipo de QA  
**Última actualización**: Febrero 2026
