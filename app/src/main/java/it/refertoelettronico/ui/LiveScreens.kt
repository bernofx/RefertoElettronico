package it.refertoelettronico.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.refertoelettronico.model.*
import kotlinx.coroutines.delay

@Composable
fun LiveScreen(config:MatchConfig,state:MatchState,lastEvent:Event?,onEvent:(Event)->Unit,onUndo:()->Unit,onRedo:()->Unit,
               canUndo:Boolean,canRedo:Boolean,onHistory:()->Unit,onEditSetup:(()->Unit)?=null,journal:Journal?=null,onSignatures:((Journal)->Boolean)?=null,onFinalSignatures:(()->Unit)?=null) {
    var signatureStart by remember{mutableStateOf<Event?>(null)}
    var startAfterSigning by remember{mutableStateOf(false)}
    var operation by remember { mutableStateOf<String?>(null) }
    var team by remember { mutableIntStateOf(0) }
    val set=state.current
    val leftTeam=set?.let{CourtGeometry.leftTeam(it)} ?: 0
    val playing=set!=null && set.endedAt==null && !state.finished(config)
    val problem=if(playing)MatchEngine.rallyProblem(state) else null
    Column(Modifier.fillMaxSize().padding(horizontal=12.dp,vertical=4.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("Set ${set?.number ?: 1} · ${state.wins(leftTeam)}–${state.wins(1-leftTeam)} set",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
            Row {TextButton(onClick=onUndo,enabled=canUndo,contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.tourTarget("undo")){Text("↶ Annulla")};TextButton(onClick=onRedo,enabled=canRedo,contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.tourTarget("redo")){Text("Ripristina")}}
        }
        if(playing && set!=null) Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            for(t in listOf(leftTeam,1-leftTeam)) key(t) {
                val accent=config.teams[t].displayColor(t)
                Surface(modifier=Modifier.weight(1f).testTag("point-card-$t"),shape=MaterialTheme.shapes.large,color=MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(horizontal=8.dp,vertical=6.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                        Text("${if(t==0)"Casa"else"Ospiti"} · ${config.teams[t].name}",style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(set.score[t].toString(),modifier=Modifier.testTag("score-$t"),fontSize=32.sp,lineHeight=36.sp,fontWeight=FontWeight.Bold)
                            Button(onClick={onEvent(Event(Kind.POINT,team=t))},enabled=problem==null,
                                colors=ButtonDefaults.buttonColors(containerColor=accent,contentColor=teamInk(accent)),
                                contentPadding=PaddingValues(horizontal=8.dp),modifier=Modifier.weight(1f).height(48.dp).tourTarget("point-$t").testTag("point-button-$t"),shape=MaterialTheme.shapes.small) {
                                Text("+1 punto",fontSize=14.sp,fontWeight=FontWeight.Bold,maxLines=1)
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.weight(1f).testTag("live-scroll-region").verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(lastEvent!=null)Text("Ultima: ${lastEvent.description(config)}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        if(problem!=null)Notice(problem)
        if(state.finished(config)) {
            Notice(if(state.endedEarly)"Gara terminata anticipatamente. Le decisioni sono nelle osservazioni." else "Gara conclusa · ${config.teams[if(state.wins(0)>state.wins(1))0 else 1].name} vince ${maxOf(state.wins(0),state.wins(1))}:${minOf(state.wins(0),state.wins(1))}. Puoi generare il referto.")
            if(onFinalSignatures!=null)Button(onClick=onFinalSignatures,modifier=Modifier.fillMaxWidth()){Text("Firme e approvazione finale")}
        } else if(!playing && !state.interrupted) {
            if(state.sets.isEmpty() && onEditSetup!=null)FilledTonalButton(onClick=onEditSetup,modifier=Modifier.fillMaxWidth().testTag("edit-pre-match")) {
                Text("← Correggi dati e rose")
            }
            if(set!=null)Notice("Set ${set.number} concluso: ${set.score[0]} — ${set.score[1]}. Conferma le formazioni per il prossimo set.")
            StartSetPanel(config,state,onEvent,signatureJournal=journal,
                onCollectSignatures=if(onSignatures!=null)({event->startAfterSigning=false;signatureStart=event})else null,
                onBeforeStart=if(onSignatures!=null && state.sets.isEmpty())({event->
                    if(journal!=null && Signatures.missing(journal,true,event).isEmpty())onEvent(event)
                    else {startAfterSigning=true;signatureStart=event}
                })else null)
        }
        if(playing && set!=null) {
            TimeoutClock(lastEvent,config)
            if(set.changedEndsAt!=null)Text("Quinto set · cambio campo effettuato a ${scoreLabel(set.changedEndsAt)}",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall)
            val left=CourtGeometry.leftTeam(set)
            ScorerCourt(config,state,Modifier.tourTarget("court"))
            BoxWithConstraints {
                val compact=maxWidth<650.dp
                Row(horizontalArrangement=Arrangement.spacedBy(if(compact)8.dp else 14.dp)) {
                    listOf(left,1-left).forEach { t ->TeamActions(config,state,t,{action ->team=t;operation=action},Modifier.weight(1f),compact)}
                }
            }
            Text("Pallone sul battitore · L = libero. Le posizioni sono viste dal tavolo del segnapunti.",style=MaterialTheme.typography.bodySmall)
        }
        if(state.interrupted)Notice("Gara interrotta. Registra la ripresa prima di continuare a segnare punti.")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick={operation="note"},modifier=Modifier.weight(1f).tourTarget("notes")){Text("Osservazioni")}
            OutlinedButton(onClick={operation="other"},modifier=Modifier.weight(1f).tourTarget("other")){Text("Altre operazioni")}
        }
        TextButton(onClick=onHistory,modifier=Modifier.fillMaxWidth()){Text("Apri registro · correggi più operazioni")}
        }
    }
    val pending=signatureStart
    if(pending!=null && journal!=null && onSignatures!=null)SignatureWorkflow(journal,true,pending,onSignatures,
        onClose={signatureStart=null},onStart=if(startAfterSigning)({signatureStart=null;onEvent(pending.copy(at=System.currentTimeMillis()))})else null)
    if(operation=="other")AlertDialog(onDismissRequest={operation=null},title={Text("Altre operazioni")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            if(playing) {
                listOf("sanction" to "Sanzione / richiesta impropria","exception" to "Sostituzione eccezionale","redesignate" to "Ridesignazione libero","captain" to "Nuova capitana","unavailable" to "Inabilità giocatrice / libero").forEach { (key,label) ->
                    TextButton(onClick={operation=key}){Text(label)}
                }
            }
            TextButton(onClick={operation=if(state.interrupted)"resume" else "interrupt"}){Text(if(state.interrupted)"Ripresa della gara" else "Interruzione della gara")}
            if(!state.finished(config))TextButton(onClick={operation="end"}){Text("Termina gara per decisione arbitrale")}
        }
    },confirmButton={TextButton(onClick={operation=null}){Text("Chiudi")}})
    else if(operation!=null)OperationDialog(operation!!,team,config,state,onDismiss={operation=null},onEvent={onEvent(it);operation=null})
}

@Composable
private fun TimeoutClock(event:Event?,config:MatchConfig) {
    if(event?.kind!=Kind.TIMEOUT)return
    var remaining by remember(event) { mutableIntStateOf(((event.at+30_000-System.currentTimeMillis())/1000).toInt().coerceIn(0,30)) }
    LaunchedEffect(event) { while(remaining>0){delay(1000);remaining=((event.at+30_000-System.currentTimeMillis()+999)/1000).toInt().coerceIn(0,30)} }
    if(remaining>0)Notice("Timeout ${config.teams[event.team].name} · ${remaining}s")
}

@Composable
private fun TeamActions(config:MatchConfig,state:MatchState,team:Int,onAction:(String)->Unit,modifier:Modifier,compact:Boolean) {
    val set=state.current!!
    val serves=set.server==team
    Card(modifier=modifier,shape=MaterialTheme.shapes.large,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(if(compact)8.dp else 14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${state.letter(team)} · ${config.teams[team].name}",fontWeight=FontWeight.Bold,fontSize=if(compact)13.sp else 18.sp)
                    Text(if(serves)"AL SERVIZIO · n. ${set.visible(team)[0]}" else "IN RICEZIONE",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                }
            }
            @Composable fun controls() {
                FilledTonalButton(onClick={onAction("timeout")},modifier=(if(compact)Modifier.fillMaxWidth() else Modifier).tourTarget("timeout-$team"),contentPadding=PaddingValues(8.dp)){Text("Timeout ${set.timeouts.count{it.team==team}}/2",fontSize=12.sp)}
                FilledTonalButton(onClick={onAction("sub")},modifier=(if(compact)Modifier.fillMaxWidth() else Modifier).tourTarget("sub-$team"),contentPadding=PaddingValues(8.dp)){Text("Cambi ${set.countSubs(team)}/${config.maxSubstitutions}",fontSize=12.sp)}
                FilledTonalButton(onClick={onAction("libero")},modifier=(if(compact)Modifier.fillMaxWidth() else Modifier).tourTarget("libero-$team"),contentPadding=PaddingValues(8.dp)){Text("Libero",fontSize=12.sp)}
            }
            if(compact)Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                FilledTonalButton(onClick={onAction("timeout")},modifier=Modifier.fillMaxWidth().tourTarget("timeout-$team"),contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)) {
                    UiGlyph(UiSymbol.CLOCK,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("Timeout ${set.timeouts.count{it.team==team}}/2",fontSize=12.sp)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(onClick={onAction("sub")},modifier=Modifier.weight(1.15f).tourTarget("sub-$team"),contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)) {
                        Text("Cambi ${set.countSubs(team)}/${config.maxSubstitutions}",fontSize=12.sp,maxLines=1)
                    }
                    FilledTonalButton(onClick={onAction("libero")},modifier=Modifier.weight(1f).tourTarget("libero-$team"),contentPadding=PaddingValues(horizontal=4.dp,vertical=6.dp)){Text("Libero",fontSize=12.sp)}
                }
            } else Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){controls()}

        }
    }
}

