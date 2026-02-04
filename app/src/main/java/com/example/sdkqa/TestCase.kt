package com.example.sdkqa

/**
 * Represents a test case item in the SDK QA test suite.
 * Easy to extend - just add new entries to TestCaseType.
 */
data class TestCase(
    val type: TestCaseType,
    val title: String,
    val category: Category
) {
    enum class Category(val displayName: String) {
        AUDIO("Audio"),
        VIDEO("Video")
    }

    enum class TestCaseType {
        AUDIO_AOD_SIMPLE,
        AUDIO_AOD_WITH_SERVICE,
        AUDIO_EPISODE,
        AUDIO_LOCAL,
        AUDIO_LOCAL_WITH_SERVICE,
        AUDIO_LIVE,
        AUDIO_LIVE_WITH_SERVICE,
        AUDIO_LIVE_DVR,
        AUDIO_MIXED,
        AUDIO_MIXED_WITH_SERVICE,
        VIDEO_VOD_SIMPLE,
        VIDEO_LOCAL,
        VIDEO_LOCAL_WITH_SERVICE,
        VIDEO_EPISODE,
        VIDEO_LIVE,
        VIDEO_LIVE_DVR,
        VIDEO_MIXED,
        VIDEO_MIXED_WITH_SERVICE,
        VIDEO_LIVE_PIP,
        VIDEO_REELS,
        VIDEO_LIVE_SCROLL,
        VIDEO_EPISODE_MANUAL,
        VIDEO_EPISODE_AUTO,
        VIDEO_VOD_PIP
    }

    companion object {
        /**
         * Returns all available test cases.
         * Add new test cases here.
         */
        fun getAllTestCases(): List<TestCase> = listOf(
            // Audio Test Cases
            TestCase(TestCaseType.AUDIO_AOD_SIMPLE, "AOD Simple", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_AOD_WITH_SERVICE, "AOD with Service", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_EPISODE, "Episode", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_LOCAL, "Local Audio", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_LOCAL_WITH_SERVICE, "Local Audio with Service", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_LIVE, "Live Audio", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_LIVE_WITH_SERVICE, "Live Audio with Service", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_LIVE_DVR, "Live Audio DVR", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_MIXED, "Mixed Audio", Category.AUDIO),
            TestCase(TestCaseType.AUDIO_MIXED_WITH_SERVICE, "Mixed Audio with Service", Category.AUDIO),
            // Video Test Cases
            TestCase(TestCaseType.VIDEO_VOD_SIMPLE, "VOD Simple", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_VOD_PIP, "VOD with PiP", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_LOCAL, "Local Video", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_LOCAL_WITH_SERVICE, "Local Video with Service", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_EPISODE, "Episode", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_LIVE, "Live Video", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_LIVE_DVR, "Live Video DVR", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_MIXED, "Mixed Video", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_MIXED_WITH_SERVICE, "Mixed Video with Service", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_LIVE_PIP, "Live Video with PiP", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_REELS, "Video Reels", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_LIVE_SCROLL, "Live with Scroll (PiP)", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_EPISODE_MANUAL, "Next Episode (Manual)", Category.VIDEO),
            TestCase(TestCaseType.VIDEO_EPISODE_AUTO, "Next Episode (Auto API)", Category.VIDEO)
        )
    }

    /**
     * Returns the formatted display title with category prefix.
     */
    fun getDisplayTitle(): String = "${category.displayName}: $title"
}
