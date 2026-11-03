package it.refertoelettronico

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.refertoelettronico.storage.MatchStore
import it.refertoelettronico.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppGuideTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun next() {
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("guide-next").fetchSemanticsNodes().singleOrNull()?.config?.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)==false &&
                compose.onAllNodesWithText("Preparazione dell’esempio…").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithTag("guide-next").assertIsEnabled().performClick()
    }
    @Test fun firstLaunchTutorialCanGoBackFinishAndReopenButDoesNotRepeatAfterRestart() {
        val progress=GuideProgress(compose.activity)
        assertTrue(progress.needsIntroduction)
        compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[0].title)
        next()
        compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[1].title)
        compose.onNodeWithTag("guide-previous").performClick()
        compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[0].title)
        compose.runOnIdle {
            val view=org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView
            val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            val dir=File("build/outputs/verification").apply{mkdirs()}
            File(dir,"ui-tutorial-0.8.0.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
            val launcher=compose.activity.getDrawable(R.mipmap.ic_launcher)!!
            val iconBitmap=android.graphics.Bitmap.createBitmap(432,432,android.graphics.Bitmap.Config.ARGB_8888)
            launcher.setBounds(0,0,432,432);launcher.draw(android.graphics.Canvas(iconBitmap))
            File(dir,"launcher-0.8.0.png").outputStream().use{iconBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        }
        repeat(AppGuide.steps.size){ i ->
            compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[i].title)
            if(AppGuide.steps[i].target=="pdf-preview") {
                compose.waitUntil(30_000){compose.onAllNodesWithContentDescription("Anteprima prima pagina del referto compilato").fetchSemanticsNodes().isNotEmpty()}
                assertTrue(File(compose.activity.cacheDir,".").walkTopDown().any{it.extension=="pdf"})
            }
            if(AppGuide.steps[i].target in listOf("court","operation-form","pdf-preview")) {
                compose.waitUntil(30_000){compose.onAllNodesWithText("Preparazione dell’esempio…").fetchSemanticsNodes().isEmpty()}
                compose.runOnIdle {
                    val view=org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView
                    val image=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
                    view.draw(android.graphics.Canvas(image))
                    val dir=File("build/outputs/verification").apply{mkdirs()}
                    File(dir,"tutorial-0.8.0-step-$i.png").outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
                    if(AppGuide.steps[i].target=="pdf-preview") {
                        val pdf=compose.activity.cacheDir.walkTopDown().single{it.extension=="pdf"}
                        pdf.copyTo(File(dir,"tutorial-referto.pdf"),overwrite=true)
                        com.tom_roush.pdfbox.pdmodel.PDDocument.load(pdf).use { document ->
                            assertTrue(document.numberOfPages>=2)
                            val text=com.tom_roush.pdfbox.text.PDFTextStripper().getText(document)
                            assertTrue(text.contains("TUTORIAL"))
                            assertTrue(text.contains("DATI FITTIZI"))
                            assertTrue(text.contains("Stella Como"))
                        }
                    }
                }
            }
            next()
        }
        compose.onNodeWithTag("guide-close").assertDoesNotExist()
        assertFalse(progress.needsIntroduction)
        assertFalse(compose.activity.cacheDir.listFiles().orEmpty().any{it.name.startsWith("tutorial-")})
        assertFalse(File(compose.activity.filesDir,"exports").exists())
        assertTrue(MatchStore(File(compose.activity.filesDir,"matches")).list().isEmpty())
        compose.mainClock.autoAdvance=false
        compose.activityRule.scenario.recreate()
        compose.mainClock.autoAdvance=true
        compose.onNodeWithTag("guide-close").assertDoesNotExist()
        compose.onNodeWithTag("home-guide").performClick()
        compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[0].title)
        compose.onNodeWithTag("guide-close").performClick()
        assertFalse(GuideProgress(compose.activity).needsIntroduction)
    }
    @Test @Config(sdk=[35],qualifiers="w320dp-h568dp-xhdpi")
    fun compactScreenScrollsToOffscreenControlsAndAlwaysAllowsExit() {
        repeat(16){index ->
            compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[index].title)
            next()
        }
        compose.onNodeWithTag("guide-close").assertIsDisplayed().performClick()
        assertTrue(MatchStore(File(compose.activity.filesDir,"matches")).list().isEmpty())
    }
    @Test fun guideTabAndManualOverlayNeverModifyAnOngoingMatch() {
        compose.onNodeWithTag("guide-close").performClick()
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithTag("point-button-0").performClick()
        val store=MatchStore(File(compose.activity.filesDir,"matches"))
        val before=store.list().single().let{it.first to it.second.getOrThrow()}
        compose.onNodeWithText("Guida",substring=false).performClick()
        compose.onNodeWithText("Guida a Referto Volley").assertExists()
        compose.onNodeWithTag("guide-restart").performClick()
        next()
        compose.onNodeWithTag("guide-close").performClick()
        compose.onNodeWithText("Gara",substring=false).performClick()
        compose.onNodeWithTag("score-0").assertTextEquals("1")
        compose.onNodeWithText("Dati",substring=false).performClick()
        compose.onNodeWithText("Modifica i dati della gara").performClick()
        compose.onNodeWithTag("editor-guide").performClick()
        compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[0].title)
        compose.onNodeWithTag("guide-close").performClick()
        assertEquals(before.second,store.load(before.first))
        assertEquals(1,store.list().size)
    }
}
