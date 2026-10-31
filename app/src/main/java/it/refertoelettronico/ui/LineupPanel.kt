package it.refertoelettronico.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import it.refertoelettronico.model.*
import kotlin.math.roundToInt

/** Order stored in the event always remains zone 1..6, independently of the sheet view. */
fun assignLineup(lineup:List<Int>,zone:Int,number:Int):List<Int> {
    require(lineup.size==6 && zone in 1..6 && number>0)
    val old=lineup[zone-1]
    return lineup.mapIndexed { index,value->when {index==zone-1->number;value==number->old;else->value} }
}

@Composable
fun StartSetPanel(config:MatchConfig,state:MatchState,onEvent:(Event)->Unit) {
    val n=state.sets.size+1
    val deciding=MatchEngine.isDeciding(config,n)
    val eligible=config.teams.mapIndexed { t,team->team.players.filter {
        it.number !in state.designatedLiberos[t] && it.number !in state.retired[t] && it.number !in state.disqualified[t]
    }.map{it.number} }
    var lineups by remember(n,eligible){mutableStateOf(List(2){List(6){0}})}
    var server by remember(n){mutableIntStateOf(if(n>1 && !deciding)1-state.current!!.firstServer else -1)}
    var left by remember(n){mutableIntStateOf(if(n>1 && !deciding)1-state.current!!.initialLeft else -1)}
    var team by remember(n){mutableIntStateOf(0)}
    val complete=lineups.all{it.all{number->number>0} && it.distinct().size==6}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Formazioni · set $n",style=MaterialTheme.typography.titleLarge)
        if(n==1 || deciding)Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(deciding)"Nuovo sorteggio · quinto set" else "Esito del sorteggio",style=MaterialTheme.typography.titleMedium)
                Text("Riporta le scelte comunicate dall’arbitro.",style=MaterialTheme.typography.bodySmall)
                TossChoice("Chi serve per primo?","serve",server,config){server=it}
                TossChoice("Chi è a sinistra del tavolo?","left",left,config){left=it}
                Text(if(n==1)"Guarda il campo dal tavolo del segnapunti: la squadra inizialmente a sinistra sarà A, l’altra B." else "Guarda dal tavolo del segnapunti. Le lettere A/B restano quelle del primo set.",style=MaterialTheme.typography.bodySmall)
            }
        } else Text("Servizio: ${config.teams[server].name} · A sinistra del tavolo: ${config.teams[left].name}. Scelte alternate dal set precedente.",style=MaterialTheme.typography.bodySmall)
        if(eligible.any{it.size<6})Notice("Meno di sei giocatrici disponibili: registra la decisione dell’arbitro nelle altre operazioni.")
        else {
            TeamChoiceRow(config,team,"lineup-team",lineups.map{"${it.count{number->number>0}}/6 in formazione"}){team=it}
            key(n,team) {
                LineupBoard(team,config.teams[team],eligible[team],lineups[team],onAssign={zone,number->
                    lineups=lineups.with(team,assignLineup(lineups[team],zone,number))
                },onClear={lineups=lineups.with(team,List(6){0})})
            }
            val previous=state.current?.initial?.get(team)
            if(previous!=null && previous.all{it in eligible[team]})TextButton(onClick={lineups=lineups.with(team,previous)}){Text("Copia formazione del set precedente")}
            Text("Casa ${lineups[0].count{it>0}}/6 · Ospiti ${lineups[1].count{it>0}}/6",style=MaterialTheme.typography.labelMedium)
            if(server<0 || left<0)Text("Completa entrambe le scelte del sorteggio.",style=MaterialTheme.typography.bodySmall)
            Button(enabled=complete && server>=0 && left>=0,onClick={onEvent(Event(Kind.START_SET,lineups=lineups,server=server,left=left))},
                modifier=Modifier.fillMaxWidth().testTag("start-set")){Text("Conferma e inizia il set $n")}
        }
    }
}

