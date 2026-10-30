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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.refertoelettronico.model.*
import kotlinx.coroutines.delay

@Composable
fun LiveScreen(config:MatchConfig,state:MatchState,lastEvent:Event?,onEvent:(Event)->Unit,onUndo:()->Unit,onRedo:()->Unit,
               canUndo:Boolean,canRedo:Boolean,onHistory:()->Unit) {
    var operation by remember { mutableStateOf<String?>(null) }
    var team by remember { mutableIntStateOf(0) }
    val set=state.current
    val playing=set!=null && set.endedAt==null && !state.finished(config)
    val problem=if(playing)MatchEngine.rallyProblem(state) else null
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Column {Text("SET ${set?.number ?: 1}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                Text("Set vinti: ${state.wins(0)} — ${state.wins(1)}",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
            Row {OutlinedButton(onClick=onUndo,enabled=canUndo){Text("↶ Annulla")};Spacer(Modifier.width(6.dp));TextButton(onClick=onRedo,enabled=canRedo){Text("Ripristina")}}
        }
        if(lastEvent!=null)Text("Ultima: ${lastEvent.description(config)}",style=MaterialTheme.typography.bodySmall)
        if(playing && set!=null) Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            for(t in 0..1) Surface(modifier=Modifier.weight(1f),shape=RoundedCornerShape(16.dp),color=config.teams[t].displayColor(t).copy(alpha=.12f),border=BorderStroke(2.dp,config.teams[t].displayColor(t))) {
                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(config.teams[t].name,color=Color(0xFF183E48),fontSize=13.sp,fontWeight=FontWeight.Bold,maxLines=2)
                            Text(if(set.server==t && set.liberos[t]?.replaced==set.court[t][0])"Uscita libero richiesta" else if(set.server==t)"Serve n. ${set.visible(t)[0]}" else "Riceve",fontSize=11.sp)
                        }
                        Text(set.score[t].toString(),modifier=Modifier.testTag("score-$t"),fontSize=30.sp,fontWeight=FontWeight.Bold)
                    }
                    Button(onClick={onEvent(Event(Kind.POINT,team=t))},enabled=problem==null,colors=ButtonDefaults.buttonColors(containerColor=config.teams[t].displayColor(t),contentColor=teamInk(config.teams[t].displayColor(t))),modifier=Modifier.fillMaxWidth().height(48.dp)) {
                        Text("+1 punto",fontSize=18.sp,fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        if(problem!=null)Notice(problem)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(state.finished(config)) {
            Notice(if(state.endedEarly)"Gara terminata anticipatamente. Le decisioni sono nelle osservazioni." else "Gara conclusa · ${config.teams[if(state.wins(0)>state.wins(1))0 else 1].name} vince ${maxOf(state.wins(0),state.wins(1))}:${minOf(state.wins(0),state.wins(1))}. Puoi generare il referto.")
        } else if(!playing && !state.interrupted) {
            if(set!=null)Notice("Set ${set.number} concluso: ${set.score[0]} — ${set.score[1]}. Conferma le formazioni per il prossimo set.")
            StartSetPanel(config,state,onEvent)
        }
        if(playing && set!=null) {
            TimeoutClock(lastEvent,config)
            if(set.changedEndsAt!=null)Text("Quinto set · cambio campo effettuato a ${scoreLabel(set.changedEndsAt)}",color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.bodySmall)
            val left=CourtGeometry.leftTeam(set)
            ScorerCourt(config,state)
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
            OutlinedButton(onClick={operation="note"},modifier=Modifier.weight(1f)){Text("Osservazioni")}
            OutlinedButton(onClick={operation="other"},modifier=Modifier.weight(1f)){Text("Altre operazioni")}
        }
        TextButton(onClick=onHistory,modifier=Modifier.fillMaxWidth()){Text("Apri registro · correggi più operazioni")}
        }
    }
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
    Card(modifier=modifier,border=BorderStroke(if(serves)2.dp else 1.dp,if(serves)Color(0xFFDCA849) else config.teams[team].displayColor(team)),colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFEFA))) {
        Column(Modifier.padding(if(compact)8.dp else 14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${state.letter(team)} · ${config.teams[team].name}",fontWeight=FontWeight.Bold,fontSize=if(compact)13.sp else 18.sp)
                    Text(if(serves)"AL SERVIZIO · n. ${set.visible(team)[0]}" else "IN RICEZIONE",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                }
            }
            @Composable fun controls() {
                OutlinedButton(onClick={onAction("timeout")},modifier=if(compact)Modifier.fillMaxWidth() else Modifier,contentPadding=PaddingValues(8.dp)){Text("Timeout ${set.timeouts.count{it.team==team}}/2",fontSize=12.sp)}
                OutlinedButton(onClick={onAction("sub")},modifier=if(compact)Modifier.fillMaxWidth() else Modifier,contentPadding=PaddingValues(8.dp)){Text("Cambi ${set.countSubs(team)}/${config.maxSubstitutions}",fontSize=12.sp)}
                OutlinedButton(onClick={onAction("libero")},modifier=if(compact)Modifier.fillMaxWidth() else Modifier,contentPadding=PaddingValues(8.dp)){Text("Libero",fontSize=12.sp)}
            }
            if(compact)Column{controls()} else Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){controls()}

        }
    }
}

