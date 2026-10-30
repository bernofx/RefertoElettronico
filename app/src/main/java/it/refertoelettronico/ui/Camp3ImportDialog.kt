package it.refertoelettronico.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import it.refertoelettronico.model.Player
import it.refertoelettronico.ocr.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun Camp3ImportDialog(teamName:String,onDismiss:()->Unit,onApply:(List<Player>)->Unit,
                     section:Camp3Section=Camp3Section.PLAYERS,onStaff:(Map<Int,String>)->Unit={}) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var reading by remember{mutableStateOf<Camp3Reading?>(null)}
    var source by remember{mutableStateOf<android.net.Uri?>(null)}
    var rotation by remember{mutableIntStateOf(0)}
    var originalPhoto by remember{mutableStateOf<OriginalPhoto?>(null)}
    var photoBitmap by remember{mutableStateOf<android.graphics.Bitmap?>(null)}
    var crop by remember{mutableStateOf(PhotoCrop())}
    var column by remember{mutableStateOf<Float?>(null)}
    var needColumn by remember{mutableStateOf(false)}
    var cameraFile by remember{mutableStateOf<File?>(null)}
    DisposableEffect(Unit){onDispose{originalPhoto?.close();photoBitmap?.takeUnless{it.isRecycled}?.recycle();cameraFile?.delete()}}
    DisposableEffect(photoBitmap){val bitmap=photoBitmap;onDispose{bitmap?.takeUnless{it.isRecycled}?.recycle()}}
    fun load(uri:android.net.Uri,turns:Int=0) {
        if(busy)return
        val reusable=originalPhoto?.takeIf{source==uri}
        busy=true;error=null;reading=null
        scope.launch {
            var working=reusable
            var preview:android.graphics.Bitmap?=null
            var adopted=false
            try {
                withContext(Dispatchers.IO) {
                    val acquisition=kotlin.coroutines.coroutineContext
                    if(working==null)working=OriginalPhoto.open(context,uri){acquisition.ensureActive()}
                    preview=working!!.preview(turns)
                }
                if(originalPhoto!==working)originalPhoto?.close()
                originalPhoto=working;photoBitmap=preview;source=uri;rotation=turns;adopted=true
                crop=PhotoCrop();column=null;needColumn=false
                val first=withContext(Dispatchers.IO){Camp3Ocr.readOriginal(working!!,section=section,quarterTurns=turns)}
                val detected=first.detectedCrop
                if(detected!=null) {
                    crop=detected
                    val result=withContext(Dispatchers.IO){Camp3Ocr.readOriginal(working!!,detected,section=section,quarterTurns=turns)}
                    if(section==Camp3Section.PLAYERS && result.rows.isNotEmpty() && result.rows.all{it.number==null}) {
                        needColumn=true
                        error="Lista individuata. Non ho riconosciuto la colonna delle maglie: toccala nella foto ingrandita, poi premi Leggi."
                    } else reading=result.copy(notices=listOf("Tabella individuata automaticamente. Se mancano righe, usa Ritaglia e rileggi.")+result.notices)
                } else error="Non ho individuato con certezza la tabella. Controlla il ritaglio qui sotto e premi Leggi."
            }
            catch(t:kotlinx.coroutines.CancellationException){throw t}
            catch(t:Exception){error=t.message ?: "Foto non leggibile. Scegli una foto più nitida."}
            finally{
                if(!adopted){preview?.takeUnless{it.isRecycled}?.recycle();if(working!==reusable)working?.close()}
                busy=false
            }
        }
    }
    fun scan() {
        if(busy)return
        val original=originalPhoto ?: return
        val selectedCrop=crop;val selectedColumn=column
        busy=true;error=null;reading=null
        scope.launch {
            try{reading=withContext(Dispatchers.IO){Camp3Ocr.readOriginal(original,selectedCrop,selectedColumn,section,rotation)}}
            catch(t:kotlinx.coroutines.CancellationException){throw t}
            catch(t:Exception){error=t.message ?: "Lettura non riuscita. Riprova con una foto più vicina."}
            finally{busy=false}
        }
    }
    val photo=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)load(uri)}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ok->
        if(ok)cameraFile?.let{load(FileProvider.getUriForFile(context,"${context.packageName}.files",it))}
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.95f).padding(12.dp),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${if(section==Camp3Section.PLAYERS)"Lista giocatrici" else "Staff"} · $teamName",style=MaterialTheme.typography.titleLarge)
                if(photoBitmap==null)Text(if(section==Camp3Section.PLAYERS)"Fotografa da vicino la lista e la colonna N° maglia. Lo staff si acquisisce con una foto separata, facoltativa." else "Fotografa da vicino lo staff, includendo i nomi dei ruoli. Questa acquisizione non modifica la lista delle giocatrici.",style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled=!busy,onClick={try {
                        cameraFile?.delete()
                        val file=File.createTempFile("camp3-",".jpg",File(context.cacheDir,"camp3").apply{mkdirs()})
                        cameraFile=file
                        camera.launch(FileProvider.getUriForFile(context,"${context.packageName}.files",file))
                    }catch(t:Exception){error="Fotocamera non disponibile. Usa Carica foto."}}){Text("Fotografa")}
                    OutlinedButton(enabled=!busy,onClick={photo.launch(arrayOf("image/*"))}){Text("Carica foto")}
                }
                if(busy){LinearProgressIndicator(Modifier.fillMaxWidth());Text("${if(photoBitmap==null)"Preparazione foto" else "Individuazione e lettura della tabella"}…")}
                if(error!=null)Notice(error!!)
                if(photoBitmap!=null && !busy)Row {
                    TextButton(onClick={source?.let{load(it,(rotation+1)%4)}}){Text("Ruota di 90°")}
                    if(reading!=null)TextButton(onClick={reading=null}){Text("Ritaglia e rileggi")}
                }
                Box(Modifier.weight(1f)) {
                    val result=reading
                    if(!busy && result!=null)key(result) {
                        if(section==Camp3Section.PLAYERS)Camp3RosterReview(result,onApply)
                        else Camp3StaffReview(result,onStaff)
                    }
                    else if(!busy && photoBitmap!=null)Camp3PhotoEditor(photoBitmap!!,crop,{crop=it},column,{column=it},section==Camp3Section.PLAYERS,::scan,chooseColumnInitially=needColumn)
                    else if(!busy)Text("Puoi usare una foto diversa per ogni sezione. Le immagini vengono lette sul dispositivo. Controlla sempre il risultato prima di importarlo.")
                }
                TextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()){Text("Chiudi senza importare")}
            }
        }
    }
}

