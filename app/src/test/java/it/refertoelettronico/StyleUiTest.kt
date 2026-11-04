package it.refertoelettronico

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import it.refertoelettronico.model.*
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
class StyleUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun dashboardCanEditAndReturnWithoutLosingSignatures() {
        var journal by mutableStateOf(GuideExample.journal(11).let{it.copy(config=it.config.copy(location=""))})
        val original=journal.signatures
        compose.setContent{VolleyTheme(Appearance(layout=InterfaceStyle.DASHBOARD)){MatchEditor(journal.config,{journal=Signatures.withConfig(journal,it)},true)}}
        compose.onNodeWithTag("preparation-dashboard").assertIsDisplayed()
        compose.onNodeWithTag("dashboard-game").performScrollTo().performClick()
        compose.onNodeWithTag("setup-field-location").performScrollTo().performTextInput("Como")
        compose.onNodeWithTag("setup-overview").performClick()
        compose.onNodeWithTag("dashboard-save").performClick()
        assertEquals("Como",journal.config.location);assertEquals(original,journal.signatures)
    }
    @Test fun courtDockRecordsTheSelectedTeamAndUndoRestoresRotationAfterStyleSwitch() {
        var journal by mutableStateOf(Journal(config=Demo.config(),events=listOf(Demo.start())))
        var appearance by mutableStateOf(Appearance(layout=InterfaceStyle.COURT))
        val initial=journal
        compose.setContent{VolleyTheme(appearance){Surface{LiveScreen(journal.config,MatchEngine.replay(journal.config,journal.events),journal.events.lastOrNull(),
            {journal=MatchEngine.add(journal,it)},{journal=MatchEngine.undo(journal)},{journal=MatchEngine.redo(journal)},journal.events.isNotEmpty(),journal.redo.isNotEmpty(),{})}}}
        compose.onNodeWithTag("point-button-1").performClick()
        val rotated=MatchEngine.replay(journal.config,journal.events).current!!
        assertEquals(1,rotated.server);assertNotEquals(Demo.start().lineups[1],rotated.court[1])
        compose.onNodeWithTag("dock-team-1").performClick()
        compose.onNodeWithTag("dock-timeout").performClick()
        compose.onNodeWithText("Registra",substring=false).performClick()
        assertEquals(Kind.TIMEOUT,journal.events.last().kind);assertEquals(1,journal.events.last().team)
        compose.onNodeWithText("↶ Annulla",substring=false).performClick()
        compose.runOnIdle{appearance=appearance.copy(layout=InterfaceStyle.DASHBOARD)}
        compose.onNodeWithText("↶ Annulla",substring=false).performClick()
        assertEquals(initial.events,journal.events)
        assertEquals(MatchEngine.replay(initial.config,initial.events).current!!.court,MatchEngine.replay(journal.config,journal.events).current!!.court)
    }
    @Test fun guideCanReachRosterAndOperationsInDashboardStyle()=walkGuide(InterfaceStyle.DASHBOARD)
    @Test fun guideCanReachRosterAndOperationsInCourtStyle()=walkGuide(InterfaceStyle.COURT)
    private fun walkGuide(layout:InterfaceStyle) {
        compose.setContent{VolleyTheme(Appearance(layout=layout)){GuideOverlay({})}}
        repeat(20){index->
            compose.onNodeWithTag("guide-step-title").assertTextEquals(AppGuide.steps[index].title)
            compose.waitUntil(15000){compose.onAllNodesWithTag("guide-next").fetchSemanticsNodes().any{!it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)}}
            compose.onNodeWithTag("guide-next").performClick()
        }
        compose.onNodeWithTag("guide-close").performClick()
    }
    @Test fun renderBothStylesInLightAndDarkWithIndependentAccents() {
        var appearance by mutableStateOf(Appearance(layout=InterfaceStyle.DASHBOARD))
        var scene by mutableIntStateOf(0)
        val live=GuideExample.journal(2)
        var root:android.view.View?=null
        compose.setContent{root=LocalView.current.rootView;VolleyTheme(appearance){Surface(Modifier.fillMaxSize()){
            key(appearance.layout,scene){Column{
                PreparationHeader({},{},{})
                when(scene) {
                    0->MatchEditor(Demo.config().copy(officials=Demo.config().officials.with(0,"")),{},false)
                    1->LiveScreen(live.config,MatchEngine.replay(live.config,live.events),live.events.last(),{},{},{},true,false,{})
                    else->FinalSignatures(GuideExample.journal(11),{})
                }
            }}
        }}}
        for(layout in listOf(InterfaceStyle.DASHBOARD,InterfaceStyle.COURT))for(mode in listOf(ThemeMode.LIGHT,ThemeMode.DARK))for(screen in 0..2) {
            compose.runOnIdle{appearance=Appearance(ThemePalette.BLUE,mode,layout,StyleColor.BLUE,StyleColor.GOLD);scene=screen}
            compose.waitForIdle()
            compose.runOnIdle{
                val view=root!!;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
                val file=File("build/outputs/verification/style-${layout.name}-${mode.name}-$screen.png");file.parentFile!!.mkdirs()
                file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
        }
    }
}
