package it.refertoelettronico

import it.refertoelettronico.ocr.*
import org.junit.Assert.*
import org.junit.Test

class Camp3StaffTest {
    private fun word(text:String,x:Float,y:Float)=OcrWord(text,x,y,x+text.length*8,y+16)
    @Test fun staffRolesComeFromTheirLabelsNotMembershipNumbers() {
        val words=listOf(word("ALLENATORE",0f,0f),word("ROSSI",180f,0f),word("LUCA",240f,0f),word("234567",360f,0f),
            word("2°",0f,30f),word("ALLENATORE",30f,30f),word("BIANCHI",180f,30f),word("MARCO",260f,30f),
            word("DIRIGENTE",0f,60f),word("VERDI",180f,60f),word("ANNA",240f,60f))
        val rows=Camp3Staff.suggest(words)
        assertEquals(listOf(0,1,4),rows.map{it.index})
        assertEquals(listOf("ROSSI LUCA","BIANCHI MARCO","VERDI ANNA"),rows.map{it.name})
    }
    @Test fun namesWithoutRoleLabelsAreLeftForManualReview() {
        assertTrue(Camp3Staff.suggest(listOf(word("ROSSI",100f,0f),word("LUCA",200f,0f))).isEmpty())
    }
    @Test fun cropCoordinatesStayInsideThePhotoWhenDraggingInReverse() {
        val crop=PhotoCrop.between(.9f,.8f,.1f,.2f)
        assertEquals(PhotoCrop(.1f,.2f,.9f,.8f),crop)
        val tiny=PhotoCrop.between(1f,1f,1f,1f)
        assertTrue(tiny.right<=1f && tiny.bottom<=1f && tiny.right>tiny.left)
    }
}
