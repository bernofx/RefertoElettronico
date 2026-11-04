package it.refertoelettronico

import android.graphics.Bitmap
import android.graphics.Canvas
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
@Config(sdk=[35],qualifiers="w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StyleActivityTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun capture(name:String) {
        compose.runOnIdle {
            val modal=org.robolectric.shadows.ShadowDialog.getLatestDialog()
            val view=if(modal?.isShowing==true)modal.window!!.decorView else compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
            val path=File("build/outputs/verification/$name.png");path.parentFile!!.mkdirs()
            path.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
        }
    }
    private fun go(label:String) {
        if(compose.onAllNodesWithTag("style-menu").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("style-menu").performClick();compose.onNodeWithTag("style-menu-$label").performClick()
        } else compose.onNodeWithText(label,substring=false).performClick()
    }
    @Test fun realSettingsSwitchStylesAndRestoreEachProfileWithoutChangingTheOpenMatch() {
        compose.onNodeWithTag("guide-close").performClick()
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithTag("point-button-1").performClick()
        val store=MatchStore(File(compose.activity.filesDir,"matches"));val id=store.list().single().first;val match=store.load(id)
        go("Dati")
        compose.onNodeWithTag("appearance-open").performScrollTo().performClick()
        compose.onNodeWithTag("style-layout-DASHBOARD").performScrollTo().performClick()
        compose.onNodeWithTag("theme-mode-DARK").performScrollTo().performClick()
        compose.onNodeWithTag("style-customize").performScrollTo().performClick()
        compose.onNodeWithTag("style-primary-GREEN").performScrollTo().performClick()
        compose.onNodeWithTag("style-accent-ORANGE").performScrollTo().performClick()
        compose.onNodeWithTag("style-layout-COURT").performScrollTo().performClick()
        compose.onNodeWithTag("theme-mode-LIGHT").performScrollTo().performClick()
        compose.onNodeWithText("Fatto",substring=false).performClick()
        go("Gara")
        compose.onNodeWithTag("court-command-dock").assertIsDisplayed()
        capture("stile-C-activity-chiaro")
        assertEquals(match,store.load(id))
        compose.onNodeWithTag("dock-timeout").performClick()
        capture("stile-C-activity-pannello")
        compose.onNodeWithText("Chiudi",substring=false).performClick()
        go("Dati")
        compose.onNodeWithTag("appearance-open").performScrollTo().performClick()
        compose.onNodeWithTag("style-layout-DASHBOARD").performScrollTo().performClick()
        compose.onNodeWithText("Fatto",substring=false).performClick()
        val prefs=AppearanceStore(compose.activity).load()
        assertEquals(InterfaceStyle.DASHBOARD,prefs.layout);assertEquals(ThemeMode.DARK,prefs.mode)
        assertEquals(StyleColor.GREEN,prefs.primary);assertEquals(StyleColor.ORANGE,prefs.accent)
        assertEquals(ThemeMode.LIGHT,AppearanceStore(compose.activity).forLayout(InterfaceStyle.COURT).mode)
        go("Gara")
        capture("stile-B-activity-scuro")
        assertEquals(match,store.load(id))
        compose.activityRule.scenario.recreate()
        assertEquals(prefs,AppearanceStore(compose.activity).load())
        assertEquals(match,store.load(id))
    }
}
