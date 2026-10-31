package it.refertoelettronico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.model.*
import it.refertoelettronico.storage.RosterStore
import it.refertoelettronico.ui.TeamEditor
import it.refertoelettronico.ui.VolleyTheme
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
class RosterEditorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun fullRosterCanBeSavedAndLoadedWithNewAttendanceAndStableEditableRows() {
        val members=(1..16).map{RosterMember("member-$it",Player(it,"Atleta $it",captain=it==1),it<=6)}
        var team by mutableStateOf(Rosters.withMembers(Team("Rosa prova",emptyList()),members))
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        val store=RosterStore(File(context.filesDir,"rosters"))
        var rootView:android.view.View?=null
        compose.setContent{rootView=LocalView.current.rootView;VolleyTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){TeamEditor(team,0,"2026/27",false,{team=it})}}}
        compose.onNodeWithText("Salva rosa",substring=false).performScrollTo().performClick()
        compose.waitUntil(10000){
            compose.onAllNodesWithTag("save-roster-confirm").fetchSemanticsNodes().any { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        }
        compose.onNodeWithTag("save-roster-confirm").performClick()
        compose.waitUntil(10000){
            store.list().size==1 && compose.onAllNodesWithText("Rosa salvata sul dispositivo").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(16,store.list().single().second.getOrThrow().team.players.size)
        compose.runOnIdle{team=Team("Nuova gara",emptyList())}
        compose.onNodeWithText("Carica rosa").performScrollTo().performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("Rosa prova · 2026/27 · 16 giocatrici").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Rosa prova · 2026/27 · 16 giocatrici").performClick()
        compose.onNodeWithTag("attendance-count").assertTextContains("0/14",substring=true)
        compose.runOnIdle{assertTrue(team.players.isEmpty());assertEquals(16,team.roster!!.size)}
        val outputs=File("build/outputs/verification").apply{mkdirs()}
        compose.runOnIdle {
            val view=rootView!!
            val image=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(outputs,"rosa-e-presenze.png").outputStream().use{image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        }
        val id=team.roster!![0].id
        compose.onNodeWithTag("presence-$id").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Modifica giocatrice 1").performScrollTo().performClick()
        compose.onNodeWithText("Cognome e nome").performScrollTo().performTextReplacement("Nome corretto")
        compose.runOnIdle{assertEquals("Nome corretto",team.players.single().name);assertEquals(id,team.roster!![0].id)}
    }
    @Test fun attendanceIsLockedAfterStartWhileLibrarySaveRemainsAvailable() {
        val team=Rosters.withMembers(Demo.config().teams[0],Rosters.members(Demo.config().teams[0]))
        compose.setContent{VolleyTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){TeamEditor(team,0,"2026/27",true,{error("Dati bloccati")})}}}
        compose.onNodeWithText("Carica rosa").assertIsNotEnabled()
        compose.onNodeWithTag("presence-${team.roster!!.first().id}").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Salva rosa").performScrollTo().assertIsEnabled()
    }
}
