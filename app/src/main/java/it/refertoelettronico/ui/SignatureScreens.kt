package it.refertoelettronico.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import it.refertoelettronico.model.*
import it.refertoelettronico.pdf.SignatureGeometry
import it.refertoelettronico.pdf.TextCell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun PreliminarySignatures(journal:Journal,toss:Event,onOpen:()->Unit) {
    val required=Signatures.required(journal.config,true)
    val missing=Signatures.missing(journal,true,toss)
    Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerLow,modifier=Modifier.tourTarget("pre-signatures")) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("Firme dopo il sorteggio",style=MaterialTheme.typography.titleSmall)
            Text("Capitane e allenatori presenti · ${required.size-missing.size}/${required.size} raccolte",style=MaterialTheme.typography.bodySmall)
            FilledTonalButton(onClick=onOpen,enabled=toss.server in 0..1 && toss.left in 0..1,modifier=Modifier.testTag("pre-signatures")){Text("Raccogli / controlla firme")}
            Text("Prima di ogni set verifica anche la firma sui tagliandi di formazione ricevuti dagli allenatori.",style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun FinalSignatures(journal:Journal,onOpen:()->Unit) {
    val state=remember(journal){MatchEngine.replay(journal.config,journal.events)}
    val required=Signatures.required(journal.config,false)
    val missing=Signatures.missing(journal,false)
    Card(Modifier.fillMaxWidth().tourTarget("final-signatures")) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("Firme del referto",style=MaterialTheme.typography.titleMedium)
            Text("Preliminari: ${Signatures.required(journal.config,true).size-Signatures.missing(journal,true).size}/${Signatures.required(journal.config,true).size}",style=MaterialTheme.typography.bodySmall)
            Text(if(state.finished(journal.config))"Approvazione finale: ${required.size-missing.size}/${required.size}" else "Approvazione finale disponibile a gara terminata.",style=MaterialTheme.typography.bodySmall)
            Text("A fine gara firmano segnapunti, 2° arbitro se presente, 1° arbitro. In FIPAV i capitani non firmano a fine gara.",style=MaterialTheme.typography.bodySmall)
            Button(onClick=onOpen,modifier=Modifier.testTag("final-signatures")){Text("Controlla e raccogli firme")}
        }
    }
}

