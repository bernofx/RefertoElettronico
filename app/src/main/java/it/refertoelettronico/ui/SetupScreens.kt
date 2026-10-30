package it.refertoelettronico.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun Notice(message: String) { Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(12.dp)) {Text(message,Modifier.padding(14.dp),style=MaterialTheme.typography.bodyMedium)} }
@Composable
fun Field(label:String,value:String,onChange:(String)->Unit,modifier:Modifier=Modifier,numeric:Boolean=false,enabled:Boolean=true) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=modifier.fillMaxWidth(),singleLine=true,enabled=enabled,
        keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Number else KeyboardType.Text))
}
@Composable
fun <T> Picker(label:String,selected:T,choices:List<T>,name:(T)->String,onChange:(T)->Unit,modifier:Modifier=Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        Box { OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {Text(name(selected),maxLines=2)}
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                choices.forEach { item ->DropdownMenuItem(text={Text(name(item))},onClick={onChange(item);expanded=false}) }
            }
        }
    }
}

@Composable
fun HomeScreen(archives:List<Pair<String,Result<Journal>>>,onNew:()->Unit,onDemo:()->Unit,onOpen:(String)->Unit,onImport:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Surface(color=Color(0xFF153E49),shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(25.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Ogni punto.\nTutto sotto controllo.",style=MaterialTheme.typography.headlineLarge,color=Color.White,fontWeight=FontWeight.Bold)
                Text("U14 femminile · FIPAV Como\nPunti, rotazioni e referto cartaceo, anche senza rete.",color=Color(0xFFD6E4E7))
                Button(onClick=onNew,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFF3CA74),contentColor=Color(0xFF163A44))) {Text("Prepara una nuova gara")}
            }
        }
        OutlinedButton(onClick=onDemo,modifier=Modifier.fillMaxWidth()){Text("Prova con una gara dimostrativa")}
        if(archives.isNotEmpty())Text("Le tue gare",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
        archives.forEach { (key,result) ->
            val j=result.getOrNull()
            if(j!=null) {
                val s=remember(j){MatchEngine.replay(j.config,j.events)}
                OutlinedCard(onClick={onOpen(key)},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("${j.config.teams[0].name} — ${j.config.teams[1].name}",fontWeight=FontWeight.Bold)
                        Text("${j.config.date} · set ${s.wins(0)}:${s.wins(1)} · ${if(s.finished(j.config))"Terminata" else "Da riprendere"}")
                    }
                }
            } else Notice("Archivio $key non leggibile: è stato conservato. Puoi recuperare una copia JSON valida.")
        }
        TextButton(onClick=onImport){Text("Importa una copia della gara (JSON)")}
        Text("Le gare restano su questo dispositivo. Salva una copia dal menu Referto per trasferirle o conservarle altrove.",style=MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MatchEditor(original:MatchConfig?,onSave:(MatchConfig)->Unit,rosterLocked:Boolean) {
    val initial=remember { original ?: MatchConfig(teams=List(2){Team("",(1..6).map { Player(it,"",captain=it==1) })},
        date=LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    var config by remember { mutableStateOf(initial) }
    var section by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex=section,edgePadding=12.dp) {
            listOf("Gara","Squadra di casa","Squadra ospite","Ufficiali").forEachIndexed { i,label ->Tab(selected=section==i,onClick={section=i},text={Text(label)})}
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            when(section) {
                0 -> {
                    Notice("U14 femminile Como 2026/27: al meglio di 5 set, rete 2,15 m, libero e secondo libero ammessi. Otto sostituzioni e due timeout per set.")
                    Field("Campionato",config.competition,{config=config.copy(competition=it)})
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Field("Stagione",config.season,{config=config.copy(season=it)},Modifier.weight(1f))
                        Field("Numero gara",config.matchNumber,{config=config.copy(matchNumber=it)},Modifier.weight(1f))
                    }
                    Field("Girone / fase",config.phase,{config=config.copy(phase=it)})
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Field("Data (gg/mm/aaaa)",config.date,{config=config.copy(date=it)},Modifier.weight(1f))
                        Field("Ora prevista (hh:mm)",config.scheduledTime,{config=config.copy(scheduledTime=it)},Modifier.weight(1f))
                    }
                    Field("Località",config.location,{config=config.copy(location=it)})
                    Field("Palestra / campo",config.venue,{config=config.copy(venue=it)})
                    if(!rosterLocked) {
                        Picker("Formula",config.format,Format.entries.toList(),{it.label},{config=config.copy(format=it)})
                        Picker("Limite sostituzioni",config.maxSubstitutions,listOf(8,6),{if(it==8)"8 · stagione 2026/27" else "6 · regolamento precedente"},{config=config.copy(maxSubstitutions=it)})
                    }
                    Field("Motivo dell'uso del cartaceo",config.fallbackReason,{config=config.copy(fallbackReason=it)})
                    OutlinedTextField(value=config.initialObservations,onValueChange={config=config.copy(initialObservations=it)},label={Text("Osservazioni prima della gara")},minLines=3,modifier=Modifier.fillMaxWidth())
                }
                1,2 -> {
                    val t=section-1;val team=config.teams[t]
                    TeamEditor(team,index=t,locked=rosterLocked,onChange={config=config.copy(teams=config.teams.with(t,it))})
                }
                3 -> {
                    Text("Nomi e città degli ufficiali",style=MaterialTheme.typography.titleLarge)
                    listOf("1° arbitro","2° arbitro","Segnapunti","Giudice di linea 1","Giudice di linea 2","Giudice di linea 3","Giudice di linea 4").forEachIndexed { i,label ->
                        Field(label,config.officials.getOrElse(i){""},{config=config.copy(officials=config.officials.with(i,it))})
                        if(i<3)Field("Città · $label",config.officialCities.getOrElse(i){""},{config=config.copy(officialCities=config.officialCities.with(i,it))})
                    }
                }
            }
        }
        Row(Modifier.padding(16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(section<3)OutlinedButton(onClick={section++},modifier=Modifier.weight(1f)){Text("Avanti")}
            Button(onClick={onSave(config)},modifier=Modifier.weight(1f)){Text(if(original==null)"Salva gara" else "Salva modifiche")}
        }
    }
}

