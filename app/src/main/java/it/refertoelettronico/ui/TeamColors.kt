package it.refertoelettronico.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.refertoelettronico.model.Team
import it.refertoelettronico.model.TeamColor

fun Team.displayColor(index: Int): Color = Color(TeamColor.of(this,index).argb)
fun teamInk(background: Color): Color = if(background.luminance()>.179f)Color.Black else Color.White

/** Keep pale team colours on badges; use a legible ink for lines and text on light surfaces. */
fun teamDetailInk(accent:Color,background:Color):Color {
    val a=accent.luminance();val b=background.luminance()
    val contrast=(maxOf(a,b)+.05f)/(minOf(a,b)+.05f)
    return if(contrast>=4.5f)accent else teamInk(background)
}

@Composable
fun TeamColorPicker(team: Team, index: Int, onChange: (Team)->Unit) {
    val current=TeamColor.of(team,index)
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text("Colore squadra: ${current.label}",fontWeight=FontWeight.Bold)
        TeamColor.entries.chunked(5).forEach { row ->
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach { choice ->
                    val picked=choice==current
                    Box(Modifier.size(48.dp).testTag("team-color-${choice.name}")
                        .semantics { selected=picked;contentDescription="Colore ${choice.label}" }
                        .clickable(role=Role.RadioButton){onChange(team.copy(colorKey=choice.name))}
                        .padding(4.dp).background(Color(choice.argb),CircleShape)
                        .border(if(picked)3.dp else 1.dp,if(picked)Color(0xFF145663)else Color(0xFF9EA9AC),CircleShape),
                        contentAlignment=Alignment.Center) {
                        if(picked)Text("✓",color=teamInk(Color(choice.argb)),fontWeight=FontWeight.Bold)
                    }
                }
            }
        }
        Text("Il colore segue la squadra anche al cambio campo. Puoi cambiarlo durante la gara.",
            style=androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}
