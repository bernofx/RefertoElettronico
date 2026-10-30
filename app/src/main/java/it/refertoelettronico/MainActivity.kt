package it.refertoelettronico

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import it.refertoelettronico.model.*
import it.refertoelettronico.integration.IncomingMatch
import it.refertoelettronico.feedback.ActionFeedback
import it.refertoelettronico.feedback.Cue
import it.refertoelettronico.pdf.PdfExporter
import it.refertoelettronico.storage.MatchStore
import it.refertoelettronico.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private var incoming by mutableStateOf<Intent?>(null)
    public override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if(IncomingMatch.accepts(intent))incoming=intent
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(savedInstanceState==null && IncomingMatch.accepts(intent))incoming=intent
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary=Color(0xFF145663),secondary=Color(0xFF9B6326),
            background=Color(0xFFF6F5F0),surface=Color(0xFFFFFEFA),primaryContainer=Color(0xFFDCEEF0))) { VolleyApp(incoming,onIncomingHandled={incoming=null}) } }
    }
}

@Composable
fun VolleyApp(incoming:Intent?=null,onIncomingHandled:()->Unit={}) {
    val context=LocalContext.current
    val store=remember { MatchStore(File(context.filesDir,"matches")) }
    val feedback=remember { ActionFeedback(context) }
    var soundEnabled by remember { mutableStateOf(feedback.enabled) }
    DisposableEffect(feedback) { onDispose{feedback.close()} }
    var journal by remember { mutableStateOf<Journal?>(null) }
    var id by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var importedDraft by remember { mutableStateOf<MatchConfig?>(null) }
    var incomingReview by remember { mutableStateOf<MatchConfig?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var archiveRevision by remember { mutableIntStateOf(0) }
    var confirmUndo by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    val snackbar=remember { SnackbarHostState() }
    val scope=rememberCoroutineScope()
    fun failure(t: Throwable) { error=t.message ?: "Operazione non riuscita.";feedback.play(Cue.ERROR) }
    fun persist(next: Journal,cue:Cue=Cue.CONFIRM):Boolean {
        try { val key=id ?: store.create(next);if(id!=null)store.save(key,next);id=key;journal=next;archiveRevision++;feedback.play(cue);return true }
        catch(t:Exception) { failure(t);return false }
    }
    fun event(e: Event) { try { persist(MatchEngine.add(journal!!,e),Cue.forEvent(e)) } catch(t:Exception) { failure(t) } }
    LaunchedEffect(incoming) {
        if(incoming!=null) {
            try { incomingReview=withContext(Dispatchers.IO){IncomingMatch.read(context,incoming)} }
            catch(t:Exception){failure(IllegalArgumentException("Importazione non riuscita: ${t.message ?: "file non valido"}",t))}
            finally { onIncomingHandled() }
        }
    }
    var pendingBackup by remember { mutableStateOf<String?>(null) }
    val saveBackup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val data=pendingBackup;pendingBackup=null
        if(uri!=null && data!=null)scope.launch { try {
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) } ?: error("Impossibile aprire il documento.") }
            feedback.play(Cue.CONFIRM)
            snackbar.showSnackbar("Copia della gara salvata, inclusa la cronologia.")
        } catch(t:Exception){failure(t)} }
    }
    val importBackup=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null)scope.launch { try {
            val restored=withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer=java.io.ByteArrayOutputStream()
                    val block=ByteArray(8192)
                    while(true) { val n=input.read(block);if(n<0)break;require(buffer.size()+n<=20_000_000){"Archivio troppo grande."};buffer.write(block,0,n) }
                    store.decode(buffer.toString("UTF-8"))
                } ?: error("Impossibile leggere l'archivio.")
            }
            id=null;persist(restored);tab=0
        } catch(t:Exception){failure(t)} }
    }
    BackHandler(enabled=editing || journal!=null) { if(editing){editing=false;importedDraft=null} else {journal=null;id=null} }
    val state=remember(journal) { journal?.let { MatchEngine.replay(it.config,it.events) } }
    Scaffold(snackbarHost={SnackbarHost(snackbar)},bottomBar={
        if(journal!=null && !editing) NavigationBar {
            listOf("Gara","Registro","Referto","Dati").forEachIndexed { index,label ->
                NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Text(listOf("◉","≡","▤","⚙")[index])},label={Text(label)})
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("REFERTO VOLLEY",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                    Text(if(editing) "Prepara la gara" else if(journal==null) "A bordo campo" else journal!!.config.competition,
                        style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,maxLines=2)
                }
                SoundToggle(soundEnabled){ enabled ->
                    try {feedback.setEnabled(enabled);soundEnabled=enabled}catch(t:Exception){failure(t)}
                }
                if(journal!=null || editing)TextButton(onClick={editing=false;importedDraft=null;journal=null;id=null}){Text("Gare")}
            }
            HorizontalDivider(color=Color(0xFFE2E5DF))
            if(editing) key(importedDraft) { MatchEditor(importedDraft ?: journal?.config,onSave={ config ->
                try {
                    MatchEngine.validate(config)
                    val previous=journal
                    val retainedRedo=previous?.redo?.takeIf { events ->
                        runCatching{MatchEngine.replay(config,previous.events+events)}.isSuccess
                    }.orEmpty()
                    val next=previous?.copy(config=config,redo=retainedRedo) ?: Journal(config=config)
                    MatchEngine.replay(config,next.events);persist(next)
                    if(journal==next){editing=false;importedDraft=null;tab=0}
                } catch(t:Exception){failure(t)}
            },rosterLocked=journal?.events?.any{it.kind==Kind.START_SET}==true) }
            else if(journal==null) {
                val archives=remember(archiveRevision) { store.list() }
                HomeScreen(archives,onNew={importedDraft=null;id=null;editing=true},onDemo={id=null;persist(Journal(config=Demo.config(),events=listOf(Demo.start())));tab=0},
                    onOpen={key ->try {journal=store.load(key);id=key;tab=0;feedback.play(Cue.CONFIRM)}catch(t:Exception){failure(t)}},
                    onImport={importBackup.launch(arrayOf("application/json","text/plain","application/octet-stream"))})
            } else {
                val j=journal!!;val s=state!!
                when(tab) {
                    0 -> LiveScreen(j.config,s,j.events.lastOrNull(),onEvent=::event,
                        onUndo={try{if(persist(MatchEngine.undo(j),Cue.UNDO))scope.launch{snackbar.showSnackbar("Operazione annullata")}}catch(t:Exception){failure(t)}},onRedo={try{persist(MatchEngine.redo(j))}catch(t:Exception){failure(t)}},
                        canUndo=j.events.isNotEmpty(),canRedo=j.redo.isNotEmpty(),onHistory={tab=1})
                    1 -> HistoryScreen(j,onUndoTo={confirmUndo=it},onRedo={try{persist(MatchEngine.redo(j))}catch(t:Exception){failure(t)}})
                    2 -> ExportScreen(j,onError=::failure,onComplete={feedback.play(Cue.CONFIRM)},onBackup={pendingBackup=store.encode(j);saveBackup.launch("gara-${j.config.matchNumber.ifBlank{"volley"}}.json")})
                    3 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        Text("Dati e regolamento",style=MaterialTheme.typography.headlineSmall)
                        Text("${j.config.teams[0].name} / ${j.config.teams[1].name}\n${j.config.date} · ${j.config.location}\n${j.config.format.label}")
                        Button(onClick={editing=true}){Text("Modifica i dati della gara")}
                        Text(if(j.events.isEmpty())"Distinte e formula sono modificabili prima dell'inizio." else "Distinte e formula sono bloccate dopo l'inizio. Per correggerle, annulla fino all'inizio dal Registro.")
                        RulesCard()
                        OutlinedButton(onClick={pendingBackup=store.encode(j);saveBackup.launch("gara-${j.config.matchNumber.ifBlank{"volley"}}.json")}){Text("Salva copia della gara (JSON)")}
                    }
                }
            }
        }
    }
    if(incomingReview!=null)AlertDialog(onDismissRequest={incomingReview=null},title={Text("Dati gara ricevuti")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            val config=incomingReview!!
            Text(config.competition)
            config.teams.forEach { Text("${it.name.ifBlank{"Squadra da completare"}} · ${it.players.size} giocatrici") }
            Text("Si aprirà una nuova gara da controllare e completare. Le gare già salvate rimarranno nell'archivio. Nessun dato ricevuto è stato ancora salvato.")
        }},confirmButton={TextButton(onClick={
            importedDraft=incomingReview;incomingReview=null;journal=null;id=null;editing=true;tab=0
        }){Text("Rivedi dati")}},dismissButton={TextButton(onClick={incomingReview=null}){Text("Ignora")}})
    if(confirmUndo>0 && journal!=null) AlertDialog(onDismissRequest={confirmUndo=0},title={Text("Annullare ${confirmUndo} operazioni?")},
        text={Text("Saranno ripristinati anche formazioni, servizio, libero, timeout e rotazioni. Puoi recuperare le operazioni con Ripristina finché non registri un nuovo evento.")},
        confirmButton={TextButton(onClick={try{persist(MatchEngine.undo(journal!!,confirmUndo),Cue.UNDO)}catch(t:Exception){failure(t)};confirmUndo=0}){Text("Annulla operazioni")}},
        dismissButton={TextButton(onClick={confirmUndo=0}){Text("Mantieni")}})
    if(error!=null)AlertDialog(onDismissRequest={error=null},title={Text("Controlla l'operazione")},text={Text(error!!)},confirmButton={TextButton(onClick={error=null}){Text("Ho capito")}})
}

