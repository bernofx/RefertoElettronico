package it.refertoelettronico.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

private fun badgeDate(value:String)=runCatching{LocalDate.parse(value,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT))}.getOrNull()

/** Category is a display label only: it never changes the match's rules. */
private fun categoryLabel(competition:String):String {
    val age=Regex("(?:under\\s*|u\\s*)([0-9]{1,2})\\b",RegexOption.IGNORE_CASE).find(competition)?.groupValues?.get(1) ?: return competition.ifBlank{"Gara"}
    val sex=when{competition.contains("femmin",true) || Regex("\\bF\\b",RegexOption.IGNORE_CASE).containsMatchIn(competition)->" F"
        competition.contains("maschil",true) || Regex("\\bM\\b",RegexOption.IGNORE_CASE).containsMatchIn(competition)->" M";else->""}
    return "U$age$sex"
}

@Composable
fun MatchBadges(archives:List<Pair<String,Result<Journal>>>,onOpen:(String)->Unit,onDeleteRequest:(String,String)->Unit) {
    val readable=archives.mapNotNull{(id,result)->result.getOrNull()?.let{id to it}}
    readable.groupBy{it.second.config.date}.entries.sortedByDescending{badgeDate(it.key) ?: LocalDate.MIN}.forEach{(date,games)->
        val heading=badgeDate(date)?.format(DateTimeFormatter.ofPattern("EEEE d MMMM uuuu",Locale.ITALIAN))?.uppercase(Locale.ITALIAN) ?: "DATA DA DEFINIRE"
        Text(heading,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp))
        games.sortedBy{it.second.config.scheduledTime.ifBlank{"99:99"}}.forEach{(id,journal)->
            MatchBadge(id,journal,onOpen={onOpen(id)},onDelete={onDeleteRequest(id,"${journal.config.teams[0].name} — ${journal.config.teams[1].name} · ${journal.config.date}")})
        }
    }
    archives.filter{it.second.isFailure}.forEach{(id,_)->
        Notice("Archivio $id non leggibile: è stato conservato. Puoi recuperare una copia JSON valida.")
        TextButton(onClick={onDeleteRequest(id,"Archivio non leggibile: $id")},modifier=Modifier.testTag("delete-match-$id")){Text("Elimina archivio non leggibile")}
    }
}

@Composable
private fun MatchBadge(id:String,journal:Journal,onOpen:()->Unit,onDelete:()->Unit) {
    val config=journal.config
    val state=remember(journal){MatchEngine.replay(config,journal.events)}
    val category=categoryLabel(config.competition)
    val accent=when {
        category.startsWith("U13")->Color(0xFFCC7600)
        category.startsWith("U14")->Color(0xFF007EA0)
        category.startsWith("U15")->Color(0xFF9E4855)
        else->MaterialTheme.colorScheme.primary
    }
    val finished=state.finished(config)
    val set=state.current
    val status=when{state.endedEarly->"Terminata anticipatamente";finished->"Terminata";state.interrupted->"Interrotta";set==null->"Da iniziare";set.endedAt!=null->"Tra i set";else->"In corso"}
    val score=when{finished->"${state.wins(0)}–${state.wins(1)}";set!=null->"${set.score[0]}–${set.score[1]}";else->"—"}
    val scoreLabel=if(finished)"set"else if(set!=null)"punti · set ${set.number}"else""
    Card(onClick=onOpen,modifier=Modifier.fillMaxWidth().testTag("archive-match-$id").semantics{contentDescription="${config.teams[0].name} — ${config.teams[1].name}"},
        shape=MaterialTheme.shapes.medium,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(accent))
            Row(Modifier.weight(1f).padding(horizontal=10.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.width(64.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(config.scheduledTime.ifBlank{"—"},style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text(config.venue.ifBlank{config.location.ifBlank{"Campo da indicare"}},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=TextOverflow.Ellipsis)
                }
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                    config.teams.forEachIndexed{t,team->Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(7.dp).background(team.displayColor(t),CircleShape).border(1.dp,MaterialTheme.colorScheme.outline,CircleShape))
                        Text(team.name,style=MaterialTheme.typography.titleSmall)
                    }}
                    Surface(color=accent.copy(alpha=.13f),shape=MaterialTheme.shapes.extraSmall) {
                        Text(category,Modifier.padding(horizontal=7.dp,vertical=3.dp),style=MaterialTheme.typography.labelSmall,color=teamDetailInk(accent,MaterialTheme.colorScheme.surface))
                    }
                    Text("Gara ${config.matchNumber.ifBlank{"da numerare"}}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.width(76.dp),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick=onDelete,modifier=Modifier.testTag("delete-match-$id").semantics{contentDescription="Elimina gara ${config.teams[0].name} — ${config.teams[1].name}"}) {
                        UiGlyph(UiSymbol.DELETE,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(score,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    if(scoreLabel.isNotEmpty())Text(scoreLabel,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(status,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
