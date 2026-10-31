package it.refertoelettronico

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
@Config(sdk=[35],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MatchBadgesTest {
    @get:Rule val compose=createComposeRule()
    @Test fun badgesShowFullTeamsTimeCourtCategoryAndScoreGroupedByDateAndTimeWithSafeDelete() {
        fun config(number:String,date:String,time:String,category:String)=Demo.config().copy(matchNumber=number,date=date,scheduledTime=time,venue="Campo 3",competition=category)
        val late=Journal(config=config("0006","02/10/2026","14:00","U13 Femminile"))
        val early=late.copy(config=config("0012","02/10/2026","10:00","U14 Femminile"))
        var live=Journal(config=config("0026","03/10/2026","14:00","U15 Femminile"),events=listOf(Demo.start()))
        repeat(5){live=MatchEngine.add(live,Event(Kind.POINT,0))};repeat(2){live=MatchEngine.add(live,Event(Kind.POINT,1))}
        val archives=listOf("late" to Result.success(late),"early" to Result.success(early),"live" to Result.success(live))
        val opened=mutableListOf<String>();val deleted=mutableListOf<String>()
        var view:android.view.View?=null
        compose.setContent{view=LocalView.current.rootView;VolleyTheme{HomeScreen(archives,{},{},{opened+=it},{},{deleted+=it})}}
        compose.onNodeWithText("U13 F").assertExists();compose.onNodeWithText("U14 F").assertExists();compose.onNodeWithText("U15 F").assertExists()
        compose.onNodeWithText("Gara 0006").assertExists();compose.onNodeWithText("Gara 0012").assertExists()
        compose.onNodeWithText("5–2").assertExists();compose.onNodeWithText("punti · set 1").assertExists()
        compose.onNodeWithText("In corso").assertExists();compose.onAllNodesWithText("Da iniziare").assertCountEquals(2)
        val topLive=compose.onNodeWithTag("archive-match-live").fetchSemanticsNode().boundsInRoot.top
        val topEarly=compose.onNodeWithTag("archive-match-early").fetchSemanticsNode().boundsInRoot.top
        val topLate=compose.onNodeWithTag("archive-match-late").fetchSemanticsNode().boundsInRoot.top
        assertTrue(topLive<topEarly);assertTrue(topEarly<topLate)
        compose.onNodeWithTag("archive-match-early").performScrollTo().performClick()
        assertEquals(listOf("early"),opened)
        compose.onNodeWithTag("delete-match-late").performScrollTo().performClick()
        compose.onNodeWithText("Eliminare questa gara?").assertExists()
        compose.onNodeWithTag("confirm-delete-match").performClick()
        assertEquals(listOf("late"),deleted);assertEquals(listOf("early"),opened)
        compose.onNodeWithTag("archive-match-live").performScrollTo()
        compose.runOnIdle {
            val root=view!!
            val bitmap=android.graphics.Bitmap.createBitmap(root.width,root.height,android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bitmap))
            val dir=File("build/outputs/verification").apply{mkdirs()}
            File(dir,"ui-badge-gare-0.6.7.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        }
    }
}
