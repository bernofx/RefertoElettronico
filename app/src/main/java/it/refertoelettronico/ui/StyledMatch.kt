package it.refertoelettronico.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import it.refertoelettronico.model.*

@Composable
internal fun StyledMatchBody(config:MatchConfig,state:MatchState,lastEvent:Event?,journal:Journal?,selected:Int,onSelect:(Int)->Unit,
    onEvent:(Event)->Unit,onUndo:()->Unit,onRedo:()->Unit,canUndo:Boolean,canRedo:Boolean,onAction:(String,Int)->Unit,
    onHistory:()->Unit,onFinalSignatures:(()->Unit)?,preparation:@Composable ()->Unit) {
    val set=state.current
    val playing=set!=null&&set.endedAt==null&&!state.finished(config)&&!state.interrupted
    if(!playing && !state.finished(config) && !state.interrupted){preparation();return}
    val style=LocalAppearance.current.layout
    val left=set?.let{CourtGeometry.leftTeam(it)}?:0
    val problem=if(playing)MatchEngine.rallyProblem(state)else null
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        if(style==InterfaceStyle.DASHBOARD)Text("${config.competition.substringBefore(" - ")} · ${config.scheduledTime} · ${config.location}",style=MaterialTheme.typography.bodySmall)
        else CompactMatchSummary(config)
        if(playing && set!=null) {
            if(style==InterfaceStyle.COURT)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(left,1-left).forEach{t->
                    val accent=config.teams[t].displayColor(t)
                    TextButton(onClick={onSelect(t)},modifier=Modifier.weight(1f).testTag("dock-team-$t").semantics{contentDescription="Operazioni per ${config.teams[t].name}"},shape=MaterialTheme.shapes.small,
                        colors=ButtonDefaults.textButtonColors(contentColor=MaterialTheme.colorScheme.onSurface),contentPadding=PaddingValues(4.dp)) {
                        Box(Modifier.size(9.dp).background(accent,CircleShape));Spacer(Modifier.width(6.dp))
                        Text(shortTeamName(config.teams[t].name),style=MaterialTheme.typography.labelLarge,fontWeight=if(selected==t)FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            if(style==InterfaceStyle.DASHBOARD) {
                Surface(shape=MaterialTheme.shapes.small,modifier=Modifier.fillMaxWidth()) {
                    Row {
                        listOf(left,1-left).forEach{t->
                            val accent=config.teams[t].displayColor(t)
                            Surface(onClick={onSelect(t)},color=accent,contentColor=teamInk(accent),modifier=Modifier.weight(1f).testTag("point-card-$t")) {
                                Row(Modifier.padding(horizontal=10.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                                    Text(shortTeamName(config.teams[t].name),style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
                                    Text("${set.score[t]}",modifier=Modifier.testTag("score-$t"),fontSize=34.sp,fontWeight=FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(left,1-left).forEach{t->
                        val accent=config.teams[t].displayColor(t)
                        Button(onClick={onSelect(t);onEvent(Event(Kind.POINT,team=t))},enabled=problem==null,
                            modifier=Modifier.weight(1f).heightIn(min=48.dp).tourTarget("point-$t").testTag("point-button-$t"),
                            shape=MaterialTheme.shapes.small,colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=teamInk(accent))) {Text("+1 punto",style=MaterialTheme.typography.titleMedium)}
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    listOf(left,1-left).forEachIndexed{index,t->
                        val accent=config.teams[t].displayColor(t)
                        @Composable fun point(){Button(onClick={onSelect(t);onEvent(Event(Kind.POINT,team=t))},enabled=problem==null,
                            modifier=Modifier.size(52.dp).tourTarget("point-$t").testTag("point-button-$t"),shape=MaterialTheme.shapes.small,
                            colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=teamInk(accent)),contentPadding=PaddingValues(0.dp)){Text("+1",fontSize=21.sp,fontWeight=FontWeight.Bold)}}
                        if(index==0)point()
                        Text("${set.score[t]}",modifier=Modifier.testTag("score-$t"),fontSize=38.sp,fontWeight=FontWeight.Bold)
                        if(index==0)Column(horizontalAlignment=Alignment.CenterHorizontally) {
                            Text("Set ${set.number}",style=MaterialTheme.typography.labelMedium)
                            Text("${state.wins(left)}–${state.wins(1-left)}",style=MaterialTheme.typography.titleMedium)
                        }
                        if(index==1)point()
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                if(style==InterfaceStyle.COURT)OutlinedButton(onClick=onUndo,enabled=canUndo,modifier=Modifier.heightIn(min=36.dp).tourTarget("undo").testTag("styled-undo"),contentPadding=PaddingValues(horizontal=10.dp,vertical=3.dp),shape=MaterialTheme.shapes.small){Text("Annulla",style=MaterialTheme.typography.labelMedium)}
                else Text("Set ${set.number} · ${state.wins(left)}–${state.wins(1-left)} set",style=MaterialTheme.typography.labelMedium)
                TextButton(onClick=onRedo,enabled=canRedo,modifier=Modifier.tourTarget("redo"),contentPadding=PaddingValues(4.dp)){Text("Ripristina",style=MaterialTheme.typography.labelMedium)}
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("live-scroll-region"),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(problem!=null)Notice(problem)
            if(playing && set!=null) {
                TimeoutClock(lastEvent,config)
                if(set.changedEndsAt!=null)Text("Quinto set · cambio campo a ${scoreLabel(set.changedEndsAt)}",style=MaterialTheme.typography.bodySmall)
                ScorerCourt(config,state,Modifier.tourTarget("court"),compact=true)
                if(style==InterfaceStyle.DASHBOARD)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(left,1-left).forEach{t->
                        OutlinedCard(onClick={onSelect(t)},modifier=Modifier.weight(1f),colors=CardDefaults.outlinedCardColors(containerColor=lerp(MaterialTheme.colorScheme.surface,config.teams[t].displayColor(t),.10f))) {
                            Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text(shortTeamName(config.teams[t].name),style=MaterialTheme.typography.labelMedium)
                                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                                    listOf("Timeout" to "${set.timeouts.count{it.team==t}}/2","Cambi" to "${set.countSubs(t)}/${config.maxSubstitutions}","Libero" to if(set.liberos[t]!=null)"In campo"else"Fuori").forEach{(label,value)->
                                        Column(horizontalAlignment=Alignment.CenterHorizontally){Text(label,style=MaterialTheme.typography.labelSmall);Text(value,style=MaterialTheme.typography.labelMedium)}
                                    }
                                }
                            }
                        }
                    }
                }
                if(style==InterfaceStyle.DASHBOARD)OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                            Text("Ultime azioni",style=MaterialTheme.typography.titleSmall)
                            TextButton(onClick=onHistory,contentPadding=PaddingValues(0.dp)){Text("Registro",style=MaterialTheme.typography.labelSmall)}
                        }
                        (journal?.events?.takeLast(3)?:listOfNotNull(lastEvent)).forEach{e->Text("${clock(e.at)} · ${e.description(config)}",style=MaterialTheme.typography.bodySmall)}
                    }
                }
            }
            if(state.finished(config)) {
                Text(if(state.endedEarly)"Gara terminata anticipatamente · controlla le osservazioni"else"Gara conclusa · ${state.wins(0)}–${state.wins(1)}",style=MaterialTheme.typography.headlineSmall)
                if(onFinalSignatures!=null)Button(onClick=onFinalSignatures,colors=styleButtonColors()){Text("Firme e approvazione finale")}
                TextButton(onClick=onHistory){Text("Controlla il registro della gara")}
            }
            if(state.interrupted){Notice("Gara interrotta");Button(onClick={onAction("resume",selected)}){Text("Registra la ripresa")}}
            Row {
                TextButton(onClick={onAction("note",selected)},modifier=Modifier.weight(1f).tourTarget("notes")){Text("Osservazioni",style=MaterialTheme.typography.labelMedium)}
                TextButton(onClick={onAction("other",selected)},modifier=Modifier.weight(1f).tourTarget("other")){Text("Altre operazioni",style=MaterialTheme.typography.labelMedium)}
            }
        }
        if(playing && set!=null) {
            Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainer,modifier=Modifier.testTag("court-command-dock")) {
                Column(Modifier.padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Text("Operazioni · ${shortTeamName(config.teams[selected].name)}",style=MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(Triple("timeout","Timeout",UiSymbol.CLOCK),Triple("sub","Sostituzione",UiSymbol.SWAP),Triple("libero","Libero",UiSymbol.PEOPLE)).forEach{(action,label,icon)->
                            OutlinedButton(onClick={onAction(action,selected)},modifier=Modifier.weight(1f).heightIn(min=52.dp).tourTarget("$action-$selected").testTag("dock-$action"),shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(horizontal=3.dp,vertical=5.dp)) {
                                Column(horizontalAlignment=Alignment.CenterHorizontally){UiGlyph(icon,Modifier.size(18.dp));Text(if(style==InterfaceStyle.COURT&&action=="timeout")"Timeout ${set.timeouts.count{it.team==selected}}/2"else if(style==InterfaceStyle.COURT&&action=="sub")"Sostituzioni ${set.countSubs(selected)}/${config.maxSubstitutions}"else label,style=MaterialTheme.typography.labelSmall)}
                            }
                        }
                        if(style==InterfaceStyle.DASHBOARD)OutlinedButton(onClick=onUndo,enabled=canUndo,modifier=Modifier.weight(1f).heightIn(min=52.dp).tourTarget("undo").testTag("styled-undo"),shape=MaterialTheme.shapes.small,contentPadding=PaddingValues(3.dp)) {
                            Text("Annulla",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
