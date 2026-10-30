package it.refertoelettronico.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Bundled Latin model. Photo and recognized text never leave the device. Run on Dispatchers.IO. */
object Camp3Ocr {
    fun read(context:Context,uri:Uri,quarterTurns:Int=0):Camp3Reading =
        OriginalPhoto.open(context,uri).use {readOriginal(it,quarterTurns=quarterTurns)}

    internal fun readOriginal(original:OriginalPhoto,crop:PhotoCrop=PhotoCrop(),numberColumn:Float?=null,
                     section:Camp3Section=Camp3Section.PLAYERS,quarterTurns:Int=0,
                     recognize:(Bitmap,Float?,Camp3Section,Boolean)->Camp3Reading=::recognize):Camp3Reading =
        original.decode(crop,quarterTurns).use { decoded->
            recognize(decoded.bitmap,decoded.columnX(numberColumn),section,decoded.limited && crop!=PhotoCrop())
        }

    private fun recognize(bitmap:Bitmap,column:Float?,section:Camp3Section,limited:Boolean):Camp3Reading {
        val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val result=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)))
            val lines=result.textBlocks.flatMap{it.lines}
            val slopes=lines.mapNotNull { line->
                val corners=line.cornerPoints
                if(corners==null || corners.size<2)null else {
                    val dx=corners[1].x-corners[0].x;val dy=corners[1].y-corners[0].y
                    if(dx<40)null else (dy.toFloat()/dx).takeIf{kotlin.math.abs(it)<.3f}
                }
            }.sorted()
            val slope=slopes.getOrNull(slopes.size/2) ?: 0f
            val words=lines.flatMap{it.elements}.mapNotNull { element->
                element.boundingBox?.let{r->OcrWord(element.text,r.left.toFloat(),r.top.toFloat(),r.right.toFloat(),r.bottom.toFloat())}
            }
            val rows=if(section==Camp3Section.PLAYERS)Camp3Rows.suggest(words,column,slope) else emptyList()
            val staff=if(section==Camp3Section.STAFF)Camp3Staff.suggest(words,slope) else emptyList()
            val notices=buildList {
                if(limited)add("Su questo dispositivo il ritaglio originale non è disponibile per questa foto: la lettura usa una copia ridotta. Per più dettaglio usa una foto JPEG ravvicinata della tabella.")
                if(words.isEmpty())add("Testo non leggibile: scegli una foto più nitida o avvicina la tabella.")
                else if(words.map{it.height}.sorted().let{it[it.size/2]}<16f)add("Il testo nella foto è piccolo: avvicina la fotocamera alla tabella per una lettura più precisa.")
                if(section==Camp3Section.STAFF && staff.isEmpty())add("Ruoli non riconosciuti. Includi le etichette dello staff oppure completa i nomi manualmente.")
                if(section==Camp3Section.PLAYERS && rows.size<6)add("Sono state riconosciute poche atlete. Verifica il ritaglio e prova a indicare la colonna delle maglie.")
                if(rows.any{it.number==null})add("Alcuni numeri non sono riconoscibili con certezza: controlla le righe evidenziate.")
            }
            return Camp3Reading(rows,result.text,notices,staff,Camp3Table.detect(words,bitmap.width,bitmap.height,section,slope))
        } finally {recognizer.close()}
    }

    internal fun decodePhoto(context:Context,uri:Uri,quarterTurns:Int):Bitmap =
        OriginalPhoto.open(context,uri).use{it.decode(quarterTurns=quarterTurns).bitmap}
}
