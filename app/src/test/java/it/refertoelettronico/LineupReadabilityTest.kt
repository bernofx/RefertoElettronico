package it.refertoelettronico

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
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
class LineupReadabilityTest {
    @get:Rule val compose=createComposeRule()
    @Test fun whiteAndYellowTeamsKeepTheirColourWhileFormationNumbersAndNetRemainLegible() {
        val config=Demo.config().copy(teams=Demo.config().teams.mapIndexed{i,t->t.copy(colorKey=if(i==0)"WHITE"else"YELLOW")})
        var view:android.view.View?=null
        compose.setContent {view=LocalView.current.rootView;VolleyTheme {Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            StartSetPanel(config,MatchEngine.initial(config),{})
        }}}
        for(t in 0..1) {
            compose.onNodeWithTag("lineup-team-$t").performScrollTo().performClick()
            compose.onNodeWithTag("lineup-player-$t-1").performScrollTo().performClick()
            compose.onNodeWithTag("lineup-slot-$t-4").performScrollTo().performClick()
            val layouts=mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("lineup-slot-number-$t-4",useUnmergedTree=true)
                .assertTextEquals("1").performSemanticsAction(SemanticsActions.GetTextLayoutResult){get->get(layouts)}
            assertEquals(Color.Black,layouts.single().layoutInput.style.color)
            compose.onNodeWithTag("lineup-net-$t").assertExists()
        }
        // All palette choices must retain sufficient contrast for ink on either board surface.
        for(choice in TeamColor.entries)for(bg in listOf(Color(0xFFFCFEFC),Color(0xFFEAF0EB),Color(0xFFD7F2E7))) {
            val ink=teamDetailInk(Color(choice.argb),bg)
            val ratio=(maxOf(ink.luminance(),bg.luminance())+.05f)/(minOf(ink.luminance(),bg.luminance())+.05f)
            assertTrue("${choice.name}: contrasto $ratio",ratio>=4.5f)
        }
        assertEquals("WHITE",config.teams[0].colorKey)
        compose.onNodeWithTag("lineup-team-0").performScrollTo().performClick()
        compose.onNodeWithTag("lineup-slot-0-4").performScrollTo()
        compose.runOnIdle {
            val root=view!!
            val bitmap=android.graphics.Bitmap.createBitmap(root.width,root.height,android.graphics.Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bitmap))
            val dir=File("build/outputs/verification").apply{mkdirs()}
            File(dir,"ui-formazione-bianco-0.6.7.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}
        }
    }
}