/** Saved signatures always belong to a named actor and a specific match snapshot. */
@Composable
internal fun SignatureWorkflow(journal:Journal,preliminary:Boolean,toss:Event?=Signatures.toss(journal),
    onUpdate:(Journal)->Boolean,onClose:()->Unit,onStart:(()->Unit)?=null) {
    val roles=Signatures.required(journal.config,preliminary)
    val missing=Signatures.missing(journal,preliminary,toss)
    val finished=remember(journal){MatchEngine.replay(journal.config,journal.events).finished(journal.config)}
    var capturing by remember { mutableStateOf<SignatureRole?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.96f).widthIn(max=600.dp).fillMaxHeight(.94f),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text(if(preliminary)"Firme preliminari" else "Approvazione finale",style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f))
                    TextButton(onClick=onClose,modifier=Modifier.testTag("signatures-close")){Text("Chiudi")}
                }
                Text(if(preliminary)"Dopo il sorteggio e prima del primo set: capitane e allenatori presenti." else "A gara terminata e dopo i controlli arbitrali: segnapunti, 2° arbitro se presente, 1° arbitro, in quest’ordine.",style=MaterialTheme.typography.bodyMedium)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    if(!preliminary)LegacySignatureRecovery(journal,onUpdate)
                    if(!preliminary)Text("Le capitane non firmano a fine gara nelle competizioni FIPAV. Attendere i controlli e gli eventuali termini previsti per le istanze prima dell’approvazione.",style=MaterialTheme.typography.bodySmall)
                    roles.forEachIndexed { index,role ->
                        val current=Signatures.current(journal,role,toss)
                        val old=journal.signatures.orEmpty().any{it.role==role.name}
                        val name=Signatures.signer(journal.config,role)
                        val allowed=if(preliminary)Signatures.start(journal)==null else finished && roles.take(index).all{Signatures.current(journal,it,toss)!=null}
                        OutlinedCard(Modifier.fillMaxWidth().testTag("signature-row-${role.name}")) {
                            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                Text("${index+1}. ${role.label}",style=MaterialTheme.typography.titleSmall)
                                if(role.team>=0)Text(journal.config.teams[role.team].name,style=MaterialTheme.typography.bodySmall)
                                Text(name.ifBlank{if(preliminary)"Completa il nominativo nei Dati"else"Nominativo da completare"},style=MaterialTheme.typography.bodyMedium)
                                if(!preliminary && name.isBlank())MissingOfficialName(journal,role,onUpdate)
                                Text(when{current!=null->"Raccolta il ${signatureTime(current.signedAt)}";old->"Da rifirmare: il contenuto approvato è cambiato";else->"Da raccogliere"},style=MaterialTheme.typography.bodySmall,
                                    color=if(old&&current==null)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                if(current!=null)SignatureThumbnail(current)
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(enabled=allowed && name.isNotBlank(),onClick={capturing=role},modifier=Modifier.testTag("sign-${role.name}")){Text(if(current==null)"Firma" else "Rifirma")}
                                    if(old)TextButton(onClick={onUpdate(Signatures.remove(journal,role))},modifier=Modifier.testTag("remove-sign-${role.name}")){Text("Rimuovi")}
                                }
                                if(!allowed && current==null)Text(if(preliminary)"La gara è già iniziata: non si raccolgono firme preliminari retroattive." else if(!finished)"Disponibile alla conclusione della gara." else "Raccogli prima le firme precedenti.",style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if(!preliminary && Signatures.missing(journal,true).isNotEmpty())Notice("Firme preliminari non raccolte in app: controllale con l’arbitro e completa gli spazi sulla stampa.")
                    Text("Firma tracciata sul dispositivo e riportata nel PDF; non è una firma digitale qualificata. Per la consegna del referto valgono le indicazioni dell’arbitro e del Comitato.",style=MaterialTheme.typography.bodySmall)
                    if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error)
                }
                if(onStart!=null) {
                    if(missing.isEmpty())Button(onClick=onStart,modifier=Modifier.fillMaxWidth().testTag("signatures-start")){Text("Firme raccolte · inizia il set")}
                    else OutlinedButton(onClick=onStart,modifier=Modifier.fillMaxWidth().testTag("signatures-paper-start")){Text("Inizia · firme da completare sulla stampa")}
                } else TextButton(onClick=onClose,modifier=Modifier.align(Alignment.End)){Text("Torna al referto")}
            }
        }
    }
    val role=capturing
    if(role!=null)SignaturePad(role,Signatures.signer(journal.config,role),onCancel={capturing=null},onSave={strokes,ratio->
        try {
            val next=Signatures.capture(journal,role,strokes,ratio,toss)
            if(onUpdate(next)){capturing=null;error=null}
        } catch(t:IllegalArgumentException){error=t.message;capturing=null}
    })
}

@Composable
private fun MissingOfficialName(journal:Journal,role:SignatureRole,onUpdate:(Journal)->Boolean) {
    var name by remember(role){mutableStateOf("")}
    Field("Nome e cognome",name,{name=it},Modifier.testTag("signature-official-name-${role.name}"))
    TextButton(enabled=name.isNotBlank(),modifier=Modifier.testTag("save-official-name-${role.name}"),onClick={
        val index=when(role){SignatureRole.SCORER->2;SignatureRole.SECOND_REFEREE->1;else->0}
        val names=journal.config.officials.toMutableList()
        while(names.size<=index)names.add("")
        names[index]=name.trim()
        onUpdate(Signatures.withConfig(journal,journal.config.copy(officials=names)))
    }){Text("Salva nominativo")}
}

@Composable
private fun LegacySignatureRecovery(journal:Journal,onUpdate:(Journal)->Boolean) {
    val available=remember(journal){Signatures.hasLegacyPreliminariesToRecover(journal)}
    val latestJournal by rememberUpdatedState(journal)
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope()
    if(available || message!=null)OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("Firme delle versioni precedenti",style=MaterialTheme.typography.titleSmall)
            Text("Se sono diventate non valide dopo aver completato gli ufficiali, verifica il recupero delle firme già raccolte. Distinte e sorteggio devono risultare invariati.",style=MaterialTheme.typography.bodySmall)
            if(available)FilledTonalButton(enabled=!busy,modifier=Modifier.testTag("recover-preliminary-signatures"),onClick={scope.launch {
                busy=true
                try {
                    val next=withContext(Dispatchers.Default){Signatures.recoverLegacyPreliminaries(journal)}
                    val recovered=Signatures.missing(journal,true).size-Signatures.missing(next,true).size
                    if(latestJournal!=journal)message="I dati sono cambiati durante la verifica: riprova il recupero."
                    else if(recovered>0) {
                        if(onUpdate(next))message="Recuperate $recovered firme preliminari, conservando tratti e data originali."
                    } else message="Nessuna firma recuperabile: i dati originali non sono verificabili. Le firme non vengono ricreate."
                } finally {busy=false}
            }}){Text(if(busy)"Verifica firme…" else "Recupera firme preliminari")}
            if(message!=null)Text(message!!,style=MaterialTheme.typography.bodySmall,modifier=Modifier.testTag("signature-recovery-result"))
        }
    }
}

