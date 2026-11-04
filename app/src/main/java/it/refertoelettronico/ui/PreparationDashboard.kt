package it.refertoelettronico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*

@Composable
internal fun PreparationDashboard(config:MatchConfig,locked:Boolean,onSection:(Int)->Unit,onNext:()->Unit,onSave:()->Unit) {
    val steps=PreparationProgress.steps(config)
    val ready=listOf(steps[0].complete,steps[1].complete&&steps[2].complete,steps[3].complete).count{it}
    Column(Modifier.fillMaxSize().testTag("preparation-dashboard")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    config.teams.forEachIndexed{t,team->Text("${if(t==0)"Casa"else"Ospiti"} · ${team.name.ifBlank{"Nome da inserire"}}",style=MaterialTheme.typography.titleSmall)}
                    Text("${config.date} · ${config.scheduledTime} · ${config.location.ifBlank{"Luogo da inserire"}}",style=MaterialTheme.typography.bodySmall)
                }
            }
            Text("Stato di preparazione · $ready/3 sezioni complete",style=MaterialTheme.typography.titleSmall)
            LinearProgressIndicator(progress={ready/3f},modifier=Modifier.fillMaxWidth())
            DashboardRow("Dati gara",steps[0].summary,steps[0].complete,"dashboard-game"){onSection(0)}
            DashboardRow("Rose · ${config.teams[0].players.size} + ${config.teams[1].players.size} presenti",
                if(steps[1].complete&&steps[2].complete)"Entrambe le rose complete"else"Controlla presenze, numeri e capitane",
                steps[1].complete&&steps[2].complete,"dashboard-rosters"){onSection(if(!steps[1].complete)1 else 2)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                config.teams.forEachIndexed{t,team->OutlinedButton(onClick={onSection(t+1)},modifier=Modifier.weight(1f).testTag("dashboard-team-$t")){
                    Text(team.name.ifBlank{if(t==0)"Casa"else"Ospiti"},style=MaterialTheme.typography.labelMedium)
                }}
            }
            DashboardRow("Ufficiali",steps[3].issues.firstOrNull()?.label?:steps[3].summary,steps[3].complete,"dashboard-officials"){onSection(3)}
            if(!locked)DashboardRow("Formazioni","Sorteggio e tagliandi dopo il salvataggio",false,"dashboard-lineups",onSave)
            val next=steps.firstOrNull{!it.complete}
            if(next!=null)Surface(modifier=Modifier.fillMaxWidth(),color=MaterialTheme.colorScheme.primaryContainer,shape=MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp)) {
                    Text("Prossimo passo · ${next.title}",style=MaterialTheme.typography.titleSmall)
                    Text(next.issues.first().label,style=MaterialTheme.typography.bodySmall)
                    TextButton(onClick=onNext,modifier=Modifier.testTag("dashboard-next")){Text("Completa")}
                }
            }
        }
        Button(onClick=onSave,modifier=Modifier.fillMaxWidth().padding(16.dp).heightIn(min=48.dp).testTag("dashboard-save")) {
            Text(if(locked)"Salva modifiche"else"Salva e prepara le formazioni")
        }
    }
}

@Composable
private fun DashboardRow(title:String,detail:String,complete:Boolean,tag:String,onClick:()->Unit) {
    OutlinedCard(onClick=onClick,modifier=Modifier.fillMaxWidth().testTag(tag),
        colors=CardDefaults.outlinedCardColors(containerColor=if(complete)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            UiGlyph(if(complete)UiSymbol.CHECK else UiSymbol.EDIT,Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(title,style=MaterialTheme.typography.titleSmall)
                Text(if(complete)"Completo · $detail"else detail,style=MaterialTheme.typography.bodySmall)
            }
            UiGlyph(UiSymbol.CHEVRON,Modifier.size(20.dp))
        }
    }
}
