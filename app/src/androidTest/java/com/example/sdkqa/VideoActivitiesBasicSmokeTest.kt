package com.example.sdkqa

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.sdkqa.video.VideoEpisodeActivity
import com.example.sdkqa.video.VideoEpisodeAutomaticActivity
import com.example.sdkqa.video.VideoEpisodeManualActivity
import com.example.sdkqa.video.VideoLiveActivity
import com.example.sdkqa.video.VideoLiveDvrActivity
import com.example.sdkqa.video.VideoLiveWithScrollActivity
import com.example.sdkqa.video.VideoLocalActivity
import com.example.sdkqa.video.VideoLocalWithServiceActivity
import com.example.sdkqa.video.VideoMixedActivity
import com.example.sdkqa.video.VideoMixedWithServiceActivity
import com.example.sdkqa.video.VideoPipActivity
import com.example.sdkqa.video.VideoReelActivity
import com.example.sdkqa.video.VideoVodPipActivity
import com.example.sdkqa.video.VideoVodSimpleActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoActivitiesBasicSmokeTest {

    @get:Rule
    val notificationsRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test fun videoVodSimple_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoVodSimpleActivity::class.java, prefix = "VideoVodSimple")

    @Test fun videoVodPip_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoVodPipActivity::class.java, prefix = "VideoVodPip")

    @Test fun videoLocal_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoLocalActivity::class.java, prefix = "VideoLocal")

    @Test fun videoLocalWithService_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoLocalWithServiceActivity::class.java, prefix = "VideoLocalWithService")

    @Test fun videoEpisode_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoEpisodeActivity::class.java, prefix = "VideoEpisode")

    @Test fun videoLive_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoLiveActivity::class.java, prefix = "VideoLive")

    @Test fun videoLiveDvr_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoLiveDvrActivity::class.java, prefix = "VideoLiveDvr")

    @Test fun videoMixed_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoMixedActivity::class.java, prefix = "VideoMixed")

    @Test fun videoMixedWithService_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoMixedWithServiceActivity::class.java, prefix = "VideoMixedWithService")

    @Test fun videoLivePip_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoPipActivity::class.java, prefix = "VideoPip")

    @Test fun videoReels_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoReelActivity::class.java, prefix = "VideoReel")

    @Test fun videoLiveWithScroll_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoLiveWithScrollActivity::class.java, prefix = "VideoLiveWithScroll")

    @Test fun videoEpisodeManual_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoEpisodeManualActivity::class.java, prefix = "VideoEpisodeManual")

    @Test fun videoEpisodeAuto_basic_events() =
        SmokeTestUtils.launchAndAssertBasicEvents(VideoEpisodeAutomaticActivity::class.java, prefix = "VideoEpisodeAuto")
}