private data class ReviewedPlayer(val player:Player,val selected:Boolean=true)

@Composable
fun Camp3RosterReview(reading:Camp3Reading,onApply:(List<Player>)->Unit) {
    var rows by remember(reading){mutableStateOf(reading.rows.map{ReviewedPlayer(Player(it.number ?: 0,it.name,libero=it.libero,captain=it.captain))})}
    var showText by remember{mutableStateOf(false)}
    var expanded by remember{mutableStateOf<Int?>(null)}
    val selected=rows.filter{it.selected}.map{it.player}
    val problem=Camp3Rows.validate(selected)
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Rivedi la rosa · ${selected.size} selezionate",style=MaterialTheme.typography.titleMedium)
            Notice("Controlla i numeri di maglia e le righe mancanti. Puoi tornare al ritaglio e indicare la colonna dei numeri. Verifica K (capitana) e L (libero), anche quando proposti dalla foto.")
            reading.notices.forEach{Notice(it)}
            if(rows.isEmpty())Text("Non ho identificato righe di atlete. Prova a ritagliare la foto sulla tabella, oppure aggiungi le righe manualmente.")
            rows.forEachIndexed { i,row ->
                OutlinedCard {
                    Column(Modifier.padding(8.dp)) {
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                            Checkbox(checked=row.selected,onCheckedChange={value->rows=rows.mapIndexed{j,r->if(i==j)r.copy(selected=value)else r}})
                            Field("N° maglia",row.player.number.takeIf{it>0}?.toString().orEmpty(),{v->
                                rows=rows.mapIndexed{j,r->if(i==j)r.copy(player=r.player.copy(number=v.filter{it.isDigit()}.take(2).toIntOrNull() ?: 0))else r}
                            },Modifier.width(82.dp),numeric=true)
                            Field("Cognome e nome",row.player.name,{v->rows=rows.mapIndexed{j,r->if(i==j)r.copy(player=r.player.copy(name=v))else r}},Modifier.weight(1f))
                        }
                        val suggestion=reading.rows.getOrNull(i)
                        if(suggestion!=null) {
                            suggestion.issues.filter{issue->
                                when {
                                    issue.startsWith("Nome") -> row.player.name==suggestion.name
                                    issue.contains("altra riga") -> false
                                    else -> row.player.number==(suggestion.number ?: 0)
                                }
                            }.forEach{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
                            if(row.selected && row.player.number>0 && selected.count{it.number==row.player.number}>1)
                                Text("Numero duplicato nella rosa: controllalo.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
                            TextButton(onClick={expanded=if(expanded==i)null else i}){Text(if(expanded==i)"Nascondi riga originale" else "Mostra riga originale")}
                            if(expanded==i)SelectionContainer{Text(suggestion.source,style=MaterialTheme.typography.bodySmall)}
                        }
                        Row {
                            Checkbox(checked=row.player.captain,onCheckedChange={value->rows=rows.mapIndexed{j,r->r.copy(player=r.player.copy(captain=j==i && value))}});Text("K",Modifier.padding(top=12.dp))
                            Checkbox(checked=row.player.libero,onCheckedChange={value->rows=rows.mapIndexed{j,r->if(i==j)r.copy(player=r.player.copy(libero=value))else r}});Text("L",Modifier.padding(top=12.dp))
                        }
                    }
                }
            }
            if(rows.size<40)OutlinedButton(onClick={rows=rows+ReviewedPlayer(Player(0,""))}){Text("Aggiungi riga")}
            TextButton(onClick={showText=!showText}){Text(if(showText)"Nascondi testo letto" else "Mostra testo letto")}
            if(showText)SelectionContainer{Text(reading.text,style=MaterialTheme.typography.bodySmall)}
        }
        if(problem!=null)Text(problem,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
        Button(enabled=problem==null,onClick={onApply(selected)},modifier=Modifier.fillMaxWidth()){Text("Sostituisci rosa con ${selected.size} giocatrici")}
    }
}

