package it.refertoelettronico.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label:String) { LIGHT("Chiaro"), DARK("Scuro"), SYSTEM("Come il telefono") }
enum class ThemePalette(val label:String,val description:String) {
    GREEN("Salvia", "Verde e superfici neutre"), BLUE("Blu sport", "Blu e accenti gialli"), VIOLET("Lavanda", "Viola e superfici morbide")
}
data class Appearance(val palette:ThemePalette=ThemePalette.GREEN,val mode:ThemeMode=ThemeMode.LIGHT) {
    fun dark(systemDark:Boolean)=mode==ThemeMode.DARK || (mode==ThemeMode.SYSTEM && systemDark)
    fun scheme(systemDark:Boolean):ColorScheme {
        val base=if(dark(systemDark)) darkColorScheme(
            primary=Color(0xFF8AD6BC),onPrimary=Color(0xFF00382C),primaryContainer=Color(0xFF174F40),onPrimaryContainer=Color(0xFFB5F2DA),
            secondary=Color(0xFFB5CCBF),onSecondary=Color(0xFF22372D),secondaryContainer=Color(0xFF354C40),onSecondaryContainer=Color(0xFFD1E8DA),
            tertiary=Color(0xFFF0C56C),onTertiary=Color(0xFF402D00),tertiaryContainer=Color(0xFF5C4308),onTertiaryContainer=Color(0xFFFFE2A4),
            background=Color(0xFF101713),onBackground=Color(0xFFE2EAE3),surface=Color(0xFF141D18),onSurface=Color(0xFFE2EAE3),
            surfaceVariant=Color(0xFF35443B),onSurfaceVariant=Color(0xFFBBCBC0),surfaceContainer=Color(0xFF1A251E),
            surfaceContainerLow=Color(0xFF17211B),surfaceContainerHigh=Color(0xFF253229),surfaceContainerHighest=Color(0xFF303D34),
            outline=Color(0xFF8B9D90),outlineVariant=Color(0xFF405246))
        else lightColorScheme(
            primary=Color(0xFF126657),onPrimary=Color.White,
            primaryContainer=Color(0xFFD7F2E7),onPrimaryContainer=Color(0xFF103F35),
            secondary=Color(0xFF53665E),secondaryContainer=Color(0xFFE3EDE7),onSecondaryContainer=Color(0xFF20372D),
            tertiary=Color(0xFF795517),tertiaryContainer=Color(0xFFFFE6B3),
            background=Color(0xFFF4F7F5),onBackground=Color(0xFF192C25),
            surface=Color(0xFFFCFEFC),onSurface=Color(0xFF192C25),
            surfaceVariant=Color(0xFFEAF0EB),onSurfaceVariant=Color(0xFF52645B),
            surfaceContainer=Color(0xFFEEF3EF),surfaceContainerLow=Color(0xFFF6F9F6),
            surfaceContainerHigh=Color(0xFFE6EEE8),surfaceContainerHighest=Color(0xFFE0E9E2),
            outline=Color(0xFF75857B),outlineVariant=Color(0xFFD4DFD6))
        return when(palette) {
            ThemePalette.GREEN->base
            ThemePalette.BLUE->if(dark(systemDark))base.copy(
                primary=Color(0xFFA9C7FF),onPrimary=Color(0xFF002E65),primaryContainer=Color(0xFF204677),onPrimaryContainer=Color(0xFFD5E3FF),
                background=Color(0xFF10151F),onBackground=Color(0xFFE1E7F2),surface=Color(0xFF151D2B),onSurface=Color(0xFFE1E7F2),
                surfaceVariant=Color(0xFF354155),onSurfaceVariant=Color(0xFFBECADD),surfaceContainer=Color(0xFF1B2637),surfaceContainerLow=Color(0xFF172131),
                surfaceContainerHigh=Color(0xFF263247),surfaceContainerHighest=Color(0xFF303E54),outline=Color(0xFF8C9CB5),outlineVariant=Color(0xFF42516A),
                secondary=Color(0xFFECC76B),onSecondary=Color(0xFF3E2E00),secondaryContainer=Color(0xFF57451A),onSecondaryContainer=Color(0xFFFFE6A7))
            else base.copy(primary=Color(0xFF215CA4),onPrimary=Color.White,primaryContainer=Color(0xFFD9E7FF),onPrimaryContainer=Color(0xFF12355F),
                background=Color(0xFFF4F7FC),onBackground=Color(0xFF1B2B42),surface=Color(0xFFFCFDFF),onSurface=Color(0xFF1B2B42),
                surfaceVariant=Color(0xFFE5ECF6),onSurfaceVariant=Color(0xFF4D6079),surfaceContainer=Color(0xFFECF1F9),surfaceContainerLow=Color(0xFFF5F8FD),
                surfaceContainerHigh=Color(0xFFE4EBF5),surfaceContainerHighest=Color(0xFFDCE5F1),outline=Color(0xFF708198),outlineVariant=Color(0xFFCED9E9),
                secondary=Color(0xFF735600),onSecondary=Color.White,secondaryContainer=Color(0xFFFFE5A1),onSecondaryContainer=Color(0xFF3A2A00))
            ThemePalette.VIOLET->if(dark(systemDark))base.copy(
                primary=Color(0xFFD2BCFF),onPrimary=Color(0xFF38215E),primaryContainer=Color(0xFF503875),onPrimaryContainer=Color(0xFFECDDFF),
                background=Color(0xFF17131F),onBackground=Color(0xFFECE3F5),surface=Color(0xFF201A2A),onSurface=Color(0xFFECE3F5),
                surfaceVariant=Color(0xFF493E56),onSurfaceVariant=Color(0xFFD0C1DC),surfaceContainer=Color(0xFF282133),surfaceContainerLow=Color(0xFF231C2D),
                surfaceContainerHigh=Color(0xFF33293F),surfaceContainerHighest=Color(0xFF3E334B),outline=Color(0xFFA393B2),outlineVariant=Color(0xFF574863),
                secondary=Color(0xFFF0B6CC),onSecondary=Color(0xFF4E2033),secondaryContainer=Color(0xFF66374A),onSecondaryContainer=Color(0xFFFFD9E6))
            else base.copy(primary=Color(0xFF70479C),onPrimary=Color.White,primaryContainer=Color(0xFFEEDCFF),onPrimaryContainer=Color(0xFF40215F),
                background=Color(0xFFFAF5FC),onBackground=Color(0xFF33253E),surface=Color(0xFFFFFBFF),onSurface=Color(0xFF33253E),
                surfaceVariant=Color(0xFFF0E6F4),onSurfaceVariant=Color(0xFF6C5879),surfaceContainer=Color(0xFFF4EAF8),surfaceContainerLow=Color(0xFFFBF5FD),
                surfaceContainerHigh=Color(0xFFEEE1F4),surfaceContainerHighest=Color(0xFFE7D8EE),outline=Color(0xFF8B7597),outlineVariant=Color(0xFFE0D0E8),
                secondary=Color(0xFF924968),onSecondary=Color.White,secondaryContainer=Color(0xFFFFD9E6),onSecondaryContainer=Color(0xFF4F2236))
        }
    }
}
class AppearanceStore(context:Context) {
    private val prefs=context.applicationContext.getSharedPreferences("appearance",Context.MODE_PRIVATE)
    fun load()=Appearance(
        ThemePalette.entries.firstOrNull{it.name==prefs.getString("palette",null)}?:ThemePalette.GREEN,
        ThemeMode.entries.firstOrNull{it.name==prefs.getString("mode",null)}?:ThemeMode.LIGHT)
    fun save(value:Appearance) {
        check(prefs.edit().putString("palette",value.palette.name).putString("mode",value.mode.name).commit()) {"Impossibile salvare il tema."}
    }
}
@Composable
fun AppearanceDialog(value:Appearance,onChange:(Appearance)->Unit,onClose:()->Unit) {
    AlertDialog(onDismissRequest=onClose,title={Text("Aspetto e temi")},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Luminosità",style=MaterialTheme.typography.titleSmall)
            ThemeMode.entries.forEach{mode->
                TextButton(onClick={onChange(value.copy(mode=mode))},modifier=Modifier.fillMaxWidth().testTag("theme-mode-${mode.name}")) {
                    RadioButton(selected=value.mode==mode,onClick=null);Spacer(Modifier.width(8.dp));Text(mode.label,Modifier.weight(1f))
                }
            }
            HorizontalDivider()
            Text("Palette",style=MaterialTheme.typography.titleSmall)
            ThemePalette.entries.forEach{palette->
                val colors=Appearance(palette,value.mode).scheme(isSystemInDarkTheme())
                OutlinedButton(onClick={onChange(value.copy(palette=palette))},modifier=Modifier.fillMaxWidth().testTag("theme-palette-${palette.name}"),
                    colors=ButtonDefaults.outlinedButtonColors(containerColor=colors.surface,contentColor=colors.onSurface)) {
                    RadioButton(selected=value.palette==palette,onClick=null)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)){Text(palette.label);Text(palette.description,style=MaterialTheme.typography.bodySmall)}
                    Surface(color=colors.primary,shape=MaterialTheme.shapes.small,modifier=Modifier.size(20.dp)){}
                }
            }
            Text("Il tema non cambia i colori delle squadre o il PDF.",style=MaterialTheme.typography.bodySmall)
        }},confirmButton={TextButton(onClick=onClose){Text("Fatto")}})
}
