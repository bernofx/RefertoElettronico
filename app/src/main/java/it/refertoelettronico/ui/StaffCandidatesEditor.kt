package it.refertoelettronico.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*
import java.util.UUID

@Composable
fun StaffCandidatesEditor(config:MatchConfig,teamIndex:Int?=null,serviceOnly:Boolean=false,onChange:(Int,List<StaffCandidate>)->Unit) {
    val entries=config.teams.flatMapIndexed{t,team->team.staffRoster.orEmpty().map{t to it}}
        .filter{(t,p)->(teamIndex==null || teamIndex==t) && (!serviceOnly || p.scope=="SERVICE")}
    val issues=StaffReview.issues(config)
    var adding by remember(teamIndex,serviceOnly){mutableStateOf(false)}
    var technical by remember(teamIndex,serviceOnly){mutableStateOf(false)}
    var expanded by remember(teamIndex,serviceOnly){mutableStateOf<String?>(null)}
    fun update(t:Int,p:StaffCandidate){onChange(t,config.teams[t].staffRoster.orEmpty().map{if(it.id==p.id)p else it})}
    if(entries.isNotEmpty())Text("Spunta chi svolge l’incarico oggi. Il segnapunti è unico per la gara.",style=MaterialTheme.typography.bodySmall)
    (if(serviceOnly)listOf("SERVICE")else listOf("BENCH","SERVICE","UNKNOWN")).forEach{scope->
        val items=entries.filter{it.second.scope==scope || scope=="UNKNOWN" && it.second.scope !in listOf("BENCH","SERVICE")}
        if(items.isNotEmpty())Text(when(scope){"BENCH"->"Staff di panchina";"SERVICE"->"Incarichi di servizio";else->"Da verificare"},style=MaterialTheme.typography.titleSmall)
        items.forEach{(t,p)->key(p.id){
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(p.selected,{update(t,p.copy(selected=it))},Modifier.testTag("staff-presence-${p.id}"))
                Column(Modifier.weight(1f)) {
                    Text(p.name.ifBlank{"Nome da completare"})
                    Text("${config.teams[t].name} · ${p.roleLabel.ifBlank{p.roleCode.ifBlank{"Ruolo da completare"}}}",style=MaterialTheme.typography.bodySmall)
                    Text(StaffReview.labels[p.assignment] ?: "Da mappare",style=MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick={expanded=if(expanded==p.id)null else p.id},modifier=Modifier.testTag("staff-edit-${p.id}")){Text("Modifica")}
            }
            if(p.assignment=="UNMAPPED")Text("Ruolo non supportato: mappa un incarico o lascia esclusa la proposta.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
            issues[p.id]?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
            if(expanded==p.id)Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                Field("Nome incaricato",p.name,{update(t,p.copy(name=it))})
                Picker("Incarico per questa gara",p.assignment,StaffReview.labels.keys.toList(),{StaffReview.labels[it] ?: it},{role->update(t,p.copy(assignment=role,scope=StaffReview.scope(role)))})
                TextButton(onClick={technical=!technical}){Text(if(technical)"Nascondi dettagli importazione" else "Dettagli importazione")}
                if(technical)Column {
                Picker("Ambito",p.scope,(listOf("BENCH","SERVICE","UNKNOWN")+p.scope).distinct(),{it},{update(t,p.copy(scope=it))})
                Field("Descrizione ruolo ricevuta",p.roleLabel,{update(t,p.copy(roleLabel=it))})
                Text("Codice ricevuto: ${p.roleCode.ifBlank{"non disponibile"}}",style=MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick={onChange(t,config.teams[t].staffRoster.orEmpty().filter{it.id!=p.id})}){Text("Rimuovi proposta")}
            }
        }}
    }
    val destination=teamIndex ?: 0
    if(config.teams[destination].staffRoster.orEmpty().size<60)TextButton(onClick={adding=true},modifier=Modifier.testTag("staff-add")){Text(if(serviceOnly)"Aggiungi incarico di servizio" else "Aggiungi persona / incarico")}
    if(adding)StaffEntryDialog(serviceOnly=serviceOnly,onClose={adding=false},onSave={name,role->
        val item=StaffCandidate(id=UUID.randomUUID().toString(),name=name,assignment=role,scope=StaffReview.scope(role),selected=true)
        onChange(destination,config.teams[destination].staffRoster.orEmpty()+item);adding=false
    })
}

@Composable
fun StaffEntryDialog(serviceOnly:Boolean=false,initialName:String="",initialRole:String?=null,roles:List<String> = StaffReview.labels.keys.filter{it!="UNMAPPED"},onClose:()->Unit,onRemove:(()->Unit)?=null,onSave:(String,String)->Unit) {
    val available=roles.filter{!serviceOnly || StaffReview.scope(it)=="SERVICE"}
    var name by remember{mutableStateOf(initialName)}
    var role by remember{mutableStateOf(initialRole ?: available.first())}
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.widthIn(max=520.dp).fillMaxWidth(.95f),shape=MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Persona e incarico",style=MaterialTheme.typography.titleLarge)
                Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Picker("Incarico",role,available,{StaffReview.labels[it] ?: it},{role=it})
                    Field("Cognome e nome",name,{name=it})
                    Text("Confermando, la persona viene inserita per questa gara.",style=MaterialTheme.typography.bodySmall)
                    if(onRemove!=null)TextButton(onClick=onRemove){Text("Rimuovi incaricato")}
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    TextButton(onClick=onClose){Text("Annulla")}
                    TextButton(enabled=name.isNotBlank(),onClick={onSave(name.trim(),role)},modifier=Modifier.testTag("staff-entry-confirm")){Text("Conferma")}
                }
            }
        }
    }
}
