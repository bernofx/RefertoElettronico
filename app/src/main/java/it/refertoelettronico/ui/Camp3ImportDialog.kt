package it.refertoelettronico.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun Camp3ImportDialog(teamName:String,onDismiss:()->Unit,onApply:(List<Player>)->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var reading by remember{mutableStateOf<Camp3Reading?>(null)}
    var source by remember{mutableStateOf<android.net.Uri?>(null)}
    var rotation by remember{mutableIntStateOf(0)}
    var cameraFile by remember{mutableStateOf<File?>(null)}
    DisposableEffect(Unit){onDispose{cameraFile?.delete()}}
    fun scan(uri:android.net.Uri,turns:Int=0) {
        source=uri;rotation=turns;busy=true;error=null
        scope.launch {
            try {reading=withContext(Dispatchers.IO){Camp3Ocr.read(context,uri,turns)}}
            catch(t:Exception){error=t.message ?: "Foto non leggibile: riprova con buona luce e il documento ben disteso."}
            finally{busy=false}
        }
    }
    val photo=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scan(uri)}
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()){ok->
        if(ok)cameraFile?.let{scan(FileProvider.getUriForFile(context,"${context.packageName}.files",it))}
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.95f).padding(12.dp),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("CAMP3 · $teamName",style=MaterialTheme.typography.titleLarge)
                Text("Una foto per questa squadra. Inquadra la tabella delle atlete ben diritta e leggibile. La lettura avviene sul dispositivo.",style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled=!busy,onClick={try {
                        cameraFile?.delete()
                        val file=File.createTempFile("camp3-",".jpg",File(context.cacheDir,"camp3").apply{mkdirs()})
                        cameraFile=file
                        camera.launch(FileProvider.getUriForFile(context,"${context.packageName}.files",file))
                    }catch(t:Exception){error="Fotocamera non disponibile. Usa Carica foto."}}){Text("Fotografa")}
                    OutlinedButton(enabled=!busy,onClick={photo.launch(arrayOf("image/*"))}){Text("Carica foto")}
                }
                if(busy) {LinearProgressIndicator(Modifier.fillMaxWidth());Text("Lettura del CAMP3…")}
                if(error!=null)Notice(error!!)
                if(source!=null)TextButton(enabled=!busy,onClick={scan(source!!,(rotation+1)%4)}){Text("Ruota di 90° e rileggi")}
                Box(Modifier.weight(1f)) {
                    if(reading!=null && !busy) key(reading) {
                        Camp3RosterReview(reading!!,onApply={players->onApply(players)})
                    }
                    else if(!busy)Text("Controlla sempre numeri di maglia e nominativi. Capitana e liberi vanno indicati nella revisione. La rosa attuale verrà sostituita solo dopo la conferma.")
                }
                TextButton(onClick=onDismiss,modifier=Modifier.fillMaxWidth()){Text("Chiudi senza importare")}
            }
        }
    }
}

private data class ReviewedPlayer(val player:Player,val selected:Boolean=true)

@Composable
fun Camp3RosterReview(reading:Camp3Reading,onApply:(List<Player>)->Unit) {
    var rows by remember(reading){mutableStateOf(reading.rows.map{ReviewedPlayer(Player(it.number ?: 0,it.name))})}
    var showText by remember{mutableStateOf(false)}
    val selected=rows.filter{it.selected}.map{it.player}
    val problem=Camp3Rows.validate(selected)
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("Rivedi la rosa · ${selected.size} selezionate",style=MaterialTheme.typography.titleMedium)
            Notice("L'OCR può sbagliare. Se non riconosce la colonna MAGLIA lascia il numero vuoto: completalo a mano. Escludi le righe estranee e indica K (capitana) e L (libero).")
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
                        Row {
                            Checkbox(checked=row.player.captain,onCheckedChange={value->rows=rows.mapIndexed{j,r->r.copy(player=r.player.copy(captain=j==i && value))}});Text("K",Modifier.padding(top=12.dp))
                            Checkbox(checked=row.player.libero,onCheckedChange={value->rows=rows.mapIndexed{j,r->if(i==j)r.copy(player=r.player.copy(libero=value))else r}});Text("L",Modifier.padding(top=12.dp))
                        }
                    }
                }
            }
            if(rows.size<40)OutlinedButton(onClick={rows=rows+ReviewedPlayer(Player(0,""))}){Text("Aggiungi riga")}
            TextButton(onClick={showText=!showText}){Text(if(showText)"Nascondi testo letto" else "Mostra testo letto")}
            if(showText)Text(reading.text,style=MaterialTheme.typography.bodySmall)
        }
        if(problem!=null)Text(problem,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
        Button(enabled=problem==null,onClick={onApply(selected)},modifier=Modifier.fillMaxWidth()){Text("Sostituisci rosa con ${selected.size} giocatrici")}
    }
}
