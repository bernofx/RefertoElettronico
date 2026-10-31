package it.refertoelettronico.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
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
    var query by remember(index){mutableStateOf("")}
    val issues=RosterReview.issues(team)
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
        FilledTonalButton(enabled=!locked,onClick={load=true},modifier=Modifier.weight(1f)){ActionLabel("Carica rosa",UiSymbol.PEOPLE)}
        TextButton(onClick={save=true},modifier=Modifier.weight(1f)){ActionLabel("Salva rosa",UiSymbol.SAVE)}
        FilledTonalIconButton(onClick={colors=!colors},modifier=Modifier.semantics{contentDescription="Colore"},colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=team.displayColor(index).copy(alpha=.12f),contentColor=team.displayColor(index))){UiGlyph(UiSymbol.COLOR)}
    }
    if(colors)TeamColorPicker(team,index,onChange)
    if(saved)Text("Rosa salvata sul dispositivo",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text("Presenti ${team.players.size}/${RosterReview.MATCH_LIMIT} · Rosa ${members.size}",Modifier.weight(1f).padding(top=12.dp).testTag("attendance-count"),fontWeight=FontWeight.Bold)
        Box {
            TextButton(onClick={photos=true}){ActionLabel("Foto",UiSymbol.CAMERA)}
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
    Text(if(locked)"Presenze, numeri e ruoli bloccati dopo l'inizio. I nomi restano correggibili." else "Spunta chi partecipa. La matita apre numero, nome e ruoli.",style=MaterialTheme.typography.bodySmall)
    if(team.historicalRoster==true) {
        Text("Rosa recuperata da partite precedenti. Seleziona le atlete presenti e verifica numeri e ruoli.",style=MaterialTheme.typography.bodySmall)
    }
    if(team.roster!=null)Field("Cerca nome o numero",query,{query=it})
    if(team.players.isNotEmpty() && team.players.none{it.captain})Text("Indica una capitana tra le presenti.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
    members.filter{query.isBlank() || it.player.name.contains(query,true) || it.player.number.toString().contains(query)}.forEach { member->key(member.id) {
        val player=member.player
        Surface(shape=MaterialTheme.shapes.medium,color=if(member.present)MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
            Checkbox(checked=member.present,enabled=!locked,onCheckedChange={value->update(members.map{if(it.id==member.id)it.copy(present=value)else it})},
                modifier=Modifier.testTag("presence-${member.id}").semantics{contentDescription="Presente ${player.number} ${player.name}"})
            Column(Modifier.weight(1f).padding(vertical=6.dp).clickable{
                if(locked)expanded=if(expanded==member.id)null else member.id
                else update(members.map{if(it.id==member.id)it.copy(present=!it.present)else it})
            }) {
                Text("${player.number.takeIf{it>0}?.toString() ?: "—"} · ${player.name.ifBlank{"Nome da completare"}}",maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=if(member.present)FontWeight.Medium else FontWeight.Normal)
                Text(listOfNotNull(if(member.present)"Presente" else "Assente",if(player.captain)"K" else null,if(player.libero)"L" else null).joinToString(" · "),style=MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick={expanded=if(expanded==member.id)null else member.id},modifier=Modifier.semantics{contentDescription="Modifica giocatrice ${player.number}"}){UiGlyph(if(expanded==member.id)UiSymbol.CHECK else UiSymbol.EDIT,color=MaterialTheme.colorScheme.primary)}
        }
        }
        if(player.number==0)Text("Numero da verificare",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)
        issues[member.id]?.let{Text(it,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
        if(expanded==member.id)Column(Modifier.fillMaxWidth().padding(start=12.dp,bottom=6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Field("N°",player.number.takeIf{it>0}?.toString().orEmpty(),{value->
                    val p=player.copy(number=value.filter{it.isDigit()}.take(2).toIntOrNull() ?: 0)
                    update(members.map{if(it.id==member.id)it.copy(player=p)else it})
                },Modifier.width(80.dp),numeric=true,enabled=!locked)
                Field("Cognome e nome",player.name,{value->update(members.map{if(it.id==member.id)it.copy(player=player.copy(name=value))else it})},Modifier.weight(1f))
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=player.captain,enabled=!locked,onClick={update(members.map{if(it.id==member.id)it.copy(player=player.copy(captain=!player.captain))else it})},label={Text("K · capitana")})
                FilterChip(selected=player.libero,enabled=!locked,onClick={update(members.map{if(it.id==member.id)it.copy(player=player.copy(libero=!player.libero))else it})},label={Text("L · libero")})
                if(!locked)TextButton(onClick={update(members.filter{it.id!=member.id});expanded=null}){Text("Rimuovi")}
            }
        }
    }}
    if(!locked && members.size<60)TextButton(onClick={val item=RosterMember(UUID.randomUUID().toString(),Player(0,""));update(members+item);expanded=item.id}){Text("Aggiungi giocatrice")}
    TextButton(onClick={details=!details}){Text(if(details)"Nascondi panchina e codice società" else "Panchina e codice società")}
    if(details) {
        Field("Codice società",team.clubCode,{onChange(team.copy(clubCode=it))})
        listOf("Allenatore","Vice allenatore","Fisioterapista","Medico","Dirigente").forEachIndexed{i,label->
            val proposal=team.staffRoster.orEmpty().firstOrNull{it.selected && it.scope=="BENCH" && it.assignment==StaffReview.bench[i]}
            Field(label,proposal?.name ?: team.staff.getOrElse(i){""},{value->onChange(team.copy(staff=List(5){if(it==i)value else team.staff.getOrElse(it){""}}))},enabled=proposal==null)
        }
    }
}
