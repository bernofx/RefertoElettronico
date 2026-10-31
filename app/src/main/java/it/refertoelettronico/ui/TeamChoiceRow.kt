package it.refertoelettronico.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.MatchConfig

/** Home/away identity is stable even before the toss assigns A/B and after ends change. */
@Composable
fun TeamChoiceRow(config:MatchConfig,selected:Int,tag:String,details:List<String>?=null,onSelect:(Int)->Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        config.teams.forEachIndexed{t,team->
            val chosen=selected==t
            val accent=team.displayColor(t)
            val ink=if(chosen)teamInk(accent) else MaterialTheme.colorScheme.onSurface
            Surface(shape=MaterialTheme.shapes.medium,
                color=if(chosen)accent else MaterialTheme.colorScheme.surface,contentColor=ink,
                border=BorderStroke(if(chosen)2.dp else 1.dp,if(chosen)MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant),
                modifier=Modifier.weight(1f).fillMaxHeight().heightIn(min=64.dp).testTag("$tag-$t")
                    .selectable(selected=chosen,role=Role.RadioButton,onClick={onSelect(t)})) {
                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(9.dp).background(team.displayColor(t),CircleShape).border(1.dp,MaterialTheme.colorScheme.outline,CircleShape))
                        Text(if(t==0)"Casa" else "Ospiti",style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f))
                        if(chosen)UiGlyph(UiSymbol.CHECK,Modifier.size(16.dp),ink)
                    }
                    Text(team.name,style=MaterialTheme.typography.labelLarge,modifier=Modifier.testTag("$tag-$t-name"))
                    details?.getOrNull(t)?.let{Text(it,style=MaterialTheme.typography.labelSmall)}
                }
            }
        }
    }
}