@Composable
internal fun OperationDialog(action:String,initialTeam:Int,config:MatchConfig,state:MatchState,onDismiss:()->Unit,onEvent:(Event)->Unit,embedded:Boolean=false,exampleText:String="",exampleRecipient:String="") {
    var team by remember(action){mutableIntStateOf(initialTeam)}
    val set=state.current
    val regular=set?.court?.get(team).orEmpty()
    val libero=set?.liberos?.get(team)
    val players=config.teams[team].players.map{it.number}
    var outgoing by remember(action,team){mutableIntStateOf(regular.firstOrNull()?:players.first())}
    var incoming by remember(action,team){mutableIntStateOf(players.firstOrNull{it !in regular && it !in state.designatedLiberos[team]}?:players.first())}
    var selectedLibero by remember(action,team){mutableIntStateOf(state.designatedLiberos[team].firstOrNull{it!=libero?.number}?:state.designatedLiberos[team].firstOrNull()?:0)}
    var replaced by remember(action,team){mutableIntStateOf(libero?.replaced ?: regular.getOrElse(4){regular.firstOrNull()?:0})}
    var text by remember(action){mutableStateOf(exampleText)}
    var recipient by remember(action){mutableStateOf(exampleRecipient)}
    var card by remember(action){mutableStateOf(Card.WARNING)}
    var localError by remember(action){mutableStateOf<String?>(null)}
    var liberoExit by remember(action){mutableStateOf(false)}
    val title=when(action) {
        "sub"->"Sostituzione";"exception"->"Sostituzione eccezionale";"libero"->"Rimpiazzo del libero";"timeout"->"Timeout"
        "sanction"->"Sanzione";"redesignate"->"Ridesignazione libero";"captain"->"Nuova capitana";"unavailable"->"Inabilità al gioco"
        "interrupt"->"Interrompi la gara";"resume"->"Riprendi la gara";"end"->"Termine anticipato";else->"Osservazioni"
    }
    fun send(e:Event) {try {MatchEngine.apply(config,state,e);onEvent(e)}catch(t:IllegalArgumentException){localError=t.message}}
    OperationFrame(embedded,onDismiss,title={Text(title)},text={
        Column(Modifier.tourTarget("operation-form").heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            if(action !in listOf("note","interrupt","resume","end"))Picker("Squadra",team,listOf(0,1),{config.teams[it].name},{team=it})
            fun label(n:Int) = "$n · ${config.teams[team].players.find{it.number==n}?.name.orEmpty()}"
            when(action) {
                "sub","exception" -> {
                    Picker("Esce",outgoing,regular,::label,{outgoing=it})
                    Picker("Entra",incoming,players.filter{it !in regular && it !in state.designatedLiberos[team]},::label,{incoming=it})
                    Text("Punteggio ${set?.score?.let{scoreLabel(it,team)}} · ${set?.countSubs(team)}/${config.maxSubstitutions} cambi usati")
                    if(action=="exception")Notice("Solo per decisione dell'arbitro. La giocatrice uscente non potrà rientrare nella gara.")
                }
                "libero" -> {
                    if(libero!=null) {
                        Notice("In campo: libero ${libero.number}, al posto di ${libero.replaced}.")
                        Row(verticalAlignment=Alignment.CenterVertically){Switch(checked=liberoExit,onCheckedChange={liberoExit=it});Text("Registra uscita del libero")}
                    }
                    if(!liberoExit) {
                        if(state.designatedLiberos[team].isEmpty())Notice("Nessun libero designato per questa squadra.")
                        else Picker("Libero che entra",selectedLibero,state.designatedLiberos[team],::label,{selectedLibero=it})
                        if(libero==null)Picker("Giocatrice rimpiazzata",replaced,listOf(0,4,5).mapNotNull{regular.getOrNull(it)},::label,{replaced=it})
                    }
                    Text("I rimpiazzi non contano come sostituzioni. Un'uscita forzata senza azione richiede una motivazione.",style=MaterialTheme.typography.bodySmall)
                }
                "timeout" ->Text("${set?.timeouts?.count{it.team==team}?:0} timeout già usati. Registra la richiesta sul ${set?.score?.let{scoreLabel(it,team)}}.")
                "sanction" -> {
                    Picker("Provvedimento dell'arbitro",card,Card.entries.toList(),{it.label},{card=it})
                    Field("Destinatario: n° maglia o ruolo (A, VA, M…)",recipient,{recipient=it})
                    if(card.penalty)Notice("Verrà assegnato automaticamente un punto e il servizio alla squadra avversaria.")
                }
                "redesignate" -> {
                    Picker("Libero dichiarato inabile",selectedLibero,state.designatedLiberos[team],::label,{selectedLibero=it})
                    Picker("Nuovo libero",incoming,players,::label,{incoming=it})
                    Notice("Registra prima l'uscita forzata del libero. La ridesignazione richiede la decisione dell'arbitro.")
                }
                "unavailable" ->Picker("Giocatrice dichiarata inabile",outgoing,players,::label,{outgoing=it})
                "captain" ->Picker("Nuova capitana",incoming,players,::label,{incoming=it})
                "end" ->Notice("Questa operazione chiude la gara nello stato attuale senza inventare un risultato a tavolino. Trascrivi la decisione dell'arbitro; puoi annullare dal Registro.")
            }
            if(action in listOf("exception","redesignate","note","interrupt","resume","end","libero","captain","unavailable"))
                OutlinedTextField(value=text,onValueChange={text=it},label={Text(if(action=="note")"Annotazione" else "Motivo / decisione dell'arbitro")},minLines=2,modifier=Modifier.fillMaxWidth())
            if(localError!=null)Text(localError!!,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={TextButton(modifier=Modifier.tourTarget("operation-confirm"),onClick={
        val event=when(action) {
            "sub"->Event(Kind.SUBSTITUTION,team,outgoing,incoming)
            "exception"->Event(Kind.EXCEPTIONAL_SUB,team,outgoing,incoming,text=text)
            "libero"->if(liberoExit)Event(Kind.LIBERO_OUT,team,text=text) else Event(Kind.LIBERO_IN,team,selectedLibero,replaced,text=text)
            "timeout"->Event(Kind.TIMEOUT,team)
            "sanction"->Event(Kind.SANCTION,team,player=recipient.trim().toIntOrNull()?:0,text=recipient.trim(),card=card)
            "redesignate"->Event(Kind.REDESIGNATE_LIBERO,team,selectedLibero,incoming,text=text)
            "unavailable"->Event(Kind.UNAVAILABLE,team,player=outgoing,text=text)
            "captain"->Event(Kind.CAPTAIN,team,player=incoming,text=text)
            "interrupt"->Event(Kind.INTERRUPT,text=text)
            "resume"->Event(Kind.RESUME,text=text)
            "end"->Event(Kind.END_MATCH,text=text)
            else->Event(Kind.NOTE,text=text)
        };send(event)
    }){Text("Registra")}},dismissButton={TextButton(onClick=onDismiss){Text("Chiudi")}})
}

@Composable
fun HistoryScreen(journal:Journal,onUndoTo:(Int)->Unit,onRedo:()->Unit) {
    val rows=remember(journal) {
        var s=MatchEngine.initial(journal.config)
        journal.events.map { e -> s=MatchEngine.apply(journal.config,s,e);Triple(e,s.current?.number?:0,s.current?.score?:listOf(0,0)) }
    }
    Column(Modifier.fillMaxSize().padding(horizontal=18.dp)) {
        Text("Registro della gara",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=14.dp).tourTarget("history"))
        Text("Per correggere un errore, annulla da quell'operazione in avanti e reinserisci gli eventi corretti. Le rotazioni vengono ricostruite insieme al punteggio.",style=MaterialTheme.typography.bodyMedium)
        if(journal.redo.isNotEmpty())TextButton(onClick=onRedo){Text("Ripristina operazione · ${journal.redo.size} disponibili")}
        if(rows.isEmpty())Text("Nessuna operazione registrata.",Modifier.padding(20.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=16.dp)) {
            itemsIndexed(rows.asReversed()) { reversed,row ->
                val index=rows.size-1-reversed;val (e,set,score)=row
                OutlinedCard {
                    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text("${index+1} · ${clock(e.at)} · set $set · ${scoreLabel(score)}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                        Text(e.description(journal.config),fontWeight=FontWeight.Medium)
                        TextButton(onClick={onUndoTo(journal.events.size-index)}){Text("Annulla da qui (${journal.events.size-index})")}
                    }
                }
            }
        }
    }
}

/** The tour embeds the very same operation form in its own window, so spotlight bounds share a root. */
@Composable
private fun OperationFrame(embedded:Boolean,onDismiss:()->Unit,title:@Composable ()->Unit,text:@Composable ()->Unit,
                           confirmButton:@Composable ()->Unit,dismissButton:@Composable ()->Unit) {
    if(!embedded)AlertDialog(onDismissRequest=onDismiss,title=title,text=text,confirmButton=confirmButton,dismissButton=dismissButton)
    else Box(Modifier.fillMaxSize().padding(16.dp),contentAlignment=Alignment.Center) {
        Surface(shape=MaterialTheme.shapes.large,tonalElevation=6.dp) {
            Column(Modifier.widthIn(max=480.dp).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                title();Box(Modifier.weight(1f,fill=false)){text()}
                Row(Modifier.align(Alignment.End)){dismissButton();confirmButton()}
            }
        }
    }
}
