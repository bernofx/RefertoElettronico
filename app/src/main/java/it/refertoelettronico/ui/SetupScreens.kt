package it.refertoelettronico.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
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
fun Notice(message: String) { Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(12.dp)) {Text(message,Modifier.padding(horizontal=14.dp,vertical=10.dp),style=MaterialTheme.typography.bodyMedium)} }
@Composable
fun Field(label:String,value:String,onChange:(String)->Unit,modifier:Modifier=Modifier,numeric:Boolean=false,enabled:Boolean=true) {
    OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=modifier.fillMaxWidth(),singleLine=true,enabled=enabled,
        shape=MaterialTheme.shapes.medium,
        colors=OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor=MaterialTheme.colorScheme.surface,
            focusedContainerColor=MaterialTheme.colorScheme.surface,
            unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant),
        keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Number else KeyboardType.Text))
}
@Composable
fun <T> Picker(label:String,selected:T,choices:List<T>,name:(T)->String,onChange:(T)->Unit,modifier:Modifier=Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
        Box { FilledTonalButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=MaterialTheme.shapes.small,
            contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)) {
            Text(name(selected),Modifier.weight(1f),maxLines=2);UiGlyph(UiSymbol.CHEVRON)
        }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                choices.forEach { item ->DropdownMenuItem(text={Text(name(item))},onClick={onChange(item);expanded=false}) }
            }
        }
    }
}

