package it.refertoelettronico.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*
import it.refertoelettronico.ocr.Camp3Section
import java.util.UUID

@Composable
fun TeamEditor(team:Team,index:Int,season:String,locked:Boolean,onChange:(Team)->Unit) {
    val members=remember(team){Rosters.members(team)}
    var expanded by remember(index){mutableStateOf<String?>(null)}
    var details by remember(index){mutableStateOf(false)}
    var colors by remember(index){mutableStateOf(false)}
    var load by remember(index){mutableStateOf(false)}
    var save by remember(index){mutableStateOf(false)}
    var photos by remember(index){mutableStateOf(false)}
    var saved by remember(index){mutableStateOf(false)}
    var importSection by remember(index){mutableStateOf<Camp3Section?>(null)}
    fun update(next:List<RosterMember>){onChange(Rosters.withMembers(team,next));saved=false}
    if(load)LoadRosterDialog(onClose={load=false},onLoad={onChange(it);load=false;expanded=null;saved=false})
    if(save)SaveRosterDialog(team,season,onClose={save=false},onSaved={save=false;saved=true})
    if(importSection!=null)Camp3ImportDialog(team.name,onDismiss={importSection=null},
        onApply={onChange(Rosters.importedPlayers(team,it));importSection=null;saved=false},section=importSection!!,
        onStaff={changes->onChange(team.copy(staff=List(5){changes[it] ?: team.staff.getOrElse(it){""}}));importSection=null;details=true})
    Field("Denominazione squadra",team.name,{onChange(team.copy(name=it))})
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled=!locked,onClick={load=true},modifier=Modifier.weight(1f)){Text("Carica rosa")}
        TextButton(onClick={save=true},modifier=Modifier.weight(1f)){Text("Salva rosa")}
        TextButton(onClick={colors=!colors}){Text("Colore")}
    }
    if(colors)TeamColorPicker(team,index,onChange)
    if(saved)Text("Rosa salvata sul dispositivo",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text("Presenti ${team.players.size}/14 · Rosa ${members.size}",Modifier.weight(1f).padding(top=12.dp).testTag("attendance-count"),fontWeight=FontWeight.Bold)
        Box {
            TextButton(onClick={photos=true}){Text("Foto")}
            DropdownMenu(expanded=photos,onDismissRequest={photos=false}) {
                if(!locked)DropdownMenuItem(text={Text("Lista giocatrici")},onClick={photos=false;importSection=Camp3Section.PLAYERS})
                DropdownMenuItem(text={Text("Staff")},onClick={photos=false;importSection=Camp3Section.STAFF})
            }
        }
    }
    if(!locked)Row {
        TextButton(onClick={update(members.map{it.copy(present=true)})}){Text("Tutte presenti")}
        TextButton(onClick={update(members.map{it.copy(present=false)})}){Text("Nessuna")}
    }
    Text(if(locked)"Presenze, numeri e ruoli bloccati dopo l'inizio. I nomi restano correggibili." else "Spunta chi partecipa. Tocca Modifica per numero, nome e ruoli.",style=MaterialTheme.typography.bodySmall)
    members.forEach { member->key(member.id) {
        val player=member.player
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            Checkbox(checked=member.present,enabled=!locked,onCheckedChange={value->update(members.map{if(it.id==member.id)it.copy(present=value)else it})},
                modifier=Modifier.testTag("presence-${member.id}").semantics{contentDescription="Presente ${player.number} ${player.name}"})
            Column(Modifier.weight(1f).padding(top=6.dp).clickable{
                if(locked)expanded=if(expanded==member.id)null else member.id
                else update(members.map{if(it.id==member.id)it.copy(present=!it.present)else it})
            }) {
                Text("${player.number.takeIf{it>0}?.toString() ?: "—"} · ${player.name.ifBlank{"Nome da completare"}}",maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=if(member.present)FontWeight.Medium else FontWeight.Normal)
                Text(listOfNotNull(if(member.present)"Presente" else "Assente",if(player.captain)"K" else null,if(player.libero)"L" else null).joinToString(" · "),style=MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick={expanded=if(expanded==member.id)null else member.id},modifier=Modifier.semantics{contentDescription="Modifica giocatrice ${player.number}"}){Text(if(expanded==member.id)"Chiudi" else "Modifica")}
        }
        if(expanded==member.id)Column(Modifier.fillMaxWidth().padding(start=12.dp,bottom=6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Field("N°",player.number.takeIf{it>0}?.toString().orEmpty(),{value->
                    val p=player.copy(number=value.filter{it.isDigit()}.take(2).toIntOrNull() ?: 0)
                    update(members.map{if(it.id==member.id)it.copy(player=p)else it})
                },Modifier.width(80.dp),numeric=true,enabled=!locked)
                Field("Cognome e nome",player.name,{value->update(members.map{if(it.id==member.id)it.copy(player=player.copy(name=value))else it})},Modifier.weight(1f))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=player.captain,enabled=!locked,onClick={update(members.map{it.copy(player=it.player.copy(captain=it.id==member.id && !player.captain))})},label={Text("K · capitana")})
                FilterChip(selected=player.libero,enabled=!locked,onClick={update(members.map{if(it.id==member.id)it.copy(player=player.copy(libero=!player.libero))else it})},label={Text("L · libero")})
                if(!locked)TextButton(onClick={update(members.filter{it.id!=member.id});expanded=null}){Text("Rimuovi")}
            }
        }
        HorizontalDivider()
    }}
    if(!locked && members.size<60)TextButton(onClick={val item=RosterMember(UUID.randomUUID().toString(),Player(0,""));update(members+item);expanded=item.id}){Text("Aggiungi giocatrice")}
    TextButton(onClick={details=!details}){Text(if(details)"Nascondi panchina e codice società" else "Panchina e codice società")}
    if(details) {
        Field("Codice società",team.clubCode,{onChange(team.copy(clubCode=it))})
        listOf("Allenatore","Vice allenatore","Fisioterapista","Medico","Dirigente").forEachIndexed{i,label->
            Field(label,team.staff.getOrElse(i){""},{value->onChange(team.copy(staff=List(5){if(it==i)value else team.staff.getOrElse(it){""}}))})
        }
    }
}
