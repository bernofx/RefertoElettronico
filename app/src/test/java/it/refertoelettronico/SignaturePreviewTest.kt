package it.refertoelettronico

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import it.refertoelettronico.model.*
import it.refertoelettronico.storage.MatchStore
import it.refertoelettronico.ui.GuideExample
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog
import java.io.File

/** Reproducible previews from the actual Activity and dialog windows, using fictitious match data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignaturePreviewTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun capture(name:String,dialog:Boolean=false) {
        compose.waitForIdle()
        compose.runOnIdle {
            val activityView=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(activityView.width,activityView.height,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap);activityView.draw(canvas)
            if(dialog) {
                val window=ShadowDialog.getLatestDialog().window!!
                canvas.drawColor(Color.argb((window.attributes.dimAmount*255).toInt(),0,0,0))
                val where=IntArray(2);val parent=IntArray(2)
                window.decorView.getLocationOnScreen(where);activityView.getLocationOnScreen(parent)
                canvas.save();canvas.translate((where[0]-parent[0]).toFloat(),(where[1]-parent[1]).toFloat())
                window.decorView.draw(canvas);canvas.restore()
            }
            val directory=File("build/outputs/verification/firme-preview").apply{mkdirs()}
            File(directory,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun showAccessCollectionPadAndFinalApprovalFromActualAppScreens() {
        compose.onNodeWithTag("guide-close").performClick()
        val store=MatchStore(File(compose.activity.filesDir,"matches"))
        val config=Demo.config().copy(matchNumber="ANTEPRIMA",officials=Demo.config().officials.with(1,"Secondo arbitro dimostrativo"))
        val preliminary=store.create(Journal(config=config,openingToss=OpeningToss(0,0)))
        val ink=listOf(listOf(SignaturePoint(.1f,.6f),SignaturePoint(.3f,.15f),SignaturePoint(.45f,.75f),SignaturePoint(.8f,.4f)))
        var finished=Journal(config=config)
        Signatures.required(config,true).forEach { role->finished=Signatures.capture(finished,role,ink,3f,Demo.start()) }
        finished=finished.copy(events=GuideExample.journal(10).events)
        val finalId=store.create(finished)
        compose.mainClock.autoAdvance=false;compose.activityRule.scenario.recreate();compose.mainClock.autoAdvance=true
        compose.onNodeWithTag("archive-match-$preliminary").performScrollTo().performClick()
        compose.onNodeWithTag("pre-signatures").performScrollTo()
        capture("1-accesso-formazioni")
        compose.onNodeWithTag("pre-signatures").performClick()
        capture("2-firme-preliminari",true)
        compose.onNodeWithTag("sign-CAPTAIN_HOME").performClick()
        compose.onNodeWithTag("signature-pad").performTouchInput {
            down(Offset(width*.15f,height*.65f))
            moveTo(Offset(width*.28f,height*.2f),80);moveTo(Offset(width*.23f,height*.75f),80)
            moveTo(Offset(width*.5f,height*.45f),80);moveTo(Offset(width*.6f,height*.65f),80)
            moveTo(Offset(width*.8f,height*.35f),80);up()
        }
        compose.onNodeWithTag("signature-save").assertIsEnabled()
        capture("3-riquadro-firma",true)
        compose.onNodeWithTag("signature-cancel").performClick()
        compose.onNodeWithTag("signatures-close").performClick()
        compose.onNodeWithText("Gare",substring=false).performClick()
        compose.onNodeWithTag("archive-match-$finalId").performScrollTo().performClick()
        compose.onNodeWithText("Referto",substring=false).performClick()
        compose.onNodeWithTag("final-signatures").performScrollTo()
        capture("4-accesso-referto")
        compose.onNodeWithTag("final-signatures").performClick()
        capture("5-approvazione-finale",true)
    }
}
