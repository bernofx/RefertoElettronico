package it.refertoelettronico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp

enum class UiSymbol { COURT, HISTORY, DOCUMENT, SETTINGS, ADD, CHEVRON, PEOPLE, CAMERA, UPLOAD, SAVE, EDIT, CHECK, COLOR, SWAP, CLOCK }

/** Decorative vector icons; the adjacent label supplies the accessible name. */
@Composable
fun UiGlyph(symbol:UiSymbol,modifier:Modifier=Modifier,color:Color=LocalContentColor.current) {
    Canvas(modifier.size(20.dp)) {
        scale(size.width/24f,size.height/24f,pivot=Offset.Zero) {
            fun line(x:Float,y:Float,x2:Float,y2:Float)=drawLine(color,Offset(x,y),Offset(x2,y2),1.8f,StrokeCap.Round)
            fun box(x:Float,y:Float,w:Float,h:Float)=drawRoundRect(color,Offset(x,y),Size(w,h),androidx.compose.ui.geometry.CornerRadius(2f),style=Stroke(1.7f))
            when(symbol) {
                UiSymbol.COURT->{box(2f,4f,20f,16f);line(12f,4f,12f,20f);line(7f,4f,7f,20f);line(17f,4f,17f,20f)}
                UiSymbol.HISTORY->{box(4f,3f,16f,18f);for(y in listOf(8f,12f,16f)){line(8f,y,16f,y)}}
                UiSymbol.DOCUMENT->{box(5f,2f,14f,20f);line(9f,8f,15f,8f);line(9f,12f,15f,12f);line(9f,16f,13f,16f)}
                UiSymbol.SETTINGS->{for(y in listOf(6f,12f,18f))line(3f,y,21f,y);drawCircle(color,2.5f,Offset(8f,6f));drawCircle(color,2.5f,Offset(16f,12f));drawCircle(color,2.5f,Offset(10f,18f))}
                UiSymbol.ADD->{line(5f,12f,19f,12f);line(12f,5f,12f,19f)}
                UiSymbol.CHEVRON->{line(9f,6f,15f,12f);line(15f,12f,9f,18f)}
                UiSymbol.PEOPLE->{drawCircle(color,3f,Offset(9f,7f),style=Stroke(1.7f));drawArc(color,180f,180f,false,Offset(2f,13f),Size(14f,12f),style=Stroke(1.7f));drawArc(color,260f,190f,false,Offset(14f,4f),Size(6f,6f),style=Stroke(1.7f));drawArc(color,270f,90f,false,Offset(13f,13f),Size(10f,12f),style=Stroke(1.7f))}
                UiSymbol.CAMERA->{box(2f,6f,20f,15f);line(7f,6f,9f,3f);line(9f,3f,15f,3f);line(15f,3f,17f,6f);drawCircle(color,4f,Offset(12f,13f),style=Stroke(1.7f))}
                UiSymbol.UPLOAD->{line(12f,4f,12f,16f);line(7f,9f,12f,4f);line(12f,4f,17f,9f);line(4f,16f,4f,21f);line(4f,21f,20f,21f);line(20f,21f,20f,16f)}
                UiSymbol.SAVE->{box(4f,3f,16f,18f);box(8f,3f,8f,6f);box(8f,14f,8f,7f)}
                UiSymbol.EDIT->{line(5f,17f,17f,5f);line(8f,20f,20f,8f);line(17f,5f,20f,8f);line(5f,17f,4f,21f);line(4f,21f,8f,20f)}
                UiSymbol.CHECK->{line(5f,12f,10f,17f);line(10f,17f,20f,6f)}
                UiSymbol.COLOR->{drawCircle(color,8f,Offset(12f,12f),style=Stroke(1.7f));drawCircle(color,2f,Offset(9f,9f));drawCircle(color,2f,Offset(16f,11f));drawCircle(color,2f,Offset(10f,16f))}
                UiSymbol.SWAP->{line(3f,8f,20f,8f);line(16f,4f,20f,8f);line(20f,8f,16f,12f);line(21f,17f,4f,17f);line(8f,13f,4f,17f);line(4f,17f,8f,21f)}
                UiSymbol.CLOCK->{drawCircle(color,9f,Offset(12f,12f),style=Stroke(1.7f));line(12f,6f,12f,12f);line(12f,12f,16f,14f)}
            }
        }
    }
}

@Composable
fun ActionLabel(label:String,symbol:UiSymbol) {UiGlyph(symbol);Spacer(Modifier.width(7.dp));Text(label)}
