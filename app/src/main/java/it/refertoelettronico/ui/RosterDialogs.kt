package it.refertoelettronico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.refertoelettronico.model.*
import it.refertoelettronico.storage.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SaveRosterDialog(team:Team,season:String,onClose:()->Unit,onSaved:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    val store=remember{RosterStore(File(context.filesDir,"rosters"))}
    var label by remember{mutableStateOf("${team.name} · $season".take(120))}
    var existing by remember{mutableStateOf<List<SavedRoster>>(emptyList())}
    var busy by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(Unit){try{existing=withContext(Dispatchers.IO){store.list().mapNotNull{it.second.getOrNull()}}}catch(t:Exception){error=t.message};busy=false}
    val target=existing.firstOrNull{it.label.equals(label.trim(),true)}
    RosterDialog(onDismissRequest={if(!busy)onClose()},title={Text("Salva rosa riutilizzabile")},text={
        Column(Modifier.fillMaxWidth().heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Field("Nome della rosa",label,{if(!busy)label=it.take(120)})
            Text("${Rosters.template(team).players.size} giocatrici, anche assenti oggi. Le presenze verranno scelte a ogni nuova gara.")
            if(target!=null)Text("La rosa con questo nome verrà aggiornata. Le gare già salvate restano invariate.")
            if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
        }
    },confirmButton={TextButton(modifier=Modifier.testTag("save-roster-confirm"),enabled=!busy && label.isNotBlank(),onClick={val name=label;val replace=target?.id;busy=true;scope.launch{
        try{withContext(Dispatchers.IO){store.save(name,team,replace)};onSaved()}
        catch(t:Exception){error=t.message;busy=false}
    }}){Text(if(target==null)"Salva rosa" else "Aggiorna rosa")}},dismissButton={TextButton(enabled=!busy,onClick=onClose){Text("Annulla")}})
}

@Composable
fun LoadRosterDialog(onClose:()->Unit,onLoad:(Team)->Unit) {
    val context=LocalContext.current
    var saved by remember{mutableStateOf<List<SavedRoster>>(emptyList())}
    var previous by remember{mutableStateOf<List<Pair<String,Team>>>(emptyList())}
    var busy by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(Unit){try {
        val result=withContext(Dispatchers.IO){
            val templates=RosterStore(File(context.filesDir,"rosters")).list().mapNotNull{it.second.getOrNull()}
            val games=MatchStore(File(context.filesDir,"matches")).list().mapNotNull{it.second.getOrNull()}
            templates to games.flatMap{j->j.config.teams.map{team->"${team.name} · ${j.config.date}" to Rosters.template(team)}}.distinctBy{it.second}
        };saved=result.first;previous=result.second
    }catch(t:Exception){error=t.message};busy=false}
    RosterDialog(onDismissRequest=onClose,title={Text("Carica rosa")},text={
        Column(Modifier.fillMaxWidth().heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("Carica i dati della squadra, poi seleziona le presenti. La distinta che stai preparando verrà sostituita.")
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
            if(saved.isNotEmpty())Text("Rose salvate",style=MaterialTheme.typography.titleSmall)
            saved.forEach{item->OutlinedButton(onClick={onLoad(Rosters.fromTemplate(item.team))},modifier=Modifier.fillMaxWidth()){
                Text("${item.label} · ${item.team.players.size} giocatrici")
            }}
            if(previous.isNotEmpty())Text("Da partite precedenti",style=MaterialTheme.typography.titleSmall)
            previous.forEach{(label,team)->TextButton(onClick={onLoad(Rosters.fromTemplate(team))},modifier=Modifier.fillMaxWidth()){Text(label)}}
            if(!busy && saved.isEmpty() && previous.isEmpty())Text("Nessuna rosa salvata. Inserisci le giocatrici e usa Salva rosa; puoi farlo anche dai dati di una partita già giocata.")
        }
    },confirmButton={TextButton(onClick=onClose){Text("Chiudi")}})
}


@Composable
private fun RosterDialog(onDismissRequest:()->Unit,title:@Composable ()->Unit,text:@Composable ()->Unit,
                         confirmButton:@Composable ()->Unit,dismissButton:@Composable ()->Unit={}) {
    Dialog(onDismissRequest=onDismissRequest,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.widthIn(max=520.dp).fillMaxWidth(.95f),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge,title)
                text()
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {dismissButton();confirmButton()}
            }
        }
    }
}
