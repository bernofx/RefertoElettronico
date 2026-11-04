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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import it.refertoelettronico.R
import it.refertoelettronico.model.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StyledSetPreparation(config:MatchConfig,state:MatchState,lineups:List<List<Int>>,eligible:List<List<Int>>,
    team:Int,onTeam:(Int)->Unit,onAssign:(Int,Int)->Unit,onClear:()->Unit,server:Int,left:Int,onServer:(Int)->Unit,onLeft:(Int)->Unit,
    onStart:()->Unit,fillViewport:Boolean,inlineToss:Boolean=false,onCopyPrevious:(()->Unit)?,onEditSection:((Int)->Unit)?,journal:Journal?,onSignatures:(()->Unit)?) {
    val n=state.sets.size+1
    val newToss=n==1 || MatchEngine.isDeciding(config,n)
    var tossOpen by remember{mutableStateOf(false)}
    val complete=lineups.all{it.count{v->v>0}==6 && it.distinct().size==6}
    val currentReady=lineups[team].count{it>0}==6 && lineups[team].distinct().size==6
    val validToss=server in 0..1 && left in 0..1
    val status=PreparationProgress.steps(config)
    val last=state.current?.takeIf{it.endedAt!=null}
    Column((if(fillViewport)Modifier.fillMaxSize()else Modifier.fillMaxWidth()).testTag("styled-set-preparation")) {
        Column((if(fillViewport)Modifier.weight(1f).verticalScroll(rememberScrollState())else Modifier)
            .padding(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            CompactMatchSummary(config,last?.let{"Set ${it.number} concluso · ${it.score[0]}–${it.score[1]} · ${state.wins(0)}–${state.wins(1)} set"}.orEmpty())
            Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                listOf(Triple("Dati",0,status[0].complete),Triple("Rose",1,status[1].complete&&status[2].complete),Triple("Ufficiali",3,status[3].complete)).forEach{(label,section,ready)->
                    OutlinedButton(onClick={onEditSection?.invoke(section)},enabled=onEditSection!=null,
                        modifier=Modifier.weight(1f).heightIn(min=36.dp).testTag("formation-edit-$section"),contentPadding=PaddingValues(horizontal=7.dp,vertical=4.dp),shape=MaterialTheme.shapes.small) {
                        UiGlyph(if(ready)UiSymbol.CHECK else UiSymbol.EDIT,Modifier.size(14.dp),if(ready)styleSuccess()else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp));Text(label,style=MaterialTheme.typography.labelMedium)
                    }
                }
            }
            OutlinedCard(onClick={tossOpen=true},modifier=Modifier.fillMaxWidth().testTag("formation-toss")) {
                Row(Modifier.padding(horizontal=10.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(!validToss)"${if(newToss)"Sorteggio"else"Servizio e campo"} · da completare"else
                        "Serve ${shortTeamName(config.teams[server].name)} · Sinistra ${shortTeamName(config.teams[left].name)}",
                        style=MaterialTheme.typography.bodySmall,modifier=Modifier.weight(1f))
                    UiGlyph(UiSymbol.CHEVRON,Modifier.size(16.dp))
                }
            }
            if(inlineToss&&newToss) {
                Text("Chi serve per primo?",style=MaterialTheme.typography.labelMedium)
                Box(Modifier.tourTarget("toss-serve")){CompactTeamTabs(config,server,"toss-serve",onSelect=onServer)}
                Text("Chi è a sinistra del tavolo?",style=MaterialTheme.typography.labelMedium)
                Box(Modifier.tourTarget("toss-left")){CompactTeamTabs(config,left,"toss-left",onSelect=onLeft)}
            }
            if(n==1 && journal!=null && onSignatures!=null) {
                val toss=Event(Kind.START_SET,lineups=lineups,server=server,left=left)
                val roles=Signatures.required(config,true);val missing=Signatures.missing(journal,true,toss)
                Row(Modifier.fillMaxWidth().tourTarget("pre-signatures"),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("Firme iniziali · ${roles.size-missing.size}/${roles.size}",style=MaterialTheme.typography.labelSmall)
                    TextButton(enabled=validToss,onClick=onSignatures,contentPadding=PaddingValues(horizontal=8.dp,vertical=0.dp)){Text(if(missing.isEmpty())"Controlla"else"Raccogli",style=MaterialTheme.typography.labelMedium)}
                }
            }
            CompactTeamTabs(config,team,"lineup-team",detail={t->"${lineups[t].count{it>0}}/6"},onSelect=onTeam)
            key(n,team){StyledLineupBoard(team,config.teams[team],eligible[team],lineups[team],onAssign,onClear,onCopyPrevious)}
            if(eligible.any{it.size<6})Text("Meno di sei giocatrici disponibili: verifica la decisione arbitrale.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
        }
        Surface(color=MaterialTheme.colorScheme.background) {
            Button(enabled=currentReady && eligible.all{it.size>=6},onClick={
                if(!complete)onTeam(1-team) else if(!validToss)tossOpen=true else onStart()
            },modifier=Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp).heightIn(min=48.dp)
                .tourTarget("start-set").testTag("start-set"),colors=styleButtonColors(),shape=MaterialTheme.shapes.medium) {
                UiGlyph(UiSymbol.CHECK,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))
                Text(if(!complete)"Conferma formazione"else if(!validToss)"Completa il sorteggio"else"Conferma e inizia il set $n")
            }
        }
    }
    if(tossOpen)ModalBottomSheet(onDismissRequest={tossOpen=false},sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().padding(horizontal=16.dp).navigationBarsPadding(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(if(newToss)"Esito del sorteggio"else"Servizio e campo · set $n",style=MaterialTheme.typography.titleLarge)
            if(newToss) {
                Text("Chi serve per primo?",style=MaterialTheme.typography.labelLarge)
                Box(Modifier.tourTarget("toss-serve")){CompactTeamTabs(config,server,"toss-serve",onSelect=onServer)}
                Text("Chi è a sinistra del tavolo?",style=MaterialTheme.typography.labelLarge)
                Box(Modifier.tourTarget("toss-left")){CompactTeamTabs(config,left,"toss-left",onSelect=onLeft)}
            } else {
                Text("Servizio e lati si alternano rispetto al set precedente.",style=MaterialTheme.typography.bodyMedium)
                Text("Serve: ${config.teams[server].name}\nA sinistra del tavolo: ${config.teams[left].name}")
            }
            Button(onClick={tossOpen=false},modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),colors=styleButtonColors()){Text("Fatto")}
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyledLineupBoard(teamIndex:Int,team:Team,eligible:List<Int>,lineup:List<Int>,onAssign:(Int,Int)->Unit,onClear:()->Unit,onCopy:(()->Unit)?) {
    val assign by rememberUpdatedState(onAssign)
    var selected by remember{mutableStateOf<Int?>(null)}
    var dragging by remember{mutableStateOf<Int?>(null)}
    var position by remember{mutableStateOf(Offset.Zero)}
    var origin by remember{mutableStateOf(Offset.Zero)}
    var menu by remember{mutableStateOf(false)}
    val targets=remember{mutableStateMapOf<Int,Rect>()}
    val accent=team.displayColor(teamIndex)
    val ink=teamInk(accent)
    @Composable fun draggable(number:Int):Modifier {
        var coords by remember{mutableStateOf<LayoutCoordinates?>(null)}
        return Modifier.onGloballyPositioned{coords=it}.pointerInput(number) {
            if(number<=0)return@pointerInput
            detectDragGesturesAfterLongPress(onDragStart={local->coords?.takeIf{it.isAttached}?.let{position=it.localToRoot(local);dragging=number;selected=null}},
                onDrag={change,delta->change.consume();position+=delta},onDragCancel={dragging=null},onDragEnd={
                    targets.entries.firstOrNull{it.value.contains(position)}?.key?.let{assign(it,number)};dragging=null
                })
        }
    }
    val hovered=if(dragging==null)null else targets.entries.firstOrNull{it.value.contains(position)}?.key
    val height=LocalConfiguration.current.screenHeightDp
    val compact=height<760
    val largeCourt=LocalAppearance.current.layout==InterfaceStyle.COURT && height>=820
    val jersey=if(compact)54.dp else if(largeCourt)80.dp else 60.dp
    val slotHeight=maxOf(if(compact)72.dp else if(largeCourt)112.dp else 84.dp,(jersey.value+18*LocalDensity.current.fontScale).dp)
    val field=lerp(MaterialTheme.colorScheme.surfaceContainer,MaterialTheme.colorScheme.primaryContainer,.38f)
    val lines=MaterialTheme.colorScheme.onSurface.copy(alpha=.65f)
    Box(Modifier.fillMaxWidth().onGloballyPositioned{origin=it.localToRoot(Offset.Zero)}.testTag("lineup-board")) {
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainer,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("Formazione iniziale",style=MaterialTheme.typography.titleSmall)
                        Text("${lineup.count{it>0}}/6 in campo",style=MaterialTheme.typography.labelMedium,color=styleSuccess())
                    }
                    Box(Modifier.fillMaxWidth().tourTarget("lineup-grid").testTag("styled-jersey-grid")) {
                        Canvas(Modifier.matchParentSize()) {
                            val side=10.dp.toPx();val top=24.dp.toPx()
                            val path=Path().apply{moveTo(side,top);lineTo(size.width-side,top);lineTo(size.width,size.height);lineTo(0f,size.height);close()}
                            drawPath(path,field);drawPath(path,lines,style=Stroke(1.3.dp.toPx()))
                            drawLine(lines,Offset(side/2,(size.height+top)/2),Offset(size.width-side/2,(size.height+top)/2),1.dp.toPx())
                        }
                        Column {
                            Canvas(Modifier.fillMaxWidth().height(26.dp).testTag("lineup-net-$teamIndex")) {
                                val y=3.dp.toPx();val bottom=21.dp.toPx();val edge=4.dp.toPx()
                                for(level in 0..3){val h=y+(bottom-y)*level/3;drawLine(lines,Offset(edge,h),Offset(size.width-edge,h),.7.dp.toPx())}
                                var x=edge;while(x<size.width-edge){drawLine(lines,Offset(x,y),Offset(x,bottom),.6.dp.toPx());x+=6.dp.toPx()}
                                drawLine(lines,Offset(edge,0f),Offset(edge,size.height),3.dp.toPx());drawLine(lines,Offset(size.width-edge,0f),Offset(size.width-edge,size.height),3.dp.toPx())
                            }
                            listOf(listOf(4,3,2),listOf(5,6,1)).forEach{row->
                                Row(Modifier.padding(horizontal=10.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    row.forEach{zone->
                                        val number=lineup[zone-1]
                                        Column(Modifier.weight(1f).height(slotHeight)
                                            .background(if(hovered==zone)MaterialTheme.colorScheme.primaryContainer else Color.Transparent,MaterialTheme.shapes.small)
                                            .onGloballyPositioned{targets[zone]=it.boundsInRoot()}.then(draggable(number))
                                            .semantics{contentDescription="Zona $zone, ${if(number>0)"numero $number"else"vuota"}"}
                                            .testTag("lineup-slot-$teamIndex-$zone").clickable{
                                                selected?.let{assign(zone,it);selected=null}?:run{if(number>0)selected=number}
                                            },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                                            Text("Zona $zone",style=MaterialTheme.typography.labelSmall)
                                            Box(Modifier.size(jersey),contentAlignment=Alignment.Center) {
                                                Image(painterResource(R.drawable.ic_jersey_fill),null,Modifier.fillMaxSize(),colorFilter=ColorFilter.tint(if(number>0)accent else field))
                                                Image(painterResource(R.drawable.ic_jersey_outline),null,Modifier.fillMaxSize(),colorFilter=ColorFilter.tint(if(number>0)ink else lines))
                                                Text(if(number>0)"$number"else"—",Modifier.offset(y=5.dp).testTag("lineup-slot-number-$teamIndex-$zone"),fontSize=(if(largeCourt)26 else 23).sp,fontWeight=FontWeight.Bold,color=if(number>0)ink else MaterialTheme.colorScheme.onSurfaceVariant)
                                                if(team.players.any{it.number==number&&it.captain})Surface(shape=CircleShape,color=Color(0xFFFFD23F),contentColor=Color.Black,modifier=Modifier.size(20.dp).align(Alignment.TopEnd)) {
                                                    Box(contentAlignment=Alignment.Center){Text("C",fontSize=12.sp,fontWeight=FontWeight.Bold)}
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text(selected?.let{"n. $it · scegli la zona"}?:"Numeri disponibili",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                if(onCopy!=null)TextButton(onClick={selected=null;onCopy()},contentPadding=PaddingValues(horizontal=4.dp,vertical=0.dp),modifier=Modifier.testTag("copy-previous")){Text("Copia precedente",style=MaterialTheme.typography.labelSmall)}
                Box {
                    IconButton(onClick={menu=true},modifier=Modifier.size(36.dp).semantics{contentDescription="Opzioni formazione"}){UiGlyph(UiSymbol.SETTINGS,Modifier.size(16.dp))}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        DropdownMenuItem(text={Text("Svuota formazione")},onClick={menu=false;selected=null;onClear()})
                    }
                }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                eligible.filter{it !in lineup}.forEach{number->
                    Surface(color=if(selected==number)accent else MaterialTheme.colorScheme.surfaceContainerHigh,contentColor=if(selected==number)ink else MaterialTheme.colorScheme.onSurface,
                        shape=MaterialTheme.shapes.small,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline),modifier=Modifier.size(42.dp).testTag("lineup-player-$teamIndex-$number").then(draggable(number))
                            .semantics{contentDescription="Numero $number"}.clickable{selected=if(selected==number)null else number}) {
                        Box(contentAlignment=Alignment.Center){Text("$number",style=MaterialTheme.typography.titleMedium)}
                    }
                }
            }
            Text("C = capitana · Trascina o tocca numero e zona",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if(dragging!=null)Surface(color=accent,contentColor=ink,shape=CircleShape,modifier=Modifier.offset{IntOffset((position.x-origin.x).roundToInt()-22.dp.roundToPx(),(position.y-origin.y).roundToInt()-22.dp.roundToPx())}.size(44.dp).zIndex(10f)) {
            Box(contentAlignment=Alignment.Center){Text("$dragging",fontSize=22.sp,fontWeight=FontWeight.Bold)}
        }
    }
}