@Composable
private fun TeamEditor(team:Team,index:Int,locked:Boolean,onChange:(Team)->Unit) {
    var importSection by remember(index){mutableStateOf<it.refertoelettronico.ocr.Camp3Section?>(null)}
    if(importSection!=null)Camp3ImportDialog(team.name,onDismiss={importSection=null},onApply={players->onChange(team.copy(players=players));importSection=null},
        section=importSection!!,onStaff={changes->onChange(team.copy(staff=List(5){changes[it] ?: team.staff.getOrElse(it){""}}));importSection=null})
    Field("Denominazione squadra",team.name,{onChange(team.copy(name=it))})
    TeamColorPicker(team,index,onChange)
    Field("Codice società",team.clubCode,{onChange(team.copy(clubCode=it))})
    if(locked)Notice("Numeri e ruoli bloccati: per correggerli annulla gli eventi fino all'inizio. I nomi delle giocatrici, delle squadre e gli altri dati restano modificabili.")
    Text("Distinta · ${team.players.size}/14",style=MaterialTheme.typography.titleLarge)
    if(!locked)OutlinedButton(onClick={importSection=it.refertoelettronico.ocr.Camp3Section.PLAYERS}){Text("Foto lista giocatrici") }
    Text("Puoi iniziare con i soli numeri. Per il referto cartaceo FIPAV completa cognome e nome di tutte le giocatrici, anche in seguito.",style=MaterialTheme.typography.bodySmall)
    Text("K = capitana · L = libero. Seleziona una sola capitana e al massimo due liberi.",style=MaterialTheme.typography.bodySmall)
    team.players.forEachIndexed { index,p ->
        OutlinedCard {
            Column(Modifier.padding(10.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Field("N°",if(p.number==0)"" else p.number.toString(),{v ->onChange(team.copy(players=team.players.with(index,p.copy(number=v.filter{it.isDigit()}.take(2).toIntOrNull()?:0))))},Modifier.width(74.dp),numeric=true,enabled=!locked)
                    Field("Cognome e nome",p.name,{onChange(team.copy(players=team.players.with(index,p.copy(name=it))))},Modifier.weight(1f))
                }
                Row {
                    Row(Modifier.weight(1f)) {
                        Checkbox(checked=p.captain,enabled=!locked,onCheckedChange={checked ->onChange(team.copy(players=team.players.mapIndexed {i,v ->v.copy(captain=i==index && checked)}))});Text("K",Modifier.padding(top=13.dp))
                        Checkbox(checked=p.libero,enabled=!locked,onCheckedChange={onChange(team.copy(players=team.players.with(index,p.copy(libero=it))))});Text("L",Modifier.padding(top=13.dp))
                    }
                    if(!locked && team.players.size>6)TextButton(onClick={onChange(team.copy(players=team.players.filterIndexed { i,_ ->i!=index }))}){Text("Rimuovi")}
                }
            }
        }
    }
    if(!locked && team.players.size<14)OutlinedButton(onClick={onChange(team.copy(players=team.players+Player(0,"")))}){Text("Aggiungi giocatrice")}
    Text("Panchina",style=MaterialTheme.typography.titleMedium)
    OutlinedButton(onClick={importSection=it.refertoelettronico.ocr.Camp3Section.STAFF}){Text("Foto staff (facoltativa)")}
    listOf("Allenatore","Vice allenatore","Fisioterapista","Medico","Dirigente").forEachIndexed { i,label ->
        Field(label,team.staff.getOrElse(i){""},{onChange(team.copy(staff=team.staff.with(i,it)))})
    }
}

@Composable
fun RulesCard() {
    val context=LocalContext.current
    OutlinedCard {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Profilo U14F · Como · 2026/27",fontWeight=FontWeight.Bold)
            Text("Gara: 3 set vinti su 5. Set a 25, quinto a 15, sempre due punti di scarto. Cambio campo del quinto a 8. Due timeout; otto sostituzioni. Rimpiazzi del libero separati dai cambi.")
            Text("Il cartaceo è previsto in casi eccezionali: assenza totale di rete o problemi del portale. Le decisioni su casi eccezionali restano dell'arbitro; registrale nelle osservazioni.")
            listOf(
                "Regolamento U14F Como" to "https://www.fipav.como.it/data-files/fipavcomo/dropbox/Regolamento%20U14F%20Stagione%202026-2027.pdf",
                "Manuale e documenti FIPAV" to "https://www.federvolley.it/documenti-UG",
                "Nuove regole 2026/27" to "https://www.fipav.como.it/article.php?article_id=1320"
            ).forEach { (label,url) ->TextButton(onClick={runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}}){Text(label)} }
        }
    }
}
