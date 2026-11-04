package it.refertoelettronico.ui

import android.graphics.Bitmap
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.refertoelettronico.model.*

@Composable
internal fun StyledReport(journal:Journal,bitmap:Bitmap?,busy:Boolean,ready:Boolean,onGenerate:()->Unit,
    onSave:()->Unit,onShare:()->Unit,onBackup:()->Unit,onSign:((Boolean,SignatureRole?)->Unit)?,onEdit:(()->Unit)?) {
    val state=remember(journal){MatchEngine.replay(journal.config,journal.events)}
    val c=LocalAppearance.current.layout==InterfaceStyle.COURT
    var preview by remember{mutableStateOf(false)}
    var export by remember{mutableStateOf(false)}
    var info by remember{mutableStateOf(false)}
    Column(Modifier.fillMaxSize()) {
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        CompactMatchSummary(journal.config)
        Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(if(state.endedEarly)"Termine anticipato · decisione arbitrale"else if(state.finished(journal.config))"Gara conclusa"else"Referto provvisorio",style=MaterialTheme.typography.titleMedium,color=if(state.finished(journal.config))styleSuccess()else MaterialTheme.colorScheme.onSurfaceVariant)
                journal.config.teams.forEachIndexed{t,team->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(shortTeamName(team.name),style=MaterialTheme.typography.titleSmall);Text("${state.wins(t)}",style=MaterialTheme.typography.titleLarge)}}
                Text(state.sets.joinToString("    "){"${it.score[0]}–${it.score[1]}"},style=MaterialTheme.typography.bodySmall)
            }
        }
        val missing=journal.config.missingMetadata()
        if(missing.isNotEmpty())OutlinedCard(onClick={onEdit?.invoke()},modifier=Modifier.fillMaxWidth()) {
            Text("Da completare · ${missing.joinToString()}",Modifier.padding(10.dp),style=MaterialTheme.typography.bodySmall)
        }
        if(c && bitmap!=null)Image(bitmap.asImageBitmap(),"Anteprima prima pagina del referto compilato",
            Modifier.fillMaxWidth().height(180.dp).background(Color.White).border(1.dp,MaterialTheme.colorScheme.outlineVariant).tourTarget("pdf-preview").clickable{preview=true})
        if(onSign!=null)StyledSignatureGroups(journal,onSign)
        if(!c)OutlinedCard(onClick={if(ready)preview=true else onGenerate()},modifier=Modifier.fillMaxWidth()) {
            Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                if(bitmap!=null)Image(bitmap.asImageBitmap(),"Anteprima prima pagina del referto compilato",Modifier.width(70.dp).tourTarget("pdf-preview"))
                else UiGlyph(UiSymbol.DOCUMENT,Modifier.size(36.dp))
                Column(Modifier.weight(1f)){Text("Referto di gara",style=MaterialTheme.typography.titleMedium);Text(if(ready)"Anteprima del modello FIPAV"else"Compila il modello FIPAV",style=MaterialTheme.typography.bodySmall)}
                UiGlyph(UiSymbol.CHEVRON)
            }
        }
        TextButton(onClick={info=true},modifier=Modifier.fillMaxWidth()){Text("Informazioni e copia della gara",style=MaterialTheme.typography.labelSmall)}
    }
    Row(Modifier.fillMaxWidth().padding(12.dp).tourTarget("pdf-export"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        Button(onClick={if(ready)preview=true else onGenerate()},enabled=!busy,modifier=Modifier.weight(1f).heightIn(min=48.dp).tourTarget("pdf-generate",onGenerate),colors=styleButtonColors(),shape=MaterialTheme.shapes.small) {
            Text(if(busy)"Preparazione…"else if(c)"Anteprima PDF"else"Apri referto PDF")
        }
        if(c)Box(Modifier.weight(1f)) {
            OutlinedButton(onClick={if(ready)export=true else onGenerate()},enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=MaterialTheme.shapes.small){Text("Esporta PDF")}
            DropdownMenu(expanded=export,onDismissRequest={export=false}) {
                DropdownMenuItem(text={Text("Salva PDF")},onClick={export=false;onSave()})
                DropdownMenuItem(text={Text("Condividi")},onClick={export=false;onShare()})
            }
        }
    }
    }
    if(preview&&bitmap!=null)Dialog(onDismissRequest={preview=false},properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxWidth(.97f),shape=MaterialTheme.shapes.medium) {
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Referto FIPAV compilato",style=MaterialTheme.typography.titleMedium)
                Image(bitmap.asImageBitmap(),"Anteprima del referto",Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={preview=false}){Text("Chiudi")}
                    TextButton(onClick=onSave){Text("Salva PDF")}
                    TextButton(onClick=onShare){Text("Condividi")}
                }
            }
        }
    }
    if(info)AlertDialog(onDismissRequest={info=false},title={Text("Referto e archivio")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Modello cartaceo FIPAV con registrazioni e allegato cronologico. Le firme raccolte sono riportate nel modello; completa sulla stampa gli spazi mancanti secondo le indicazioni arbitrali.")
        if(journal.config.fallbackReason.isBlank())Text("Completa il motivo dell’uso del cartaceo nei Dati o nelle Osservazioni.")
        TextButton(onClick={info=false;onBackup()}){Text("Salva anche la gara modificabile")}
    }},confirmButton={TextButton(onClick={info=false}){Text("Chiudi")}})
}

@Composable
private fun StyledSignatureGroups(journal:Journal,onSign:(Boolean,SignatureRole?)->Unit) {
    val before=Signatures.required(journal.config,true)
    val after=Signatures.required(journal.config,false)
    Column(Modifier.fillMaxWidth().tourTarget("final-signatures").testTag("styled-signature-groups"),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        SignatureGroup("Prima della gara") {
            listOf("Capitani" to before.filter{it.name.startsWith("CAPTAIN")},"Allenatori" to before.filter{it.name.startsWith("COACH")}).forEach{(title,roles)->
                if(roles.isNotEmpty())SignatureStatusRow(title,roles.all{Signatures.current(journal,it)!=null},roles.any{Signatures.needsReview(journal,it)}){onSign(true,null)}
            }
        }
        SignatureGroup("Fine gara") {
            after.forEach{role->SignatureStatusRow(role.label,Signatures.current(journal,role)!=null,Signatures.needsReview(journal,role)){onSign(false,role)}}
        }
    }
}
@Composable
private fun SignatureGroup(title:String,content:@Composable ColumnScope.()->Unit) {
    Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainer,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,style=MaterialTheme.typography.titleSmall);content()}
    }
}
@Composable
private fun SignatureStatusRow(title:String,collected:Boolean,review:Boolean=false,onClick:()->Unit) {
    Surface(onClick=onClick,shape=MaterialTheme.shapes.small,color=if(collected)MaterialTheme.colorScheme.surface else lerp(MaterialTheme.colorScheme.surface,MaterialTheme.colorScheme.tertiary,.10f)) {
        Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            UiGlyph(if(collected&&!review)UiSymbol.CHECK else UiSymbol.EDIT,Modifier.size(20.dp),if(collected&&!review)styleSuccess()else MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)){Text(title,style=MaterialTheme.typography.labelLarge);Text(if(review)"Conservata · dati da verificare"else if(collected)"Firma raccolta"else"Da raccogliere",style=MaterialTheme.typography.bodySmall)}
            UiGlyph(UiSymbol.CHEVRON,Modifier.size(18.dp))
        }
    }
}