@Composable
private fun TossChoice(label:String,tag:String,selected:Int,config:MatchConfig,onSelect:(Int)->Unit) {
    Text(label,style=MaterialTheme.typography.labelLarge)
    TeamChoiceRow(config,selected,"toss-$tag",onSelect=onSelect)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineupBoard(teamIndex:Int,team:Team,eligible:List<Int>,lineup:List<Int>,onAssign:(Int,Int)->Unit,onClear:()->Unit) {
    val assign by rememberUpdatedState(onAssign)
    var selected by remember{mutableStateOf<Int?>(null)}
    var dragging by remember{mutableStateOf<Int?>(null)}
    var position by remember{mutableStateOf(Offset.Zero)}
    var origin by remember{mutableStateOf(Offset.Zero)}
    val targets=remember{mutableStateMapOf<Int,Rect>()}
    val accent=team.displayColor(teamIndex)
    val hovered=if(dragging==null)null else targets.entries.firstOrNull{it.value.contains(position)}?.key
    @Composable fun dragModifier(number:Int):Modifier {
        var coords by remember{mutableStateOf<LayoutCoordinates?>(null)}
        return Modifier.onGloballyPositioned{coords=it}.pointerInput(number) {
            if(number<=0)return@pointerInput
            detectDragGesturesAfterLongPress(onDragStart={local->
                coords?.takeIf{it.isAttached}?.let{position=it.localToRoot(local);dragging=number;selected=null}
            },onDrag={change,delta->change.consume();position+=delta},onDragCancel={dragging=null},onDragEnd={
                val zone=targets.entries.firstOrNull{it.value.contains(position)}?.key
                if(zone!=null)assign(zone,number)
                dragging=null
            })
        }
    }
    Box(Modifier.fillMaxWidth().onGloballyPositioned{origin=it.localToRoot(Offset.Zero)}.testTag("lineup-board")) {
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Tagliando · vista dal fondo verso la rete",style=MaterialTheme.typography.labelLarge)
            Text("Tieni premuto un numero e trascinalo. Oppure tocca il numero, poi la zona. Un numero già in campo scambia posto.",style=MaterialTheme.typography.bodySmall)
            Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("RETE",Modifier.align(Alignment.CenterHorizontally),style=MaterialTheme.typography.labelSmall)
                    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                        drawLine(accent,Offset(0f,0f),Offset(size.width,0f),2.dp.toPx())
                        drawLine(accent,Offset(0f,size.height),Offset(size.width,size.height),1.dp.toPx())
                        var x=0f;while(x<=size.width){drawLine(accent,Offset(x,0f),Offset(x,size.height),1.dp.toPx());x+=8.dp.toPx()}
                    }
                    listOf(listOf(4,3,2),listOf(5,6,1)).forEach { row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach { zone->
                            val number=lineup[zone-1]
                            Surface(shape=MaterialTheme.shapes.small,color=if(hovered==zone)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border=BorderStroke(if(hovered==zone)2.dp else 1.dp,if(hovered==zone)accent else MaterialTheme.colorScheme.outlineVariant),
                                modifier=Modifier.weight(1f).heightIn(min=68.dp).testTag("lineup-slot-$teamIndex-$zone")
                                    .onGloballyPositioned{targets[zone]=it.boundsInRoot()}.then(dragModifier(number))
                                    .semantics{contentDescription="Zona $zone, ${if(number>0)"numero $number" else "vuota"}"}
                                    .clickable{
                                        val pick=selected
                                        if(pick!=null){assign(zone,pick);selected=null}else if(number>0)selected=number
                                    }) {
                                Column(Modifier.padding(6.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                                    Text("ZONA $zone",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if(number>0)"$number" else "+",fontSize=25.sp,fontWeight=FontWeight.Bold,color=accent)
                                }
                            }
                        }
                    }}
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Text(selected?.let{"Numero $it: scegli la zona"} ?: "Numeri disponibili",style=MaterialTheme.typography.labelMedium)
                TextButton(onClick={selected=null;onClear()}){Text("Svuota")}
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                eligible.forEach { number->
                    val assigned=number in lineup
                    Surface(shape=CircleShape,color=if(selected==number || !assigned)accent else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor=if(selected==number || !assigned)teamInk(accent)else MaterialTheme.colorScheme.onSurfaceVariant,
                        border=if(selected==number)BorderStroke(3.dp,MaterialTheme.colorScheme.onSurface)else null,
                        modifier=Modifier.size(48.dp).testTag("lineup-player-$teamIndex-$number").then(dragModifier(number))
                            .semantics{contentDescription="Numero $number${if(assigned)", già in formazione" else ""}"}.clickable{selected=if(selected==number)null else number}) {
                        Box(contentAlignment=Alignment.Center){Text("$number",fontWeight=FontWeight.Bold,fontSize=18.sp)}
                    }
                }
            }
            Text("Solo le presenti, esclusi i liberi e le giocatrici non disponibili.",style=MaterialTheme.typography.bodySmall)
        }
        if(dragging!=null)Surface(shape=CircleShape,color=accent,contentColor=teamInk(accent),shadowElevation=8.dp,
            modifier=Modifier.offset{IntOffset((position.x-origin.x).roundToInt()-24.dp.roundToPx(),(position.y-origin.y).roundToInt()-24.dp.roundToPx())}.size(48.dp).zIndex(10f)) {
            Box(contentAlignment=Alignment.Center){Text("$dragging",fontSize=20.sp,fontWeight=FontWeight.Bold)}
        }
    }
}
