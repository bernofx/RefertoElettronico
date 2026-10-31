package it.refertoelettronico

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import it.refertoelettronico.ocr.*
import it.refertoelettronico.ui.Camp3PhotoEditor
import it.refertoelettronico.ui.Camp3StaffReview
import it.refertoelettronico.ui.Camp3RosterReview
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Camp3EditorTest {
    @get:Rule val compose=createComposeRule()
    @Test fun cropAndJerseyColumnTouchesMapToTheOriginalPhotoEvenAfterZoom() {
        val photo=Bitmap.createBitmap(1000,500,Bitmap.Config.ARGB_8888)
        var crop by mutableStateOf(PhotoCrop())
        var column by mutableStateOf<Float?>(null)
        var reads=0
        compose.setContent{MaterialTheme{Camp3PhotoEditor(photo,crop,{crop=it},column,{column=it},true,{reads++})}}
        compose.onNodeWithText("Ritaglia",substring=false).performClick()
        compose.onNodeWithTag("camp3-crop-editor").performTouchInput {
            val h=width*.5f;val top=(height-h)/2
            swipe(Offset(width*.2f,top+h*.2f),Offset(width*.8f,top+h*.8f))
        }
        compose.runOnIdle{assertEquals(.2f,crop.left,.03f);assertEquals(.8f,crop.right,.03f)}
        compose.onNodeWithText("Maglie",substring=false).performClick()
        compose.onNodeWithTag("camp3-crop-editor").performTouchInput{click(Offset(width*.75f,height*.5f))}
        compose.runOnIdle{assertEquals(.65f,column!!,.04f)}
        compose.onNodeWithText("Leggi lista giocatrici").performClick()
        compose.runOnIdle{assertEquals(1,reads)}
    }
    @Test fun optionalStaffImportOnlyReturnsSelectedRolesAndLeavesOtherDataToTheCaller() {
        var applied:Map<Int,String>?=null
        val reading=Camp3Reading(emptyList(),"Testo staff",staff=listOf(StaffSuggestion(0,"Rossi Luca","ALLENATORE Rossi Luca"),StaffSuggestion(4,"Verdi Anna","DIRIGENTE Verdi Anna")))
        compose.setContent{MaterialTheme{Camp3StaffReview(reading){applied=it}}}
        compose.onNodeWithText("Importa staff selezionato").assertIsEnabled().performClick()
        compose.runOnIdle{assertEquals(mapOf(0 to "Rossi Luca",4 to "Verdi Anna"),applied)}
    }
    @Test fun proposedCamp3RolesAreShownForReviewAndCanBeImportedWithoutRetyping() {
        var imported:List<it.refertoelettronico.model.Player>?=null
        val words=javaClass.getResourceAsStream("/ocr/camp3-como-anonymized.json")!!.bufferedReader().use{
            com.google.gson.Gson().fromJson(it,Array<OcrWord>::class.java).toList()
        }
        val reading=Camp3Reading(Camp3Rows.suggest(words),"Fixture con dati fittizi")
        compose.setContent{MaterialTheme{Camp3RosterReview(reading){imported=it}}}
        compose.onNodeWithText("Sostituisci rosa con 14 giocatrici").assertIsEnabled().performClick()
        compose.runOnIdle{
            assertEquals(14,imported!!.size)
            assertEquals(1,imported!!.count{it.captain})
            assertEquals(2,imported!!.count{it.libero})
        }
    }

    @Test fun swipingALockedPhotoDoesNotChangeCropOrColumnAndReadStaysVisible() {
        val photo=Bitmap.createBitmap(1000,500,Bitmap.Config.ARGB_8888)
        val original=PhotoCrop(.1f,.1f,.9f,.9f)
        var crop by mutableStateOf(original)
        var column by mutableStateOf<Float?>(.5f)
        compose.setContent{MaterialTheme{Camp3PhotoEditor(photo,crop,{crop=it},column,{column=it},true,{})}}
        compose.onNodeWithTag("camp3-crop-editor").performTouchInput{swipeUp()}
        compose.runOnIdle{assertEquals(original,crop);assertEquals(.5f,column!!,.001f)}
        compose.onNodeWithText("Leggi lista giocatrici").assertIsDisplayed()
    }
    @Test
    @Config(sdk=[35],qualifiers="w891dp-h411dp-land-xhdpi")
    fun landscapeReadButtonNeedsNoScrollingOverTheImage() {
        val photo=Bitmap.createBitmap(1000,700,Bitmap.Config.ARGB_8888)
        var crop by mutableStateOf(PhotoCrop())
        compose.setContent{MaterialTheme{Camp3PhotoEditor(photo,crop,{crop=it},null,{},true,{})}}
        compose.onNodeWithText("Leggi lista giocatrici").assertIsDisplayed()
        compose.onNodeWithTag("camp3-crop-editor").performTouchInput{swipeUp()}
        compose.runOnIdle{assertEquals(PhotoCrop(),crop)}
    }

}