@Composable
fun HomeScreen(archives:List<Pair<String,Result<Journal>>>,onNew:()->Unit,onDemo:()->Unit,onOpen:(String)->Unit,onImport:()->Unit,onDelete:(String)->Unit,onResume:(()->Unit)?=null,onDiscardDraft:(()->Unit)?=null) {
    var deleting by remember{mutableStateOf<Pair<String,String>?>(null)}
    if(deleting!=null)AlertDialog(onDismissRequest={deleting=null},title={Text("Eliminare questa gara?")},text={Text("${deleting!!.second}\n\nLa gara e la sua cronologia verranno eliminate da questo dispositivo. Le copie JSON già esportate e le rose salvate restano disponibili.")},confirmButton={TextButton(modifier=Modifier.testTag("confirm-delete-match"),onClick={val key=deleting!!.first;deleting=null;onDelete(key)}){Text("Elimina gara",color=MaterialTheme.colorScheme.error)}},dismissButton={TextButton(onClick={deleting=null}){Text("Annulla")}})
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(archives.isEmpty())Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF103E34),Color(0xFF1A6552))),RoundedCornerShape(28.dp))) {
            Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=Color.White.copy(alpha=.12f),shape=RoundedCornerShape(50)) {
                        Text("U14 FEMMINILE · COMO",Modifier.padding(horizontal=10.dp,vertical=6.dp),style=MaterialTheme.typography.labelSmall,color=Color(0xFFD7F2E7))
                    }
                    UiGlyph(UiSymbol.COURT,Modifier.size(32.dp),Color(0xFFA5CFB9))
                }
                Text("La tua prossima\ngara, punto per punto.",style=MaterialTheme.typography.headlineLarge,color=Color.White)
                Text("Formazioni, rotazioni e referto.\nTutto a portata di mano, anche offline.",style=MaterialTheme.typography.bodyMedium,color=Color(0xFFD1E5D8))
                Button(onClick=onNew,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).tourTarget("home-new"),
                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFF4CE7B),contentColor=Color(0xFF33280E))) {
                    ActionLabel("Prepara una nuova gara",UiSymbol.ADD)
                }
            }
        }
        if(archives.isNotEmpty())Button(onClick=onNew,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).tourTarget("home-new")){ActionLabel("Prepara una nuova gara",UiSymbol.ADD)}
        if(onResume!=null)Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick=onResume,modifier=Modifier.weight(1f)){Text("Riprendi preparazione")}
            if(onDiscardDraft!=null)TextButton(onClick=onDiscardDraft){Text("Scarta bozza")}
        }
        FilledTonalButton(onClick=onDemo,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp)) {ActionLabel("Prova con una gara dimostrativa",UiSymbol.COURT)}
        if(archives.isNotEmpty())Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("Le tue gare",style=MaterialTheme.typography.titleLarge)
            Text("${archives.count{it.second.isSuccess}} salvate",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        MatchBadges(archives,onOpen,onDeleteRequest={id,title->deleting=id to title})
        TextButton(onClick=onImport,modifier=Modifier.fillMaxWidth()){ActionLabel("Importa una copia della gara (JSON)",UiSymbol.UPLOAD)}
        Text("Le gare restano su questo dispositivo. Salva una copia dal menu Referto per trasferirle o conservarle altrove.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MatchEditor(original:MatchConfig?,onSave:(MatchConfig)->Unit,rosterLocked:Boolean,onDraftChange:(MatchConfig)->Unit={},initialSection:Int=0) {
    val initial=remember { original?.let{StaffReview.prepare(it)} ?: MatchConfig(teams=List(2){Team("",(1..6).map { Player(it,"",captain=it==1) })},
        date=LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))) }
    val configState=remember{mutableStateOf(initial)}
    val saveDraft by rememberUpdatedState(onDraftChange)
    // Persist in the edit callback, before a pause can interrupt the next Compose frame.
    var config by object:kotlin.properties.ReadWriteProperty<Any?,MatchConfig> {
        override fun getValue(thisRef:Any?,property:kotlin.reflect.KProperty<*>)=configState.value
        override fun setValue(thisRef:Any?,property:kotlin.reflect.KProperty<*>,value:MatchConfig){configState.value=value;saveDraft(value)}
    }
    LaunchedEffect(Unit){saveDraft(config)}
    var section by remember { mutableIntStateOf(initialSection) }
    var validationError by remember{mutableStateOf<String?>(null)}
    if(validationError!=null)AlertDialog(onDismissRequest={validationError=null},title={Text("Controlla la distinta")},text={Text(validationError!!)},confirmButton={TextButton(onClick={validationError=null}){Text("Correggi")}})
    val scroll=rememberScrollState()
    LaunchedEffect(section){scroll.scrollTo(0)}
    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex=section,edgePadding=12.dp) {
            listOf("Gara","Squadra di casa","Squadra ospite","Ufficiali").forEachIndexed { i,label ->Tab(selected=section==i,onClick={section=i},text={Text(label)})}
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(14.dp),verticalArrangement=Arrangement.spacedBy(if(section in 1..2)6.dp else 12.dp)) {
            when(section) {
                0 -> {
                    Notice("U14 femminile Como 2026/27: al meglio di 5 set, rete 2,15 m, libero e secondo libero ammessi. Otto sostituzioni e due timeout per set.")
                    Field("Campionato",config.competition,{config=config.copy(competition=it)})
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Field("Stagione",config.season,{config=config.copy(season=it)},Modifier.weight(1f))
                        Field("Numero gara",config.matchNumber,{config=config.copy(matchNumber=it)},Modifier.weight(1f).tourTarget("match-data"))
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
                    TeamEditor(team,index=t,season=config.season,locked=rosterLocked,onChange={config=config.copy(teams=config.teams.with(t,it))})
                    StaffCandidatesEditor(config,teamIndex=t,onChange={i,items->config=config.copy(teams=config.teams.with(i,config.teams[i].copy(staffRoster=items)))})
                }
                3 -> {
                    StaffCandidatesEditor(config,serviceOnly=true,onChange={i,items->config=config.copy(teams=config.teams.with(i,config.teams[i].copy(staffRoster=items)))})
                    val service=config.teams.flatMap{it.staffRoster.orEmpty()}.filter{it.selected && it.scope=="SERVICE"}
                    val attendant=service.firstOrNull{it.assignment=="ATTENDANT"}
                    Text("Assistenza della società",style=MaterialTheme.typography.titleSmall)
                    Field("Addetto di società all’arbitro",attendant?.name ?: config.refereeAttendant.orEmpty(),{config=config.copy(refereeAttendant=it)},enabled=attendant==null)
                    Text("L’addetto assiste l’arbitro. Il direttore di gara va indicato nel campo 1° arbitro qui sotto.",style=MaterialTheme.typography.bodySmall)
                    if(attendant!=null)Text("Nome ripreso dall’incarico selezionato sopra: per cambiarlo, modifica quella selezione.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider()
                    Text("Arbitri e segnapunti",style=MaterialTheme.typography.titleLarge)
                    Text("Le firme finali usano i nominativi di segnapunti, 2° arbitro se presente e 1° arbitro.",style=MaterialTheme.typography.bodySmall)
                    listOf("1° arbitro","2° arbitro","Segnapunti","Giudice di linea 1","Giudice di linea 2","Giudice di linea 3","Giudice di linea 4").forEachIndexed { i,label ->
                        val proposal=if(i==2)service.firstOrNull{it.assignment=="SCORER"}else null
                        if(i==3)Text("Giudici di linea",style=MaterialTheme.typography.titleSmall)
                        Field(label,proposal?.name ?: config.officials.getOrElse(i){""},{config=config.copy(officials=config.officials.with(i,it))},enabled=proposal==null)
                        if(proposal!=null)Text("Segnapunti ripreso dall’incarico selezionato sopra.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        if(i<3)Field("Città · $label",config.officialCities.getOrElse(i){""},{config=config.copy(officialCities=config.officialCities.with(i,it))})
                    }
                }
            }
        }
        Row(Modifier.padding(16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(section<3)OutlinedButton(onClick={section++},modifier=Modifier.weight(1f)){Text("Avanti")}
            Button(onClick={try{val final=StaffReview.confirm(config);MatchEngine.validate(final);onSave(final)}catch(t:IllegalArgumentException){validationError=t.message}},modifier=Modifier.weight(1f).tourTarget("save-setup")){Text(if(original==null)"Salva gara" else "Salva modifiche")}
        }
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
