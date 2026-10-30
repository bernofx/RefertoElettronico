package it.refertoelettronico

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.feedback.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToneGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ActionFeedbackTest {
    private lateinit var context:Context
    private lateinit var manager:AudioManager
    @Before fun initialize() {
        context=ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("feedback",Context.MODE_PRIVATE).edit().clear().commit()
        manager=context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        manager.ringerMode=AudioManager.RINGER_MODE_NORMAL
        ShadowToneGenerator.reset()
    }
    @Test fun pointAndUndoHaveDifferentShortConfirmationSounds() {
        ActionFeedback(context).use { f ->f.play(Cue.POINT);f.play(Cue.UNDO) }
        val tones=ShadowToneGenerator.getPlayedTones()
        assertEquals(2,tones.size)
        assertNotEquals(tones[0].type(),tones[1].type())
        assertEquals(70,tones[0].duration().toMillis().toInt())
        assertTrue(tones.all{it.duration().toMillis() in 1..200})
    }
    @Test fun mutePersistsAndNoFurtherActionPlaysUntilItIsReenabled() {
        ActionFeedback(context).use { f ->f.setEnabled(false);Cue.entries.forEach{f.play(it)} }
        assertTrue(ShadowToneGenerator.getPlayedTones().isEmpty())
        ActionFeedback(context).use { f ->
            assertFalse(f.enabled);f.play(Cue.POINT)
            assertTrue(ShadowToneGenerator.getPlayedTones().isEmpty())
            f.setEnabled(true);assertTrue(f.enabled)
            assertEquals(1,ShadowToneGenerator.getPlayedTones().size)
            f.play(Cue.POINT);assertEquals(2,ShadowToneGenerator.getPlayedTones().size)
        }
    }
    @Test fun silentAndVibrateModesAreRespected() {
        ActionFeedback(context).use { f ->
            manager.ringerMode=AudioManager.RINGER_MODE_SILENT;f.play(Cue.POINT)
            manager.ringerMode=AudioManager.RINGER_MODE_VIBRATE;f.play(Cue.TIMEOUT)
            assertTrue(ShadowToneGenerator.getPlayedTones().isEmpty())
            manager.ringerMode=AudioManager.RINGER_MODE_NORMAL;f.play(Cue.CONFIRM)
            assertEquals(1,ShadowToneGenerator.getPlayedTones().size)
        }
    }
}