@Composable
private fun ExportScreen(journal: Journal,onError:(Throwable)->Unit,onComplete:()->Unit,onBackup:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var file by remember(journal) { mutableStateOf<File?>(null) }
    var bitmap by remember(journal) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val saver=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val source=file
        if(uri!=null && source!=null)scope.launch { try {
            withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { output ->source.inputStream().use{it.copyTo(output)} } ?: error("Impossibile salvare il PDF.") };onComplete()
        } catch(t:Exception){onError(t)} }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Il referto, pronto da stampare",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Modello cartaceo FIPAV con registrazioni e allegato cronologico. Le firme vanno apposte sul documento stampato.")
        val state=remember(journal){MatchEngine.replay(journal.config,journal.events)}
        if(!state.finished(journal.config))Notice("Gara in corso: il PDF sarà contrassegnato come provvisorio.")
        if(journal.config.missingMetadata().isNotEmpty())Notice("Dati da completare: ${journal.config.missingMetadata().joinToString()}. Puoi correggerli nella scheda Dati.")
        if(journal.config.fallbackReason.isBlank())Notice("Completa il motivo dell'uso del cartaceo nei Dati o nelle Osservazioni.")
        Button(enabled=!busy,onClick={scope.launch {
            busy=true
            try { val result=withContext(Dispatchers.IO) { val f=PdfExporter.export(context,journal);f to PdfExporter.preview(context,journal) };file=result.first;bitmap?.recycle();bitmap=result.second;onComplete() }
            catch(t:Exception){onError(t)} finally{busy=false}
        }}){Text(if(busy)"Preparazione PDF…" else "Genera referto PDF")}
        if(bitmap!=null) {
            Image(bitmap!!.asImageBitmap(),contentDescription="Anteprima prima pagina del referto compilato",modifier=Modifier.fillMaxWidth().border(1.dp,Color.LightGray))
            Text("Prima pagina. Il PDF include anche il registro completo e le eventuali eccedenze.",style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={saver.launch("referto-${journal.config.matchNumber.ifBlank{"volley"}}.pdf")}){Text("Salva PDF")}
                OutlinedButton(onClick={try {
                    val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file!!)
                    val intent=Intent(Intent.ACTION_SEND).apply {type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)}
                    context.startActivity(Intent.createChooser(intent,"Condividi referto"))
                }catch(t:Exception){onError(t)}}){Text("Condividi")}
            }
        }
        OutlinedButton(onClick=onBackup){Text("Salva anche la gara modificabile")}
    }
}
