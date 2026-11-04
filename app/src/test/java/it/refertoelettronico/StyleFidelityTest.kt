package it.refertoelettronico

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*
import it.refertoelettronico.storage.*
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
class StyleFidelityTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val numbers=listOf(17,21,25,29,31,33,44,55,66,67,68,69)
    private fun config()=Demo.config().copy(matchNumber="ANTEPRIMA",date="04/11/2026",teams=Demo.config().teams.mapIndexed{t,team->
        team.copy(name=if(t==0)"CDA E CAVALLASCA ASD"else"POLISPORTIVA INTERCOMUNALE",colorKey=if(t==0)"BLUE"else"YELLOW",
            players=numbers.mapIndexed{i,n->Player(n,"Atleta ${t+1} $n",captain=i==0)}+Player(77,"Libero ${t+1}",libero=true)+Player(88,"Secondo libero ${t+1}",libero=true))
    })
    private fun start(server:Int=0,left:Int=0)=Demo.start().copy(lineups=listOf(numbers.take(6),numbers.take(6)),server=server,left=left)
    private fun betweenSets():Journal {
        var j=Journal(config=config());j=MatchEngine.add(j,start())
        repeat(11){j=MatchEngine.add(j,Event(Kind.POINT,0))};repeat(25){j=MatchEngine.add(j,Event(Kind.POINT,1))}
        j=MatchEngine.add(j,start(1,1));repeat(11){j=MatchEngine.add(j,Event(Kind.POINT,1))};repeat(25){j=MatchEngine.add(j,Event(Kind.POINT,0))}
        return j
    }
    private fun live():Journal {
        var j=MatchEngine.add(betweenSets(),start());repeat(18){j=MatchEngine.add(j,Event(Kind.POINT,0))};repeat(16){j=MatchEngine.add(j,Event(Kind.POINT,1))};return j
    }
    private fun finished():Journal {
        var j=Journal(config=config());val toss=start()
        val ink=listOf(listOf(SignaturePoint(.1f,.6f),SignaturePoint(.3f,.2f),SignaturePoint(.5f,.7f),SignaturePoint(.85f,.4f)))
        Signatures.required(j.config,true).forEach{j=Signatures.capture(j,it,ink,2.8f,toss)}
        repeat(3){n->j=MatchEngine.add(j,start(n%2,n%2));repeat(15+n){j=MatchEngine.add(j,Event(Kind.POINT,1))};repeat(25){j=MatchEngine.add(j,Event(Kind.POINT,0))}}
        j=Signatures.capture(j,Signatures.required(j.config,false).first(),ink,2.8f)
        return j
    }
    private fun open(layout:InterfaceStyle,mode:ThemeMode,journal:Journal):String {
        val activity=compose.activity
        AppearanceStore(activity).save(Appearance(ThemePalette.BLUE,mode,layout,StyleColor.BLUE,StyleColor.GOLD))
        GuideProgress(activity).markSeen()
        DraftStore(File(activity.filesDir,"setup-draft.json")).clear()
        val id=MatchStore(File(activity.filesDir,"matches")).create(journal)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("archive-match-$id").performScrollTo().performClick()
        return id
    }
    private fun menu(item:String){compose.onNodeWithTag("style-menu").performClick();compose.onNodeWithTag("style-menu-$item").performClick()}
    private fun capture(name:String) {
        compose.mainClock.advanceTimeBy(600)
        compose.waitForIdle()
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val result=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);val canvas=Canvas(result);view.invalidate();view.draw(canvas)
            val modal=org.robolectric.shadows.ShadowDialog.getLatestDialog()
            if(modal?.isShowing==true) {
                val overlay=modal.window!!.decorView
                val frame=Bitmap.createBitmap(overlay.width,overlay.height,Bitmap.Config.ARGB_8888);overlay.draw(Canvas(frame))
                canvas.drawBitmap(frame,0f,0f,null);frame.recycle()
            }
            val file=File("build/outputs/verification/fidelity-$name.png");file.parentFile!!.mkdirs()
            file.outputStream().use{result.compress(Bitmap.CompressFormat.PNG,100,it)};result.recycle()
        }
    }
    @Test fun courtThirdSetWithLongNamesShowsAllSixJerseysAndFixedConfirmation() {
        val original=betweenSets();val id=open(InterfaceStyle.COURT,ThemeMode.DARK,original)
        compose.onNodeWithText("Formazione · Set 3",substring=false).assertIsDisplayed()
        compose.onNodeWithTag("copy-previous").performScrollTo().performClick()
        compose.onNodeWithTag("style-match-summary").performScrollTo()
        listOf(4,3,2,5,6,1).forEach{compose.onNodeWithTag("lineup-slot-0-$it").assertIsDisplayed()}
        assertTrue(compose.onNodeWithTag("lineup-team-0").fetchSemanticsNode().boundsInRoot.height/compose.activity.resources.displayMetrics.density<=72f)
        compose.onNodeWithTag("start-set").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithText("Tagliando · vista dal fondo verso la rete").assertDoesNotExist()
        capture("C-preparazione-set3")
        compose.onNodeWithTag("start-set").performClick()
        compose.onNodeWithTag("copy-previous").performScrollTo().performClick()
        compose.onNodeWithTag("start-set").performClick()
        compose.onNodeWithText("Gara in corso",substring=false).assertIsDisplayed()
        val stored=MatchStore(File(compose.activity.filesDir,"matches")).load(id)
        assertEquals(3,MatchEngine.replay(stored.config,stored.events).current!!.number)
        assertEquals(original.events,stored.events.dropLast(1));assertEquals(listOf(numbers.take(6),numbers.take(6)),stored.events.last().lineups)
    }
    @Test fun firstSetUsesCompactTossAndTappingAssignsOnlyEligibleNumbers() {
        open(InterfaceStyle.COURT,ThemeMode.LIGHT,Journal(config=config()))
        compose.onNodeWithTag("formation-toss").performClick()
        compose.onNodeWithTag("toss-serve-1").performClick();compose.onNodeWithTag("toss-left-0").performClick()
        compose.onNodeWithText("Fatto",substring=false).performClick()
        numbers.take(6).forEachIndexed{i,num->
            compose.onNodeWithTag("lineup-player-0-$num").performScrollTo().performClick()
            compose.onNodeWithTag("lineup-slot-0-${i+1}").performScrollTo().performClick()
        }
        compose.onNodeWithTag("lineup-player-0-77").assertDoesNotExist()
        compose.onNodeWithTag("style-match-summary").performScrollTo()
        compose.onNodeWithTag("lineup-slot-0-1").assertIsDisplayed()
        compose.onNodeWithTag("start-set").assertIsDisplayed()
        compose.onNodeWithText("Conferma formazione",substring=false).assertIsDisplayed()
        capture("C-preparazione-chiaro")
    }
    @Test fun dashboardEditingIsASheetAndAllDataRemainInTheSummary() {
        open(InterfaceStyle.DASHBOARD,ThemeMode.LIGHT,Journal(config=config().copy(officials=config().officials.with(0,""))))
        menu("Dati");compose.onNodeWithText("Modifica i dati della gara").performScrollTo().performClick()
        compose.onNodeWithTag("dashboard-game").performClick()
        compose.onNodeWithTag("setup-field-location").performScrollTo().performTextReplacement("Como aggiornata")
        capture("B-modifica-dati")
        compose.onNodeWithTag("setup-overview").performClick()
        compose.onNodeWithTag("dashboard-game").assertIsDisplayed()
        capture("B-preparazione")
    }
    @Test fun reportScreensUseActualGeneratedPaperAndSignatureStatesForBothStyles() {
        listOf(InterfaceStyle.DASHBOARD,InterfaceStyle.COURT).forEach{layout->
            open(layout,if(layout==InterfaceStyle.COURT)ThemeMode.DARK else ThemeMode.LIGHT,finished())
            menu("Referto")
            compose.waitUntil(30000){compose.onAllNodesWithContentDescription("Anteprima prima pagina del referto compilato").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithTag("style-match-summary").performScrollTo()
            capture(if(layout==InterfaceStyle.COURT)"C-referto"else"B-referto")
            compose.onNodeWithText("Da raccogliere",substring=false).assertExists()
        }
    }
    @Test fun courtLiveScreenKeepsPointButtonsAndCommandsVisible() {
        listOf(InterfaceStyle.DASHBOARD,InterfaceStyle.COURT).forEach{layout->
            open(layout,if(layout==InterfaceStyle.COURT)ThemeMode.DARK else ThemeMode.LIGHT,live())
            compose.onNodeWithTag("point-button-0").assertIsDisplayed();compose.onNodeWithTag("point-button-1").assertIsDisplayed()
            compose.onNodeWithTag("dock-timeout").assertIsDisplayed()
            capture(if(layout==InterfaceStyle.COURT)"C-gara"else"B-gara")
        }
    }
}