@Composable
private fun StartSetPanel(config:MatchConfig,state:MatchState,onEvent:(Event)->Unit) {
    val n=state.sets.size+1
    val deciding=MatchEngine.isDeciding(config,n)
    val eligible=config.teams.mapIndexed { t,team ->team.players.filter{it.number !in state.designatedLiberos[t] && it.number !in state.retired[t] && it.number !in state.disqualified[t]}.map{it.number} }
    var lineups by remember(n,state.designatedLiberos,state.retired,state.disqualified) { mutableStateOf(eligible.mapIndexed { t,ns ->
        val prior=state.current?.initial?.get(t)?.filter{it in ns}.orEmpty();(prior+ns.filter{it !in prior}).take(6)
    }) }
    var server by remember(n) { mutableIntStateOf(if(state.current!=null && !deciding)1-state.current!!.firstServer else 0) }
    var left by remember(n) { mutableIntStateOf(if(state.current!=null && !deciding)1-state.current!!.initialLeft else 0) }
    OutlinedCard {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Formazioni · set $n",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Text("Inserisci le zone 1–6 del tagliando. La disposizione è vista dal tavolo del segnapunti: le due formazioni sono opposte.",style=MaterialTheme.typography.bodySmall)
            if(eligible.any{it.size<6})Notice("Meno di sei giocatrici disponibili: registra la decisione dell'arbitro nelle altre operazioni.")
            else {
                for(t in 0..1) {
                    Text(config.teams[t].name,fontWeight=FontWeight.Bold)
                    CourtGeometry.rows(if(t==left)CourtEnd.LEFT else CourtEnd.RIGHT).forEach { row ->
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {row.forEach {zone ->
                            val pos=zone-1
                            Picker("Zona ${pos+1}",lineups[t][pos],eligible[t],{num ->"$num"},{num ->lineups=lineups.with(t,lineups[t].with(pos,num))},Modifier.weight(1f))
                        }}
                    }
                }
                if(n==1 || deciding) {
                    Picker("Primo servizio · sorteggio",server,listOf(0,1),{config.teams[it].name},{server=it})
                    Picker("Squadra a sinistra · sorteggio",left,listOf(0,1),{config.teams[it].name},{left=it})
                    if(n==1)Text("La squadra inizialmente a sinistra sarà A sul referto, l'altra B.",style=MaterialTheme.typography.bodySmall)
                } else Text("Primo servizio: ${config.teams[server].name}. Cambio dei lati automatico.")
                Button(onClick={onEvent(Event(Kind.START_SET,lineups=lineups,server=server,left=left))},modifier=Modifier.fillMaxWidth()) {Text("Conferma e inizia il set $n")}
            }
        }
    }
}

@Composable
private fun OperationDialog(action:String,initialTeam:Int,config:MatchConfig,state:MatchState,onDismiss:()->Unit,onEvent:(Event)->Unit) {
    var team by remember(action){mutableIntStateOf(initialTeam)}
    val set=state.current
    val regular=set?.court?.get(team).orEmpty()
    val libero=set?.liberos?.get(team)
    val players=config.teams[team].players.map{it.number}
    var outgoing by remember(action,team){mutableIntStateOf(regular.firstOrNull()?:players.first())}
    var incoming by remember(action,team){mutableIntStateOf(players.firstOrNull{it !in regular && it !in state.designatedLiberos[team]}?:players.first())}
    var selectedLibero by remember(action,team){mutableIntStateOf(state.designatedLiberos[team].firstOrNull{it!=libero?.number}?:state.designatedLiberos[team].firstOrNull()?:0)}
    var replaced by remember(action,team){mutableIntStateOf(libero?.replaced ?: regular.getOrElse(4){regular.firstOrNull()?:0})}
    var text by remember(action){mutableStateOf("")}
    var recipient by remember(action){mutableStateOf("")}
    var card by remember(action){mutableStateOf(Card.WARNING)}
    var localError by remember(action){mutableStateOf<String?>(null)}
    var liberoExit by remember(action){mutableStateOf(false)}
    val title=when(action) {
        "sub"->"Sostituzione";"exception"->"Sostituzione eccezionale";"libero"->"Rimpiazzo del libero";"timeout"->"Timeout"
        "sanction"->"Sanzione";"redesignate"->"Ridesignazione libero";"captain"->"Nuova capitana";"unavailable"->"Inabilità al gioco"
        "interrupt"->"Interrompi la gara";"resume"->"Riprendi la gara";"end"->"Termine anticipato";else->"Osservazioni"
    }
    fun send(e:Event) {try {MatchEngine.apply(config,state,e);onEvent(e)}catch(t:IllegalArgumentException){localError=t.message}}
    AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={
        Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
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
    },confirmButton={TextButton(onClick={
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
        Text("Registro della gara",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.padding(vertical=14.dp))
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
