package it.refertoelettronico

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.ui.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class StylePreferencesTest {
    @Test fun layoutsRememberIndependentModePaletteAndBothColors() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val store=AppearanceStore(context)
        val b=Appearance(ThemePalette.VIOLET,ThemeMode.LIGHT,InterfaceStyle.DASHBOARD,StyleColor.TEAL,StyleColor.ORANGE)
        val c=Appearance(ThemePalette.BLUE,ThemeMode.SYSTEM,InterfaceStyle.COURT,StyleColor.BLUE,StyleColor.GOLD)
        store.save(b);store.save(c)
        assertEquals(c,AppearanceStore(context).load());assertEquals(b,store.forLayout(InterfaceStyle.DASHBOARD))
        store.save(store.forLayout(InterfaceStyle.DASHBOARD));assertEquals(b,store.load());assertEquals(c,store.forLayout(InterfaceStyle.COURT))
    }
    @Test fun upgradesKeepPreviousThemeAndDoNotInventStylePreferences() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("appearance",Context.MODE_PRIVATE).edit().clear().putString("palette","VIOLET").putString("mode","DARK").commit()
        val store=AppearanceStore(context)
        assertEquals(Appearance(ThemePalette.VIOLET,ThemeMode.DARK),store.load())
        assertEquals(ThemeMode.LIGHT,store.forLayout(InterfaceStyle.DASHBOARD).mode)
        assertEquals(ThemeMode.DARK,store.forLayout(InterfaceStyle.COURT).mode)
    }
    @Test fun allSelectableColorPairsKeepTextContrastInBothLayoutsAndModes() {
        for(style in listOf(InterfaceStyle.DASHBOARD,InterfaceStyle.COURT))for(mode in listOf(ThemeMode.LIGHT,ThemeMode.DARK))
            for(primary in StyleColor.entries)for(accent in StyleColor.entries) {
                val c=Appearance(layout=style,mode=mode,primary=primary,accent=accent).scheme(false)
                listOf(c.onPrimary to c.primary,c.onPrimaryContainer to c.primaryContainer,c.onSecondary to c.secondary,c.onSecondaryContainer to c.secondaryContainer).forEach{(ink,paper)->
                    assertTrue("$style $mode $primary $accent",ColorUtils.calculateContrast(ink.toArgb(),paper.toArgb())>=4.5)
                }
            }
    }
}