private fun signatureTime(at:Long)=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Europe/Rome")).format(Instant.ofEpochMilli(at))

@Composable
private fun SignatureThumbnail(signature:MatchSignature) {
    Canvas(Modifier.fillMaxWidth().height(48.dp).background(Color.White).semantics{contentDescription="Firma raccolta di ${signature.signer}"}) {
        SignatureGeometry.fit(signature,TextCell(6f,4f,size.width-6,size.height-4)).forEach { points->
            val path=Path();points.forEachIndexed{i,p->if(i==0)path.moveTo(p.x,p.y)else path.lineTo(p.x,p.y)}
            drawPath(path,Color.Black,style=Stroke(2f,cap=StrokeCap.Round))
        }
    }
}

private tailrec fun Context.signatureActivity():Activity?=when(this) {
    is Activity->this
    is ContextWrapper->baseContext.signatureActivity()
    else->null
}

@Composable
internal fun SignaturePad(role:SignatureRole,signer:String,onCancel:()->Unit,onSave:(List<List<SignaturePoint>>,Float)->Unit) {
    val activity=LocalContext.current.signatureActivity()
    DisposableEffect(activity) {
        val previousOrientation=activity?.requestedOrientation
        activity?.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if(previousOrientation!=null)activity.requestedOrientation=previousOrientation }
    }
    val landscape=LocalConfiguration.current.orientation==Configuration.ORIENTATION_LANDSCAPE
    var strokes by remember { mutableStateOf<List<List<SignaturePoint>>>(emptyList()) }
    var aspectRatio by remember { mutableFloatStateOf(1f) }
    var clearToken by remember { mutableIntStateOf(0) }
    var truncated by remember { mutableStateOf(false) }
    // The native view owns the live stroke; recomposition and rotation must not reset it.
    Dialog(onDismissRequest=onCancel,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(if(landscape)12.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(if(landscape)8.dp else 12.dp)) {
                if(landscape)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(role.label,style=MaterialTheme.typography.titleMedium)
                        Text(signer,style=MaterialTheme.typography.bodyMedium)
                    }
                    Text("Firma con dito o penna",style=MaterialTheme.typography.bodySmall)
                } else {
                    Text(role.label,style=MaterialTheme.typography.titleLarge)
                    Text(signer,style=MaterialTheme.typography.titleMedium)
                    Text("Traccia qui la tua firma con il dito o la penna.",style=MaterialTheme.typography.bodyMedium)
                }
                val padModifier=if(landscape)Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().height(220.dp).weight(1f,fill=false)
                AndroidView(
                    factory={context->SignatureInkView(context)},
                    modifier=padModifier.border(1.dp,MaterialTheme.colorScheme.outline)
                        .testTag("signature-pad").semantics{contentDescription="Riquadro firma di $signer"},
                    update={view->
                        view.onInkChanged={ink,ratio,limited->strokes=ink;aspectRatio=ratio;truncated=limited}
                        view.clearIfRequested(clearToken)
                    }
                )
                Text(if(truncated)"Firma troppo lunga: cancella e riprova."else"La firma sarà riportata negli spazi del referto.",style=MaterialTheme.typography.bodySmall,color=if(truncated)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick={clearToken++},modifier=Modifier.testTag("signature-clear")){Text("Cancella")}
                    TextButton(onClick=onCancel,modifier=Modifier.testTag("signature-cancel")){Text("Annulla")}
                    Button(enabled=!truncated && strokes.sumOf{it.size}>=2,onClick={onSave(strokes,aspectRatio)},modifier=Modifier.weight(1f).testTag("signature-save")){Text("Conferma firma")}
                }
            }
        }
    }
}
