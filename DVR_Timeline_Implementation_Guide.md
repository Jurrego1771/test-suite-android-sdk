# Definition Document: DVR and Fictitious Timeline Implementation

## Executive Summary

A complete solution has been implemented for the live-DVR flow that resolves UX issues related to temporal navigation in live streams. The implementation includes a fictitious timeline that represents the complete DVR window from the start, significantly improving the user experience.

## Problems Solved

1. **Inconsistent Timeline**: The timeline was resetting when switching between live and DVR
2. **Lost Position**: The user lost their target position during DVR transition
3. **Limited Duration**: The timeline showed only the buffer duration (1-2 minutes) instead of the complete DVR window
4. **Buffer Override**: ExoPlayer was overwriting the fictitious timeline when the buffer updated

## Solution Architecture

### Key State Variables

```kotlin
// Main DVR variables
private var isDvrMode = false
private var dvrStartParameter: String? = null
private var liveDvrUrl: String? = null
private var isTimelineInteractionEnabled = false
private var isUserScrubbing = false

// Fictitious timeline variables
private var fictitiousTimelineDuration: Long = 0L
private var isUsingFictitiousTimeline = false

// DVR position tracking variables
private var targetDvrPosition: Long = 0L
private var isWaitingForDvrSeek = false
```

### Operation Flow

#### 1. Initialization
- **When**: During `customizeUi()` and initial configuration
- **Action**: Fictitious timeline is initialized with DVR window duration
- **Data source**: Local configuration (`windowDvr`) or API data (`dvrWindowOffset`)

#### 2. Timeline Configuration
- **When**: In `setupDvrTimelineInteraction()`
- **Action**: Immediate timeline configuration is forced with fictitious duration
- **Result**: Progress bar shows complete DVR window from the start

#### 3. User Interaction
- **When**: User scrubs on the timeline
- **Action**: Target position is captured and percentage is calculated in fictitious timeline
- **Calculation**: `percentage = position / fictitiousTimelineDuration`

#### 4. DVR Activation
- **When**: User releases scrub more than 5 seconds back
- **Action**: DVR URL is built with time parameters and switches to DVR stream
- **Seek**: Stored target position is applied

#### 5. Buffer Protection
- **When**: Continuously during playback
- **Action**: Multiple protection layers to maintain fictitious timeline
- **Methods**: Listeners, periodic checks, automatic restorations

## Technical Implementation

### 1. Fictitious Timeline Initialization

```kotlin
private fun initializeFictitiousTimeline() {
    val hasDvrConfig = msConfig?.type == VideoTypes.LIVE && msConfig?.dvr == true

    if (hasDvrConfig) {
        val windowDuration = if (dvrWindowDuration > 0) {
            dvrWindowDuration
        } else {
            val configWindowDvr = msConfig?.windowDvr?.toInt() ?: 0
            if (configWindowDvr > 0) {
                configWindowDvr * 60 // Convert minutes to seconds
            } else {
                0
            }
        }

        if (windowDuration > 0) {
            fictitiousTimelineDuration = (windowDuration * 1000L)
            isUsingFictitiousTimeline = true
        }
    }
}
```

### 2. DVR Position Calculation

```kotlin
private fun handleDvrScrubStop(position: Long) {
    if (isUsingFictitiousTimeline && fictitiousTimelineDuration > 0) {
        // Calculate percentage of fictitious timeline
        val percentage = position.toFloat() / fictitiousTimelineDuration.toFloat()

        // Calculate seek back time based on DVR window duration
        val seekBackSeconds = (dvrWindowDuration * (1.0f - percentage)).toLong()

        // Store target position for later use
        targetDvrPosition = (dvrWindowDuration * 1000L * percentage).toLong()
        isWaitingForDvrSeek = true
        enableDvrMode(seekBackSeconds)
    }
}
```

### 3. Buffer Override Protection

```kotlin
override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    super.onTimelineChanged(timeline, reason)

    if (msConfig?.type == VideoTypes.LIVE && msConfig?.dvr == true &&
        !isDvrMode && isUsingFictitiousTimeline) {
        Handler(Looper.getMainLooper()).postDelayed({
            forceRestoreFictitiousTimeline()
        }, 100)
    }
}

private fun forceRestoreFictitiousTimeline() {
    if (!isDvrMode && isUsingFictitiousTimeline && fictitiousTimelineDuration > 0) {
        val defaultTimeBar: DefaultTimeBar? = msplayerView?.findViewById(androidx.media3.ui.R.id.exo_progress)
        defaultTimeBar?.setDuration(fictitiousTimelineDuration)
        defaultTimeBar?.setPosition(fictitiousTimelineDuration)
    }
}
```

### 4. Timeline Visual Control

```kotlin
private fun forceSeekbarPosition(timeBar: DefaultTimeBar?) {
    val forcePositionRunnable = object : Runnable {
        override fun run() {
            if (!isUserScrubbing && !isTimelineInteractionEnabled) {
                if (isDvrMode) {
                    // DVR mode: Normal ExoPlayer handling
                    val currentPosition = msPlayer?.currentPosition ?: 0L
                    val duration = msPlayer?.duration ?: 1000L
                    timeBar?.setPosition(currentPosition)
                    timeBar?.setDuration(duration)
                } else {
                    // Live mode: Use fictitious timeline
                    if (isUsingFictitiousTimeline && fictitiousTimelineDuration > 0) {
                        timeBar?.setDuration(fictitiousTimelineDuration)
                        timeBar?.setPosition(fictitiousTimelineDuration)
                    }
                }
            }
            forcePositionHandler.postDelayed(this, 500)
        }
    }
}
```

