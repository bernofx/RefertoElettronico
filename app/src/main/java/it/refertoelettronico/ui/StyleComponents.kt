package it.refertoelettronico.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*

/** Conventional abbreviations only; the full identity remains the accessible label. */
internal fun shortTeamName(name:String):String=name.trim()
    .replace(Regex("(?i)^polisportiva\\s+"),"P. ")
    .replace(Regex("(?i)\\s+(a\\.?s\\.?d\\.?|a\\.?s\\.?s\\.?d\\.?)$"),"")

@Composable internal fun styleAction():Color {
    val a=LocalAppearance.current
    val tone=a.primary?:when(a.palette){ThemePalette.GREEN->StyleColor.GREEN;ThemePalette.BLUE->StyleColor.BLUE;ThemePalette.VIOLET->StyleColor.VIOLET}
    return when(tone){StyleColor.BLUE->Color(0xFF0064F5);StyleColor.GREEN->Color(0xFF087D5D);StyleColor.VIOLET->Color(0xFF7C3AED);else->tone.color(false)}
}
@Composable internal fun styleButtonColors()=ButtonDefaults.buttonColors(containerColor=styleAction(),contentColor=teamInk(styleAction()))
@Composable internal fun styleSuccess()=if(MaterialTheme.colorScheme.background.luminance()<.2f)Color(0xFF63E0AC)else Color(0xFF137D45)

@Composable
internal fun StyleAppBar(title:String,onBack:()->Unit,actions:List<Pair<String,()->Unit>>) {
    var expanded by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onBack,modifier=Modifier.testTag("style-back")){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Indietro",tint=MaterialTheme.colorScheme.onSurface)}
        Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,textAlign=if(LocalAppearance.current.layout==InterfaceStyle.COURT)TextAlign.Center else TextAlign.Start)
        Box {
            IconButton(onClick={expanded=true},modifier=Modifier.testTag("style-menu")){Icon(Icons.Default.MoreVert,"Menu gara",tint=MaterialTheme.colorScheme.onSurface)}
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                actions.forEach{(label,action)->DropdownMenuItem(text={Text(label)},modifier=Modifier.testTag("style-menu-$label"),onClick={expanded=false;action()})}
            }
        }
    }
}

@Composable
internal fun CompactMatchSummary(config:MatchConfig,detail:String="",overview:Boolean=false) {
    Surface(shape=MaterialTheme.shapes.small,color=MaterialTheme.colorScheme.surfaceContainer,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),modifier=Modifier.testTag("style-match-summary")) {
        if(overview)Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("${config.competition.substringBefore(" - ")} · Gara ${config.matchNumber}",style=MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1.5f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    config.teams.forEachIndexed{t,team->Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(team.displayColor(t),CircleShape))
                        Text(shortTeamName(team.name).ifBlank{if(t==0)"Squadra di casa"else"Squadra ospite"},style=MaterialTheme.typography.titleSmall,modifier=Modifier.semantics{contentDescription=team.name})
                    }}
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(config.date,style=MaterialTheme.typography.bodySmall)
                    Text("${config.scheduledTime} · ${config.location}",style=MaterialTheme.typography.bodySmall)
                }
            }
        } else Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
            Text("${shortTeamName(config.teams[0].name)} · ${shortTeamName(config.teams[1].name)}",style=MaterialTheme.typography.labelLarge,
                modifier=Modifier.semantics{contentDescription=config.teams.joinToString(" contro "){it.name}})
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
                UiGlyph(UiSymbol.CLOCK,Modifier.size(14.dp))
                Text("${config.date} · ${config.scheduledTime} · ${config.location}",style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f))
            }
            if(detail.isNotEmpty())Text(detail,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun CompactTeamTabs(config:MatchConfig,selected:Int,tag:String,order:List<Int> = listOf(0,1),detail:((Int)->String)?=null,onSelect:(Int)->Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        order.forEach{t->
            val accent=config.teams[t].displayColor(t);val chosen=t==selected
            Surface(color=if(chosen)accent else MaterialTheme.colorScheme.surfaceContainer,
                contentColor=if(chosen)teamInk(accent)else MaterialTheme.colorScheme.onSurface,
                shape=MaterialTheme.shapes.small,border=BorderStroke(1.dp,if(chosen)teamDetailInk(accent,accent)else MaterialTheme.colorScheme.outline),
                modifier=Modifier.weight(1f).fillMaxHeight().heightIn(min=44.dp).testTag("$tag-$t")
                    .semantics{contentDescription=config.teams[t].name}
                    .selectable(selected=chosen,role=Role.Tab,onClick={onSelect(t)})) {
                Row(Modifier.padding(horizontal=10.dp,vertical=7.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(accent,CircleShape).border(1.dp,if(chosen)teamInk(accent)else MaterialTheme.colorScheme.outline,CircleShape))
                    Column(Modifier.weight(1f)) {
                        Text(shortTeamName(config.teams[t].name),style=MaterialTheme.typography.labelLarge)
                        detail?.invoke(t)?.let{Text(it,style=MaterialTheme.typography.labelSmall)}
                    }
                }
            }
        }
    }
}

/** State uses a coloured mark as well as an accessible description. */
@Composable
internal fun StyleStepMark(complete:Boolean,modifier:Modifier=Modifier.size(20.dp)) {
    val green=styleSuccess()
    Surface(shape=CircleShape,color=if(complete)green else Color.Transparent,
        contentColor=teamInk(green),border=if(complete)null else BorderStroke(1.dp,MaterialTheme.colorScheme.outline),
        modifier=modifier.semantics{stateDescription=if(complete)"Completato"else"Da completare"}) {
        Box(contentAlignment=Alignment.Center){if(complete)UiGlyph(UiSymbol.CHECK,Modifier.size(13.dp))}
    }
}
