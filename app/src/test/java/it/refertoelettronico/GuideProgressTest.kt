package it.refertoelettronico

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class GuideProgressTest {
    @Test fun unseenOrOlderGuideRevisionRequiresIntroductionButSeenAndNewerDoNot() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("app-guide",Context.MODE_PRIVATE)
        val progress=GuideProgress(context)
        prefs.edit().clear().commit()
        assertTrue(progress.needsIntroduction)
        progress.markSeen();assertFalse(GuideProgress(context).needsIntroduction)
        prefs.edit().putInt("seen-revision",AppGuide.revision-1).commit()
        assertTrue(GuideProgress(context).needsIntroduction)
        prefs.edit().putInt("seen-revision",AppGuide.revision+1).commit()
        assertFalse(GuideProgress(context).needsIntroduction)
    }
}
