package it.refertoelettronico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*

@Composable
internal fun PreparationDashboard(config:MatchConfig,locked:Boolean,onSection:(Int)->Unit,onNext:()->Unit,onSave:()->Unit) {
    val steps=PreparationProgress.steps(config)
    val ready=listOf(steps[0].complete,steps[1].complete&&steps[2].complete,steps[3].complete,locked).count{it}
    Column(Modifier.fillMaxSize().testTag("preparation-dashboard")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            CompactMatchSummary(config,overview=true)
            Text("Stato di preparazione · $ready di 4 completi",style=MaterialTheme.typography.titleSmall)
            LinearProgressIndicator(progress={ready/4f},modifier=Modifier.fillMaxWidth().height(12.dp).testTag("preparation-progress-bar"),color=styleSuccess(),trackColor=lerp(MaterialTheme.colorScheme.surface,styleSuccess(),.15f))
            DashboardRow("Dati gara",steps[0].summary,steps[0].complete,"dashboard-game"){onSection(0)}
            DashboardRow("Rose · ${config.teams[0].players.size} + ${config.teams[1].players.size} presenti",
                if(steps[1].complete&&steps[2].complete)"${shortTeamName(config.teams[0].name)}: ${config.teams[0].players.size} · ${shortTeamName(config.teams[1].name)}: ${config.teams[1].players.size}"else"Controlla presenze, numeri e capitane",
                steps[1].complete&&steps[2].complete,"dashboard-rosters"){onSection(if(!steps[1].complete)1 else 2)}
            DashboardRow("Ufficiali",steps[3].issues.firstOrNull()?.label?:steps[3].summary,steps[3].complete,"dashboard-officials"){onSection(3)}
            DashboardRow("Formazioni",if(locked)"Formazioni registrate"else"Da inserire · titolari del primo set",locked,"dashboard-lineups",onSave)
            val next=steps.firstOrNull{!it.complete}
            if(next!=null)Surface(onClick=onNext,modifier=Modifier.fillMaxWidth().testTag("dashboard-next"),color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.small) {
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    UiGlyph(UiSymbol.GUIDE,Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Completa · ${next.title}",style=MaterialTheme.typography.titleSmall)
                        Text(next.issues.first().label,style=MaterialTheme.typography.bodySmall)
                    }
                    UiGlyph(UiSymbol.CHEVRON,Modifier.size(18.dp))
                }
            }
        }
        Button(onClick=onSave,modifier=Modifier.fillMaxWidth().padding(16.dp).heightIn(min=48.dp).testTag("dashboard-save"),colors=styleButtonColors(),shape=MaterialTheme.shapes.small) {
            Text(if(locked)"Salva modifiche"else"Prepara le formazioni")
        }
    }
}

@Composable
private fun DashboardRow(title:String,detail:String,complete:Boolean,tag:String,onClick:()->Unit) {
    val warning=!complete&&tag!="dashboard-lineups"
    val stateColor=if(complete)styleSuccess()else if(warning)MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
    OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag(tag),
        colors=CardDefaults.outlinedCardColors(containerColor=if(complete||warning)lerp(MaterialTheme.colorScheme.surface,stateColor,.09f)else MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(complete)Surface(shape=CircleShape,color=styleSuccess(),contentColor=MaterialTheme.colorScheme.surface,modifier=Modifier.size(22.dp)) {
                androidx.compose.foundation.layout.Box(contentAlignment=Alignment.Center){UiGlyph(UiSymbol.CHECK,Modifier.size(14.dp))}
            } else if(warning)Icon(Icons.Default.Warning,null,modifier=Modifier.size(22.dp),tint=stateColor)
            else Surface(shape=CircleShape,color=Color.Transparent,border=BorderStroke(1.dp,stateColor),modifier=Modifier.size(22.dp)){}
            Column(Modifier.weight(1f)) {
                Text(if(complete&&tag=="dashboard-game")"$title · Completi"else if(warning&&tag=="dashboard-officials")"$title · Da completare"else title,style=MaterialTheme.typography.titleSmall,color=if(complete||warning)stateColor else MaterialTheme.colorScheme.onSurface)
                Text(detail,style=MaterialTheme.typography.bodySmall)
            }
            UiGlyph(UiSymbol.CHEVRON,Modifier.size(20.dp))
        }
    }
}
