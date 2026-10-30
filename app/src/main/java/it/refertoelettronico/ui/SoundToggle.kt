package it.refertoelettronico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun SoundToggle(enabled:Boolean,onChange:(Boolean)->Unit) {
    val ink=MaterialTheme.colorScheme.primary
    IconToggleButton(checked=enabled,onCheckedChange=onChange,modifier=Modifier.testTag("sound-toggle")
        .semantics { contentDescription=if(enabled)"Suoni di conferma attivi" else "Suoni di conferma disattivati" }) {
        Canvas(Modifier.size(24.dp)) {
            val w=size.width;val h=size.height
            val speaker=Path().apply {
                moveTo(w*.05f,h*.35f);lineTo(w*.25f,h*.35f);lineTo(w*.48f,h*.15f)
                lineTo(w*.48f,h*.85f);lineTo(w*.25f,h*.65f);lineTo(w*.05f,h*.65f);close()
            }
            drawPath(speaker,ink)
            if(enabled) {
                drawArc(ink,-55f,110f,false,Offset(w*.3f,h*.15f),Size(w*.6f,h*.7f),style=Stroke(1.5.dp.toPx()))
                drawArc(ink,-55f,110f,false,Offset(w*.32f,h*.3f),Size(w*.35f,h*.4f),style=Stroke(1.5.dp.toPx()))
            } else {
                drawLine(ink,Offset(w*.62f,h*.35f),Offset(w*.92f,h*.65f),2.dp.toPx())
                drawLine(ink,Offset(w*.92f,h*.35f),Offset(w*.62f,h*.65f),2.dp.toPx())
            }
        }
    }
}
