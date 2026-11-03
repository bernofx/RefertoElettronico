package it.refertoelettronico

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.graphics.toArgb
import it.refertoelettronico.model.Demo
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
@Config(sdk=[35],qualifiers="w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceTest {
    @get:Rule val compose=createComposeRule()
    @Test fun palettesHaveReadableTextAndSystemModeFollowsThePhone() {
        ThemePalette.entries.forEach{palette->ThemeMode.entries.forEach{mode->listOf(false,true).forEach{night->
            val colors=Appearance(palette,mode).scheme(night)
            listOf(colors.onSurface to colors.surface,colors.onBackground to colors.background,
                colors.onSurfaceVariant to colors.surfaceVariant,colors.onPrimary to colors.primary,
                colors.onPrimaryContainer to colors.primaryContainer,colors.onSecondaryContainer to colors.secondaryContainer,
                colors.onSecondary to colors.secondary).forEach{(ink,paper)->
                assertTrue("$palette $mode $night: ${ColorUtils.calculateContrast(ink.toArgb(),paper.toArgb())}",ColorUtils.calculateContrast(ink.toArgb(),paper.toArgb())>=4.5)
            }
        }}}
        assertFalse(Appearance(mode=ThemeMode.SYSTEM).dark(false));assertTrue(Appearance(mode=ThemeMode.SYSTEM).dark(true))
        assertFalse(Appearance(mode=ThemeMode.LIGHT).dark(true));assertTrue(Appearance(mode=ThemeMode.DARK).dark(false))
    }
    @Test fun preferenceSurvivesReopeningAndUnknownValuesFallBack() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=AppearanceStore(context)
        val value=Appearance(ThemePalette.BLUE,ThemeMode.SYSTEM);store.save(value)
        assertEquals(value,AppearanceStore(context).load())
        context.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().putString("palette","unknown").putString("mode","unknown").commit()
        assertEquals(Appearance(),AppearanceStore(context).load())
    }
    @Test fun chooserChangesBothPreferencesWithoutClosingOrTouchingMatchState() {
        var value by mutableStateOf(Appearance());var closed=false
        compose.setContent{VolleyTheme(value){AppearanceDialog(value,{value=it},{closed=true})}}
        compose.onNodeWithTag("theme-mode-DARK").performScrollTo().performClick()
        compose.onNodeWithTag("theme-palette-BLUE").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(Appearance(ThemePalette.BLUE,ThemeMode.DARK),value);assertFalse(closed)}
        compose.onNodeWithText("Fatto").performClick();assertTrue(closed)
    }
    @Test fun renderSixProposalsUsingTheRealPreparationScreen() {
        var value by mutableStateOf(Appearance());var root:android.view.View?=null
        compose.setContent{root=LocalView.current.rootView;VolleyTheme(value){Surface(Modifier.fillMaxSize()){
            Column {PreparationHeader({},{},{})
                MatchEditor(Demo.config(),{},false)}
        }}}
        ThemePalette.entries.forEach{palette->listOf(ThemeMode.LIGHT,ThemeMode.DARK).forEach{mode->
            compose.runOnIdle{value=Appearance(palette,mode)}
            compose.waitForIdle()
            compose.runOnIdle{
                val view=root!!;val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap))
                val file=File("build/outputs/verification/theme-${palette.name}-${mode.name}.png");file.parentFile!!.mkdirs()
                file.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
        }}
    }
    @Test @Config(qualifiers="w320dp-h720dp-xhdpi")
    fun narrowPhoneWithLargeTextKeepsAllStepLabelsOnOneLineAndToolbarCompact() {
        compose.setContent{
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,1.3f)) {
                VolleyTheme{Surface(Modifier.fillMaxSize()){Column{
                    PreparationHeader({},{},{})
                    MatchEditor(Demo.config(),{},false)
                }}}
            }
        }
        listOf("Gara","Casa","Ospiti","Ufficiali").forEachIndexed{index,label->
            compose.onNodeWithTag("setup-step-label-$index",useUnmergedTree=true).assertIsDisplayed().assertTextEquals(label)
                .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult){get->
                    val results=mutableListOf<TextLayoutResult>();get(results)
                    assertEquals(1,results.single().lineCount);assertFalse("$label: ${results.single().size}, width=${results.single().didOverflowWidth}, height=${results.single().didOverflowHeight}",results.single().hasVisualOverflow)
                }
        }
        compose.onNodeWithTag("setup-toolbar").assertHeightIsEqualTo(androidx.compose.ui.unit.Dp(56f))
        compose.onNodeWithContentDescription("Indietro").assertIsDisplayed()
        compose.onNodeWithTag("editor-guide").assertIsDisplayed()
        compose.onNodeWithContentDescription("Gare").assertIsDisplayed()
        compose.onNodeWithTag("setup-step-1").performClick()
        compose.onNodeWithTag("attendance-count").assertExists()
    }
}
