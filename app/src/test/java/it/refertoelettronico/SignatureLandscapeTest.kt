package it.refertoelettronico

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.refertoelettronico.model.*
import it.refertoelettronico.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w891dp-h411dp-land-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignatureLandscapeTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun horizontalPadSavesInkAndRestoresPreviousOrientationOnCancelAndSave() {
        var open by mutableStateOf(false)
        var captured:Journal?=null
        compose.runOnUiThread{compose.activity.requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_PORTRAIT}
        compose.setContent{VolleyTheme{
            if(open)SignaturePad(SignatureRole.CAPTAIN_HOME,"Atleta 1-1",onCancel={open=false},onSave={ink,ratio->
                captured=Signatures.capture(Journal(config=Demo.config()),SignatureRole.CAPTAIN_HOME,ink,ratio,Demo.start())
                open=false
            })
        }}
        compose.runOnIdle{open=true}
        compose.onNodeWithTag("signature-cancel").assertIsDisplayed()
        compose.runOnIdle{assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,compose.activity.requestedOrientation)}
        compose.onNodeWithTag("signature-cancel").performClick()
        compose.runOnIdle{assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,compose.activity.requestedOrientation);open=true}
        val bounds=compose.onNodeWithTag("signature-pad").fetchSemanticsNode().boundsInRoot
        assertTrue("Il riquadro deve essere largo",bounds.width>bounds.height*2)
        compose.onNodeWithTag("signature-pad").performTouchInput {
            down(Offset(width*.15f,height*.7f));moveTo(Offset(width*.28f,height*.2f),80)
            moveTo(Offset(width*.24f,height*.75f),80);moveTo(Offset(width*.5f,height*.4f),80)
            moveTo(Offset(width*.62f,height*.65f),80);moveTo(Offset(width*.8f,height*.35f),80);up()
        }
        compose.onNodeWithTag("signature-save").assertIsDisplayed().assertIsEnabled()
        compose.runOnIdle {
            val view=ShadowDialog.getLatestDialog().window!!.decorView
            assertTrue(view.width>view.height)
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output=File("build/outputs/verification/firme-preview/3-riquadro-firma-landscape.png")
            output.parentFile!!.mkdirs();output.outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
        compose.onNodeWithTag("signature-save").performClick()
        compose.runOnIdle {
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,compose.activity.requestedOrientation)
            assertNotNull(captured)
            assertTrue(captured!!.signatures!!.single().aspectRatio>2f)
        }
        compose.onNodeWithTag("signature-pad").assertDoesNotExist()
    }
}