@Composable
fun Camp3StaffReview(reading:Camp3Reading,onApply:(Map<Int,String>)->Unit) {
    val matches=remember(reading){(0..4).map{i->reading.staff.filter{it.index==i}}}
    var names by remember(reading){mutableStateOf(matches.map{it.singleOrNull()?.name.orEmpty()})}
    var selected by remember(reading){mutableStateOf(names.map{it.isNotBlank()})}
    var showText by remember{mutableStateOf(false)}
    val changes=names.mapIndexedNotNull{i,name->if(selected[i] && name.isNotBlank())i to name.trim() else null}.toMap()
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Rivedi lo staff",style=MaterialTheme.typography.titleMedium)
            Notice("Sono proposti solo i ruoli riconoscibili. Controlla i nomi. I ruoli non selezionati rimarranno invariati.")
            reading.notices.forEach{Notice(it)}
            Camp3Staff.labels.forEachIndexed { i,label->
                Row {
                    Checkbox(checked=selected[i],onCheckedChange={v->selected=selected.mapIndexed{j,old->if(i==j)v else old}})
                    Field(label,names[i],{v->names=names.mapIndexed{j,old->if(i==j)v else old}},Modifier.weight(1f))
                }
                if(matches[i].size>1)Text("Più nominativi per questo ruolo: scegli quello corretto dal testo letto.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
            }
            TextButton(onClick={showText=!showText}){Text(if(showText)"Nascondi testo letto" else "Mostra testo letto")}
            if(showText)SelectionContainer{Text(reading.text,style=MaterialTheme.typography.bodySmall)}
        }
        Button(enabled=changes.isNotEmpty() && selected.indices.none{selected[it] && names[it].isBlank()},onClick={onApply(changes)},modifier=Modifier.fillMaxWidth()){Text("Importa staff selezionato")}
    }
}