## Configuration and Parameters

### Configuration Parameters

- `msConfig.dvr`: Enables/disables DVR functionality
- `msConfig.windowDvr`: DVR window duration in minutes (local configuration)
- `msConfig.dvrStart`: DVR start timestamp (ISO format)
- `msConfig.dvrEnd`: DVR end timestamp (ISO format)

### API Parameters

- `mediaInfo.dvrWindowOffset.live_window_time`: DVR window duration from API
- `mediaInfo.dvrWindowOffset.account_window_time`: Account DVR window duration

### Configuration Priority

1. **API data**: `live_window_time` > `account_window_time`
2. **Local config**: `windowDvr` (only if ≤ API value)
3. **Fallback**: No DVR if no valid configuration

## Use Cases and Flows

### Case 1: Live → DVR → Live
1. User loads live stream
2. Timeline shows complete DVR window (e.g., 1 hour)
3. User scrubs to 20% of timeline
4. System calculates: 48 minutes back (60 - 12)
5. DVR mode is activated with specific URL
6. Player positions at minute 12 of DVR window
7. User can navigate freely in DVR
8. Clicking "LIVE" returns to live stream

### Case 2: Fictitious Timeline vs Buffer
1. **Problem**: Buffer shows 1-2 minutes, fictitious timeline shows 1 hour
2. **Solution**: Fictitious timeline always prevails over buffer
3. **Protection**: Multiple layers of automatic restoration

### Case 3: Buffer Update
1. **Problem**: Buffer updates and overwrites timeline
2. **Solution**: `onTimelineChanged` restores fictitious timeline
3. **Backup**: Checks every 500ms maintain timeline

## Key Points for the Team

### 1. State Management
- **Critical**: Always verify `isDvrMode` before applying changes
- **Important**: Reset variables when scrub is canceled or returning to live
- **Recommended**: Use debug logs to track state changes

### 2. Timeline Synchronization
- **Critical**: `fictitiousTimelineDuration` must be consistent
- **Important**: Verify that `isUsingFictitiousTimeline` is synchronized
- **Recommended**: Validate duration before applying changes

### 3. Error Handling
- **Critical**: Fallback to standard timeline if DVR fails
- **Important**: Clean variables in case of error
- **Recommended**: Detailed logs for debugging

### 4. Performance
- **Critical**: `forceSeekbarPosition` runs every 500ms
- **Important**: Verify it doesn't cause lag on slow devices
- **Recommended**: Consider adjusting interval based on performance

## Testing and Validation

### Critical Test Cases

1. **Initial Fictitious Timeline**
   - Verify timeline shows complete DVR duration when loading
   - Confirm it's not affected by initial buffer

2. **Live → DVR Transition**
   - Test scrub at different positions (10%, 50%, 90%)
   - Verify final position is correct
   - Confirm DVR URL is built correctly

3. **Buffer Protection**
   - Let stream run for several minutes
   - Verify timeline maintains DVR duration
   - Confirm it doesn't reset to buffer duration

4. **DVR → Live Transition**
   - Test "LIVE" button from different DVR positions
   - Verify variables are cleaned correctly
   - Confirm timeline returns to live edge

5. **Scrub Cancellation**
   - Test canceling scrub (touching outside timeline)
   - Verify variables are reset
   - Confirm DVR mode is not activated

### Validation Metrics

- **Timeline Consistency**: Timeline must maintain DVR duration 100% of the time
- **Position Accuracy**: Final position must be within ±2 seconds of target
- **State Management**: Variables must be clean in all cases
- **Performance**: No perceptible lag in UI

## Logs and Debugging

### Key Logs for Monitoring

```kotlin
// Initialization
Log.d(TAG, "DVR: Initialized fictitious timeline with duration: ${fictitiousTimelineDuration}ms")

// State changes
Log.d(TAG, "DVR: Timeline changed - restored fictitious duration: ${fictitiousTimelineDuration}ms")

// Position calculations
Log.d(TAG, "DVR: Fictitious timeline - percentage: ${percentage * 100}%, seekBack: ${seekBackSeconds}s")

// Restorations
Log.d(TAG, "DVR: Force restored fictitious timeline - duration: ${fictitiousTimelineDuration}ms")
```

### Variables for Debugging

- `isDvrMode`: Current DVR mode state
- `isUsingFictitiousTimeline`: Whether fictitious timeline is being used
- `fictitiousTimelineDuration`: Current fictitious timeline duration
- `targetDvrPosition`: Stored target position
- `isWaitingForDvrSeek`: Whether waiting to apply seek

## Future Considerations

### Potential Improvements

1. **Visual Timeline**: Add visual indicators of available DVR window
2. **Caching**: Cache frequent DVR positions for better performance
3. **Adaptive Timeline**: Adjust duration based on actual DVR availability
4. **Analytics**: DVR usage tracking for optimization

### Possible Extensions

1. **Multi-window DVR**: Support for multiple DVR windows
2. **DVR Bookmarks**: Bookmarks in DVR timeline
3. **DVR Quality**: Different qualities for DVR vs live
4. **Offline DVR**: Download DVR content for offline playback

## Conclusion

This implementation provides a smooth and consistent user experience for DVR navigation in live streams. The fictitious timeline resolves the identified UX issues and the protection architecture ensures long-term stability.

The development team should focus on:
1. Validating all critical use cases
2. Monitoring performance on different devices
3. Verifying error handling and edge cases
4. Considering improvements based on user feedback

---

**Document prepared for development team review**
**Date**: [Current Date]
**Version**: 1.0
**Status**: Implemented and in testing
