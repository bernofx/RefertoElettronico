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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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
    var advanced by remember{mutableStateOf(false)}
    var notes by remember{mutableStateOf(false)}
    var officialDetails by remember{mutableStateOf(false)}
    var serviceDetails by remember{mutableStateOf(false)}
    var pendingTarget by remember{mutableStateOf<String?>(null)}
    val targets=remember{PreparationTargets()}
    val steps=remember(config){PreparationProgress.steps(config)}
    val next=steps.firstOrNull{!it.complete}
    fun suggested() {
        val step=next ?: return
        val target=step.issues.first().target
        if(target in listOf("competition","season"))advanced=true
        if(target=="service")serviceDetails=true
        section=step.index;pendingTarget=target
    }
    LaunchedEffect(section,pendingTarget) {
        val key=pendingTarget ?: return@LaunchedEffect
        val entry=snapshotFlow{targets.entries[key]}.filterNotNull().first()
        snapshotFlow{entry.placed}.first{it}
        entry.bring.bringIntoView();entry.focus?.requestFocus()
        pendingTarget=null
    }
    fun save() {try{val final=StaffReview.confirm(config);MatchEngine.validate(final);onSave(final)}catch(t:IllegalArgumentException){validationError=t.message}}
    if(validationError!=null)AlertDialog(onDismissRequest={validationError=null},title={Text("Controlla la distinta")},text={Text(validationError!!)},confirmButton={TextButton(onClick={validationError=null}){Text("Correggi")}})
    val scroll=rememberScrollState()
    LaunchedEffect(section){scroll.scrollTo(0)}
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            Text("Preparazione",style=MaterialTheme.typography.labelLarge)
            Text("${steps.count{it.complete}}/4 complete",style=MaterialTheme.typography.labelMedium,modifier=Modifier.testTag("setup-progress"))
        }
        LinearProgressIndicator(progress={steps.count{it.complete}/4f},modifier=Modifier.fillMaxWidth().height(3.dp))
        TabRow(selectedTabIndex=section) {
            steps.forEach { step->Tab(selected=section==step.index,onClick={section=step.index;pendingTarget=null},
                modifier=Modifier.testTag("setup-step-${step.index}").semantics{stateDescription=if(step.complete)"Completato"else"Da completare"},
                text={Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(step.complete)"✓"else"${step.index+1}",style=MaterialTheme.typography.labelMedium,color=if(step.complete)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(step.title,style=MaterialTheme.typography.labelMedium,maxLines=2)
                }})}
        }
        Surface(color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.5f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if(next==null)(if(rosterLocked)"Dati gara completi"else"Pronto per le formazioni")else"Prossimo passo · ${next.title}",style=MaterialTheme.typography.labelMedium)
                    Text(next?.issues?.firstOrNull()?.label ?: if(rosterLocked)"Salva le modifiche e torna alla gara"else"Salva i dati e prepara il primo set",style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("setup-suggestion"))
                }
                if(next!=null)TextButton(onClick=::suggested,modifier=Modifier.testTag("setup-suggested-action")){Text("Completa")}
            }
        }
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(14.dp),verticalArrangement=Arrangement.spacedBy(if(section in 1..2)6.dp else 12.dp)) {
            when(section) {
                0 -> {
                    Field("Numero gara",config.matchNumber,{config=config.copy(matchNumber=it)},Modifier.preparationTarget(targets,"match-number",true).tourTarget("match-data"))
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Field("Data (gg/mm/aaaa)",config.date,{config=config.copy(date=it)},Modifier.weight(1f).preparationTarget(targets,"date",true))
                        Field("Ora prevista (hh:mm)",config.scheduledTime,{config=config.copy(scheduledTime=it)},Modifier.weight(1f).preparationTarget(targets,"time",true))
                    }
                    Field("Località",config.location,{config=config.copy(location=it)},Modifier.preparationTarget(targets,"location",true))
                    Field("Palestra / campo",config.venue,{config=config.copy(venue=it)},Modifier.preparationTarget(targets,"venue",true))
                    TextButton(onClick={advanced=!advanced},modifier=Modifier.testTag("setup-advanced")){Text(if(advanced)"Nascondi campionato e regolamento"else"Campionato e regolamento · ${config.season}")}
                    if(advanced) {
                        Field("Campionato",config.competition,{config=config.copy(competition=it)},Modifier.preparationTarget(targets,"competition",true))
                        Field("Stagione",config.season,{config=config.copy(season=it)},Modifier.preparationTarget(targets,"season",true))
                        Field("Girone / fase",config.phase,{config=config.copy(phase=it)})
                        if(!rosterLocked) {
                            Picker("Formula",config.format,Format.entries.toList(),{it.label},{config=config.copy(format=it)})
                            Picker("Limite sostituzioni",config.maxSubstitutions,listOf(8,6),{if(it==8)"8 · stagione 2026/27"else"6 · regolamento precedente"},{config=config.copy(maxSubstitutions=it)})
                        }
                        Text("U14F Como · rete 2,15 m · 2 timeout per set · libero e secondo libero ammessi.",style=MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick={notes=!notes},modifier=Modifier.testTag("setup-notes")){Text(if(notes)"Nascondi note"else"Note e motivo del cartaceo")}
                    if(notes) {
                        Field("Motivo dell'uso del cartaceo",config.fallbackReason,{config=config.copy(fallbackReason=it)})
                        OutlinedTextField(value=config.initialObservations,onValueChange={config=config.copy(initialObservations=it)},label={Text("Osservazioni prima della gara")},minLines=2,modifier=Modifier.fillMaxWidth())
                    }
                }
                1,2 -> {
                    val t=section-1;val team=config.teams[t]
                    TeamEditor(team,index=t,season=config.season,locked=rosterLocked,onChange={config=config.copy(teams=config.teams.with(t,it))},nameModifier=Modifier.preparationTarget(targets,"team-name-$t",true),rosterModifier=Modifier.preparationTarget(targets,"roster-$t"))
                    Column(Modifier.preparationTarget(targets,"staff-$t")){StaffCandidatesEditor(config,teamIndex=t,onChange={i,items->config=config.copy(teams=config.teams.with(i,config.teams[i].copy(staffRoster=items)))})}
                }
                3 -> {
                    val service=config.teams.flatMap{it.staffRoster.orEmpty()}.filter{it.selected && it.scope=="SERVICE"}
                    val attendant=service.firstOrNull{it.assignment=="ATTENDANT"}
                    Text("Arbitro e segnapunti",style=MaterialTheme.typography.titleMedium)
                    Text("Questi nominativi sono usati per le firme finali.",style=MaterialTheme.typography.bodySmall)
                    listOf(0,2).forEach { i->
                        val proposal=if(i==2)service.firstOrNull{it.assignment=="SCORER"}else null
                        Field(if(i==0)"1° arbitro"else"Segnapunti",proposal?.name ?: config.officials.getOrElse(i){""},
                            {config=config.copy(officials=config.officials.with(i,it))},Modifier.preparationTarget(targets,"official-$i",proposal==null),enabled=proposal==null)
                        if(proposal!=null)TextButton(onClick={serviceDetails=true}){Text("Da incarico selezionato · modifica")}
                    }
                    if(config.officials.getOrElse(1){""}.isNotBlank())Field("2° arbitro",config.officials[1],{config=config.copy(officials=config.officials.with(1,it))})
                    TextButton(onClick={serviceDetails=!serviceDetails},modifier=Modifier.testTag("setup-service-details")){Text(if(serviceDetails)"Nascondi incarichi ricevuti"else"Incarichi ricevuti · segnapunti e assistenza")}
                    if(serviceDetails)Column(Modifier.preparationTarget(targets,"service")) {
                        StaffCandidatesEditor(config,serviceOnly=true,onChange={i,items->config=config.copy(teams=config.teams.with(i,config.teams[i].copy(staffRoster=items)))})
                    }
                    TextButton(onClick={officialDetails=!officialDetails},modifier=Modifier.testTag("setup-official-details")){Text(if(officialDetails)"Nascondi altri dati degli ufficiali"else"Altri ufficiali, città e assistenza")}
                    if(officialDetails) {
                        if(config.officials.getOrElse(1){""}.isBlank())Field("2° arbitro",config.officials.getOrElse(1){""},{config=config.copy(officials=config.officials.with(1,it))})
                        listOf("1° arbitro","2° arbitro","Segnapunti").forEachIndexed{i,label->
                            Field("Città · $label",config.officialCities.getOrElse(i){""},{config=config.copy(officialCities=config.officialCities.with(i,it))})
                        }
                        Text("Assistenza della società",style=MaterialTheme.typography.titleSmall)
                        Field("Addetto di società all’arbitro",attendant?.name ?: config.refereeAttendant.orEmpty(),{config=config.copy(refereeAttendant=it)},enabled=attendant==null)
                        Text("L’addetto assiste l’arbitro. Il direttore di gara è indicato nel campo 1° arbitro.",style=MaterialTheme.typography.bodySmall)
                        if(attendant!=null)TextButton(onClick={serviceDetails=true}){Text("Nome da incarico selezionato · modifica sopra")}
                        Text("Giudici di linea",style=MaterialTheme.typography.titleSmall)
                        (3..6).forEach { i->Field("Giudice di linea ${i-2}",config.officials.getOrElse(i){""},{config=config.copy(officials=config.officials.with(i,it))}) }
                    }
                }
            }
        }
        Row(Modifier.padding(16.dp).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(next!=null)Button(onClick=::suggested,modifier=Modifier.weight(1f).testTag("setup-next")){Text("Avanti")}
            if(next==null)Button(onClick=::save,modifier=Modifier.weight(1f).tourTarget("save-setup")){Text(if(original==null)"Salva gara"else"Salva modifiche")}
            else OutlinedButton(onClick=::save,modifier=Modifier.weight(1f).tourTarget("save-setup")){Text(if(original==null)"Salva gara"else"Salva modifiche")}
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
