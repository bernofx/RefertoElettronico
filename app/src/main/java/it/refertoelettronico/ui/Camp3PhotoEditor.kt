package it.refertoelettronico.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import it.refertoelettronico.ocr.PhotoCrop
import kotlin.math.*

@Composable
fun Camp3PhotoEditor(bitmap:Bitmap,crop:PhotoCrop,onCrop:(PhotoCrop)->Unit,column:Float?,onColumn:(Float?)->Unit,
                     playerList:Boolean,onRead:()->Unit,chooseColumnInitially:Boolean=false) {
    var columnMode by remember(chooseColumnInitially){mutableStateOf(chooseColumnInitially)}
    val currentCrop by rememberUpdatedState(crop)
    val image=remember(bitmap){bitmap.asImageBitmap()}
    val shown=if(columnMode)crop else PhotoCrop()
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact=maxHeight<400.dp
    Column(Modifier.fillMaxSize().then(if(compact)Modifier.verticalScroll(rememberScrollState()) else Modifier),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(if(columnMode)"Tocca il centro della colonna dei numeri di maglia. Non scegliere il progressivo o la tessera." else "Trascina sulla foto per ritagliare solo la tabella. Puoi spostare gli angoli. Includi le intestazioni se ci sono.",style=MaterialTheme.typography.bodySmall)
        if(playerList)Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            FilterChip(selected=!columnMode,onClick={columnMode=false},label={Text("Ritaglia")})
            FilterChip(selected=columnMode,onClick={columnMode=true},label={Text("Colonna maglie")})
        }
        Canvas(Modifier.fillMaxWidth().then(if(compact)Modifier.height(240.dp) else Modifier.weight(1f)).testTag("camp3-crop-editor")
            .pointerInput(bitmap,columnMode,shown) {
                fun point(p:Offset):Offset {
                    val sourceWidth=bitmap.width*(shown.right-shown.left);val sourceHeight=bitmap.height*(shown.bottom-shown.top)
                    val scale=min(size.width/sourceWidth,size.height/sourceHeight)
                    val w=sourceWidth*scale;val h=sourceHeight*scale
                    return Offset(shown.left+((p.x-(size.width-w)/2)/w).coerceIn(0f,1f)*(shown.right-shown.left),shown.top+((p.y-(size.height-h)/2)/h).coerceIn(0f,1f)*(shown.bottom-shown.top))
                }
                if(columnMode)detectTapGestures { tap->onColumn(point(tap).x) }
                else {
                    awaitEachGesture {
                        val down=awaitFirstDown(requireUnconsumed=false)
                        val start=point(down.position);val c=currentCrop
                        val corners=listOf(Offset(c.left,c.top),Offset(c.right,c.top),Offset(c.right,c.bottom),Offset(c.left,c.bottom))
                        val nearest=corners.indices.minBy{(corners[it]-start).getDistance()}
                        val anchor=if((corners[nearest]-start).getDistance()<.12f)corners[(nearest+2)%4] else start
                        var dragging=false
                        do {
                            val event=awaitPointerEvent()
                            val change=event.changes.firstOrNull{it.id==down.id} ?: break
                            if((change.position-down.position).getDistance()>viewConfiguration.touchSlop)dragging=true
                            if(dragging) {
                                change.consume();val end=point(change.position)
                                onCrop(PhotoCrop.between(anchor.x,anchor.y,end.x,end.y))
                            }
                        }while(change.pressed)
                    }
                }
            }) {
            val source=shown.pixels(bitmap.width,bitmap.height)
            val scale=min(size.width/source.width(),size.height/source.height())
            val w=source.width()*scale;val h=source.height()*scale
            val origin=Offset((size.width-w)/2,(size.height-h)/2)
            drawImage(image,srcOffset=IntOffset(source.left,source.top),srcSize=IntSize(source.width(),source.height()),dstOffset=IntOffset(origin.x.roundToInt(),origin.y.roundToInt()),dstSize=IntSize(w.roundToInt(),h.roundToInt()))
            val l=origin.x+(crop.left-shown.left)/(shown.right-shown.left)*w;val r=origin.x+(crop.right-shown.left)/(shown.right-shown.left)*w
            val t=origin.y+(crop.top-shown.top)/(shown.bottom-shown.top)*h;val b=origin.y+(crop.bottom-shown.top)/(shown.bottom-shown.top)*h
            val shade=Color.Black.copy(alpha=.55f)
            drawRect(shade,origin,Size(w,t-origin.y));drawRect(shade,Offset(origin.x,b),Size(w,origin.y+h-b))
            drawRect(shade,Offset(origin.x,t),Size(l-origin.x,b-t));drawRect(shade,Offset(r,t),Size(origin.x+w-r,b-t))
            drawRect(Color(0xFF0C8F9E),Offset(l,t),Size(r-l,b-t),style=Stroke(2.dp.toPx()))
            for(p in listOf(Offset(l,t),Offset(r,t),Offset(r,b),Offset(l,b)))drawCircle(Color.White,5.dp.toPx(),p)
            column?.takeIf{it in crop.left..crop.right}?.let{x->drawLine(Color(0xFFE65027),Offset(origin.x+(x-shown.left)/(shown.right-shown.left)*w,t),Offset(origin.x+(x-shown.left)/(shown.right-shown.left)*w,b),3.dp.toPx())}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            TextButton(onClick={onCrop(PhotoCrop());onColumn(null)}){Text("Foto intera")}
            if(column!=null)TextButton(onClick={onColumn(null)}){Text("Colonna automatica")}
        }
        Button(onClick=onRead,modifier=Modifier.fillMaxWidth()){Text(if(playerList)"Leggi lista giocatrici" else "Leggi staff")}
    }
}

}
