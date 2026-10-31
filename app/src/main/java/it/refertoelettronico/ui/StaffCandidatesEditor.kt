package it.refertoelettronico.ui

import androidx.compose.foundation.layout.*
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
    var expanded by remember(teamIndex,serviceOnly){mutableStateOf<String?>(null)}
    fun update(t:Int,p:StaffCandidate){onChange(t,config.teams[t].staffRoster.orEmpty().map{if(it.id==p.id)p else it})}
    if(entries.isNotEmpty())Text("Proposte da gare passate: conferma solo gli incarichi di oggi. Il segnapunti è unico per tutta la gara. Gli incarichi selezionati compilano i rispettivi campi; correggili qui.",style=MaterialTheme.typography.bodySmall)
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
                Picker("Ambito",p.scope,(listOf("BENCH","SERVICE","UNKNOWN")+p.scope).distinct(),{it},{update(t,p.copy(scope=it))})
                Field("Descrizione ruolo ricevuta",p.roleLabel,{update(t,p.copy(roleLabel=it))})
                Text("Codice ricevuto: ${p.roleCode.ifBlank{"non disponibile"}}",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={onChange(t,config.teams[t].staffRoster.orEmpty().filter{it.id!=p.id})}){Text("Rimuovi proposta")}
            }
        }}
    }
    if(teamIndex!=null && config.teams[teamIndex].staffRoster.orEmpty().size<60)TextButton(onClick={
        val item=StaffCandidate(id=UUID.randomUUID().toString());onChange(teamIndex,config.teams[teamIndex].staffRoster.orEmpty()+item);expanded=item.id
    }){Text("Aggiungi persona / incarico")}
}
