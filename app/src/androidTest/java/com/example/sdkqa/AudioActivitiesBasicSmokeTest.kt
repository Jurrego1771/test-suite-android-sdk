package com.example.sdkqa

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.sdkqa.audio.AudioAodSimpleActivity
import com.example.sdkqa.audio.AudioAodWithServiceActivity
import com.example.sdkqa.audio.AudioEpisodeActivity
import com.example.sdkqa.audio.AudioLiveActivity
import com.example.sdkqa.audio.AudioLiveDvrActivity
import com.example.sdkqa.audio.AudioLiveWithServiceActivity
import com.example.sdkqa.audio.AudioLocalActivity
import com.example.sdkqa.audio.AudioLocalWithServiceActivity
import com.example.sdkqa.audio.AudioMixedActivity
import com.example.sdkqa.audio.AudioMixedWithServiceActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioActivitiesBasicSmokeTest {

    @get:Rule
    val notificationsRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test fun audioAodSimple_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioAodSimpleActivity::class.java, prefix = "AudioAodSimple")

    @Test fun audioAodWithService_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioAodWithServiceActivity::class.java, prefix = "AudioAodWithService")

    @Test fun audioEpisode_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioEpisodeActivity::class.java, prefix = "AudioEpisode")

    @Test fun audioLocal_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioLocalActivity::class.java, prefix = "AudioLocal")

    @Test fun audioLocalWithService_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioLocalWithServiceActivity::class.java, prefix = "AudioLocalWithService")

    @Test fun audioLive_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioLiveActivity::class.java, prefix = "AudioLive")

    @Test fun audioLiveWithService_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioLiveWithServiceActivity::class.java, prefix = "AudioLiveWithService")

    @Test fun audioLiveDvr_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioLiveDvrActivity::class.java, prefix = "AudioLiveDvr")

    @Test fun audioMixed_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioMixedActivity::class.java, prefix = "AudioMixed")

    @Test fun audioMixedWithService_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(AudioMixedWithServiceActivity::class.java, prefix = "AudioMixedWithService")
}

