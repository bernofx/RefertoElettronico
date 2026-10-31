package it.refertoelettronico

import android.graphics.Bitmap
import android.graphics.Color
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.rendering.PDFRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import it.refertoelettronico.model.*
import it.refertoelettronico.pdf.PdfExporter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidWorkflowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private var launchIntent:android.content.Intent?=null
    @org.junit.Before fun retainScenarioLaunchIntent(){
        launchIntent=compose.activity.intent
        org.robolectric.util.ReflectionHelpers.getStaticField<MutableMap<String,Any>>(androidx.core.content.FileProvider::class.java,"sCache").clear()
    }
    @org.junit.After fun restoreScenarioIntentForLifecycleTracking(){compose.runOnUiThread{compose.activity.intent=launchIntent}}
    @Test fun browserLinkUsesTheSameReviewWithoutSavingOrReplacingExistingMatches() {
        val json="""{"schema":"it.refertoelettronico.match-setup","version":1,"config":{"matchNumber":"LINK-001","teams":[{"name":"Casa è"},{"name":"Ospiti"}]}}"""
        val payload=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray(Charsets.UTF_8))
        val activity=compose.activity
        val store=it.refertoelettronico.storage.MatchStore(File(activity.filesDir,"matches"))
        val before=store.list().map{it.first to it.second.getOrThrow()}
        val link=android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("refertovolley://import-match?data=$payload"))
            .addCategory(android.content.Intent.CATEGORY_BROWSABLE).setPackage(activity.packageName)
        assertNotNull(activity.packageManager.resolveActivity(link,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY))
        compose.runOnUiThread{activity.onNewIntent(link)}
        compose.waitUntil(10000){compose.onAllNodesWithText("Dati gara ricevuti").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Ignora").performClick()
        assertEquals(before,store.list().map{it.first to it.second.getOrThrow()})
    }
    @Test fun incomingVolleyAlertsSetupIsReviewedAndDoesNotOverwriteTheCurrentMatch() {
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithTag("point-button-0").performClick()
        val activity=compose.activity
        val store=it.refertoelettronico.storage.MatchStore(File(activity.filesDir,"matches"))
        val before=store.list().map{it.first to it.second.getOrThrow()}
        val json="""{"schema":"it.refertoelettronico.match-setup","version":1,"config":{"matchNumber":"VA-123","teams":[{"name":"VolleyAlerts Casa"},{"name":"VolleyAlerts Ospite"}]}}"""
        val file=File(activity.filesDir,"exports/setup-volleyalerts.json").apply{parentFile!!.mkdirs();writeText(json)}
        val uri=androidx.core.content.FileProvider.getUriForFile(activity,"${activity.packageName}.files",file)
        val intent=android.content.Intent(it.refertoelettronico.integration.MatchSetupImport.ACTION)
            .setDataAndType(uri,it.refertoelettronico.integration.MatchSetupImport.MIME)
            .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        compose.runOnUiThread{activity.onNewIntent(intent)}
        compose.waitUntil(10000){compose.onAllNodesWithText("Dati gara ricevuti").fetchSemanticsNodes().isNotEmpty()}
        assertEquals(before,store.list().map{it.first to it.second.getOrThrow()})
        compose.onNodeWithText("Rivedi dati").performClick()
        compose.onNodeWithText("VA-123").assertExists()
        compose.onNodeWithText("Squadra di casa").performClick()
        compose.onNodeWithText("VolleyAlerts Casa").assertExists()
        compose.onNodeWithTag("attendance-count").assertTextContains("0/14",substring=true)
        compose.onNodeWithText("Salva modifiche").performClick()
        compose.onNodeWithText("Controlla l'operazione").assertExists()
        assertEquals(before,store.list().map{it.first to it.second.getOrThrow()})
    }
    @Test fun sharedJsonIsAcceptedButWebAndPrivateFilePathsAreRejected() {
        val activity=compose.activity
        val json="""{"schema":"it.refertoelettronico.match-setup","version":1,"config":{"teams":[{"name":"Casa"},{"name":"Ospite"}]}}"""
        val file=File(activity.filesDir,"exports/shared-setup.json").apply{parentFile!!.mkdirs();writeText(json)}
        val uri=androidx.core.content.FileProvider.getUriForFile(activity,"${activity.packageName}.files",file)
        val share=android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json")
            .putExtra(android.content.Intent.EXTRA_STREAM,uri)
        assertEquals("Casa",it.refertoelettronico.integration.IncomingMatch.read(activity,share).teams[0].name)
        listOf("https://example.com/setup.json","file:///data/private.json").forEach { address ->
            val intent=android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(address))
            assertThrows(IllegalArgumentException::class.java){it.refertoelettronico.integration.IncomingMatch.read(activity,intent)}
        }
        file.writeText("not JSON")
        compose.runOnUiThread{activity.onNewIntent(share)}
        compose.waitUntil(10000){compose.onAllNodesWithText("Controlla l'operazione").fetchSemanticsNodes().isNotEmpty()}
        assertFalse(File(activity.filesDir,"matches").listFiles().orEmpty().any{it.extension=="json"})
    }
    @Test fun scoreUndoAndResumePersistedMatchThroughTheActualScreens() {
        captureUi("ui-home-0.6.3.png")
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        captureUi("ui-gara-0.6.3.png")
        compose.onNodeWithTag("score-0").assertTextEquals("0")
        compose.onAllNodesWithText("+1 punto")[0].performClick()
        compose.onNodeWithTag("score-0").assertTextEquals("1")
        compose.onNodeWithText("↶ Annulla").performClick()
        compose.onNodeWithTag("score-0").assertTextEquals("0")
        compose.onNodeWithText("Gare",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Lago Volley — Stella Como").performScrollTo().performClick()
        compose.onNodeWithTag("score-0").assertTextEquals("0")
        compose.onNodeWithText("Ripristina",substring=false).performClick()
        compose.onNodeWithTag("score-0").assertTextEquals("1")
        val outputs=File("build/outputs/verification").apply{mkdirs()}
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(outputs,"schermata-gara.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
    @Test fun realAndroidPdfCanBeWrittenAndRenderedWithTheOfficialBackground() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        var journal=Journal(config=Demo.config(),events=listOf(Demo.start(1800000000000)))
        repeat(5){journal=MatchEngine.add(journal,Event(Kind.POINT,0,at=1800000001000+it.toLong()))}
        journal=MatchEngine.add(journal,Event(Kind.TIMEOUT,1,at=1800000009000))
        journal=MatchEngine.add(journal,Event(Kind.SUBSTITUTION,0,1,7,at=1800000010000))
        journal=MatchEngine.add(journal,Event(Kind.NOTE,text="Prova di compilazione, nessuna gara reale",at=1800000011000))
        val file=PdfExporter.export(context,journal)
        assertTrue(file.length()>10000)
        val lightweight=PdfExporter.preview(context,journal)
        assertEquals(1190,lightweight.width);assertEquals(842,lightweight.height)
        lightweight.recycle()
        PDDocument.load(file).use{doc ->
            assertTrue(doc.numberOfPages>=2)
            val text=PDFTextStripper().getText(doc)
            assertTrue(text.contains("Lago Volley"));assertTrue(text.contains("Prova di compilazione"))
            assertTrue(text.contains("PROVVISORIO"))
        }
        val preview=PDDocument.load(file).use{PDFRenderer(it).renderImageWithDPI(0,72f)}
        var colored=0
        for(y in 100 until 750 step 10)for(x in 20 until 1180 step 10)if(preview.getPixel(x,y)!=Color.WHITE)colored++
        assertTrue("Il PDF non deve essere una pagina vuota",colored>100)
        val outputs=File("build/outputs/verification").apply{mkdirs()}
        file.copyTo(File(outputs,"referto-demo-android.pdf"),overwrite=true)
        File(outputs,"referto-demo-android.png").outputStream().use{preview.compress(Bitmap.CompressFormat.PNG,100,it)}
    }
    @Test fun completeFiveSetMatchExportsAllSetsAndTieBreakContinuation() {
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        var time=java.time.Instant.parse("2026-10-30T17:00:00Z").toEpochMilli()
        var journal=Journal(config=Demo.config())
        fun add(event:Event){time+=25_000;journal=MatchEngine.add(journal,event.copy(at=time))}
        fun point(team:Int) {
            val before=MatchEngine.replay(journal.config,journal.events)
            if(MatchEngine.rallyProblem(before)!=null) {
                for(t in 0..1)if(before.current!!.liberos[t]!=null)add(Event(Kind.LIBERO_OUT,t))
            }
            add(Event(Kind.POINT,team))
        }
        val results=listOf(25 to 18,22 to 25,25 to 23,19 to 25,16 to 14)
        results.forEachIndexed { index,(a,b) ->
            time+=180_000
            add(Demo.start().copy(server=index%2,left=index%2))
            if(index==0) {
                add(Event(Kind.LIBERO_IN,0,12,6))
                add(Event(Kind.SUBSTITUTION,1,1,7))
                add(Event(Kind.TIMEOUT,1))
                add(Event(Kind.SANCTION,1,text="A",card=Card.WARNING))
            }
            repeat(minOf(a,b)){point(0);point(1)}
            repeat(a-minOf(a,b)){point(0)}
            repeat(b-minOf(a,b)){point(1)}
        }
        val state=MatchEngine.replay(journal.config,journal.events)
        assertTrue(state.finished(journal.config));assertEquals(3,state.wins(0));assertEquals(2,state.wins(1))
        assertNotNull(state.current!!.changedEndsAt)
        val file=PdfExporter.export(context,journal)
        PDDocument.load(file).use{doc ->
            val text=PDFTextStripper().getText(doc)
            assertTrue(text.contains("GARA CONCLUSA"));assertFalse(text.contains("PROVVISORIO"))
            assertTrue(text.contains("Atleta 1-8"));assertTrue(text.contains("Libero 2-2"))
            assertTrue(doc.numberOfPages>5)
        }
        val outputs=File("build/outputs/verification").apply{mkdirs()}
        file.copyTo(File(outputs,"referto-gara-completa.pdf"),overwrite=true)
        val preview=PDDocument.load(file).use{PDFRenderer(it).renderImageWithDPI(0,72f)}
        File(outputs,"referto-gara-completa.png").outputStream().use{preview.compress(Bitmap.CompressFormat.PNG,100,it)}
    }

    @Test
    @Config(sdk=[35],qualifiers="w891dp-h411dp-land-xhdpi")
    fun landscapeShowsBothScoresAndControlsTogether() {
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithTag("score-0").assertIsDisplayed()
        compose.onNodeWithTag("score-1").assertIsDisplayed()
        assertEquals(2,compose.onAllNodesWithText("+1 punto").fetchSemanticsNodes().size)
    }

    @Test fun rotationMovesTheAthleteSmoothlyAndUndoAnimatesBack() {
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithTag("scorer-court").assertIsDisplayed()
        val player=compose.onNodeWithTag("court-player-1-2")
        val start=player.fetchSemanticsNode().boundsInRoot
        val outputs=File("build/outputs/verification/rotation-frames").apply{mkdirs()}
        fun capture(frame:Int) {
            compose.runOnIdle {
                val view=compose.activity.window.decorView
                val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                File(outputs,"frame-%03d.png".format(frame)).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                bitmap.recycle()
            }
        }
        capture(0)
        compose.mainClock.autoAdvance=false
        compose.onAllNodesWithText("+1 punto")[1].performClick()
        compose.mainClock.advanceTimeByFrame()
        var middle=start
        for(frame in 1..16) {
            compose.mainClock.advanceTimeBy(50)
            if(frame==4)middle=player.fetchSemanticsNode().boundsInRoot
            capture(frame)
        }
        val end=player.fetchSemanticsNode().boundsInRoot
        assertTrue("La rotazione deve avere fotogrammi intermedi",middle.left>start.left+1 && middle.left<end.left-1)
        assertEquals(start.top,end.top,1f)
        compose.onNode(hasTestTag("service-ball") and hasAnyAncestor(hasTestTag("court-player-1-2")),useUnmergedTree=true).assertExists()
        assertEquals(1,compose.onAllNodesWithTag("service-ball",useUnmergedTree=true).fetchSemanticsNodes().size)
        compose.onNodeWithText("↶ Annulla").performClick()
        compose.mainClock.advanceTimeByFrame()
        for(frame in 17..32) {
            compose.mainClock.advanceTimeBy(50)
            if(frame==20) {
                val returning=player.fetchSemanticsNode().boundsInRoot
                assertTrue("Anche Annulla deve animare il ritorno",returning.left>start.left+1 && returning.left<end.left-1)
            }
            capture(frame)
        }
        val undone=player.fetchSemanticsNode().boundsInRoot
        assertEquals(start.left,undone.left,1f);assertEquals(start.top,undone.top,1f)
        compose.onNodeWithTag("score-1").assertTextEquals("0")
        compose.onNode(hasTestTag("service-ball") and hasAnyAncestor(hasTestTag("court-player-0-1")),useUnmergedTree=true).assertExists()
        compose.mainClock.autoAdvance=true
    }

    @Test fun colorsCanBeChangedDuringAMatchAndAreSavedWithIt() {
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithText("Dati").performClick()
        compose.onNodeWithText("Modifica i dati della gara").performClick()
        compose.onNodeWithText("Squadra di casa").performClick()
        captureUi("ui-rosa-0.6.3.png")
        compose.onNodeWithContentDescription("Colore").performClick()
        compose.onNodeWithTag("team-color-GREEN").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Salva modifiche").performClick()
        compose.onNodeWithTag("score-0").assertTextEquals("0")
        val stored=it.refertoelettronico.storage.MatchStore(File(compose.activity.filesDir,"matches")).list().single().second.getOrThrow()
        assertEquals("GREEN",stored.config.teams[0].colorKey)
        assertNull(stored.config.teams[1].colorKey)
        val outputs=File("build/outputs/verification").apply{mkdirs()}
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(outputs,"colori-squadre.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
    @Test fun confirmationSoundsFollowAcceptedActionsAndCanBeMutedFromTheCourt() {
        val manager=compose.activity.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        manager.ringerMode=android.media.AudioManager.RINGER_MODE_NORMAL
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        org.robolectric.shadows.ShadowToneGenerator.reset()
        compose.onAllNodesWithText("+1 punto")[0].performClick()
        assertEquals(it.refertoelettronico.feedback.Cue.POINT.tone,org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().single().type())
        compose.onNodeWithText("↶ Annulla").performClick()
        assertEquals(it.refertoelettronico.feedback.Cue.UNDO.tone,org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().last().type())
        compose.onNodeWithTag("sound-toggle").assertIsOn().performClick().assertIsOff()
        val count=org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().size
        compose.onAllNodesWithText("+1 punto")[1].performClick()
        compose.onNodeWithTag("score-1").assertTextEquals("1")
        assertEquals(count,org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().size)
    }
    @Test fun rejectedTimeoutDoesNotPlayASuccessSound() {
        val manager=compose.activity.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        manager.ringerMode=android.media.AudioManager.RINGER_MODE_NORMAL
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onAllNodesWithText("Timeout 0/2")[0].performScrollTo().performClick()
        compose.onNodeWithText("Registra").performClick()
        compose.onNodeWithText("Timeout 1/2").performScrollTo().performClick()
        compose.onNodeWithText("Registra").performClick()
        val count=org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().size
        compose.onNodeWithText("Timeout 2/2").performScrollTo().performClick()
        compose.onNodeWithText("Registra").performClick()
        compose.onNodeWithText("Entrambi i timeout sono già stati utilizzati.").assertExists()
        assertEquals(count,org.robolectric.shadows.ShadowToneGenerator.getPlayedTones().size)
    }


    @Test fun matchDeletionRequiresConfirmationAndPreservesOtherMatchesAndSavedRosters() {
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithText("Gare",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        compose.onNodeWithText("Gare",useUnmergedTree=true).performClick()
        val store=it.refertoelettronico.storage.MatchStore(File(compose.activity.filesDir,"matches"))
        val originals=store.list().associate{it.first to it.second.getOrThrow()}
        assertEquals(2,originals.size)
        val library=it.refertoelettronico.storage.RosterStore(File(compose.activity.filesDir,"rosters"))
        library.save("Rosa conservata",originals.values.first().config.teams[0])
        val target=originals.keys.first()
        compose.onNodeWithTag("delete-match-$target").performScrollTo().performClick()
        compose.onNodeWithText("Annulla",substring=false).performClick()
        assertEquals(originals.keys,store.list().map{it.first}.toSet())
        compose.onNodeWithTag("delete-match-$target").performScrollTo().performClick()
        compose.onNodeWithTag("confirm-delete-match").performClick()
        compose.onNodeWithTag("delete-match-$target").assertDoesNotExist()
        val survivor=originals.keys.single{it!=target}
        assertEquals(originals[survivor],store.load(survivor))
        assertEquals(1,store.list().size);assertEquals(1,library.list().size)
        assertThrows(IllegalArgumentException::class.java){store.delete("../other")}
    }

    @Test fun compactLiveHeaderLeavesMostOfTheScreenForScrollableCourtAndActions() {
        compose.onNodeWithText("Prova con una gara dimostrativa").performScrollTo().performClick()
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        val scroll=compose.onNodeWithTag("live-scroll-region").fetchSemanticsNode().boundsInRoot
        assertTrue("Almeno il 60% dello schermo deve rimanere alla gara scorrevole",scroll.height>root.height*.60f)
        compose.onNodeWithTag("point-button-0").assertIsDisplayed()
        compose.onNodeWithTag("point-button-1").assertIsDisplayed()
        captureUi("ui-gara-0.6.4.png")
    }

    private fun captureUi(name:String) {
        val outputs=File("build/outputs/verification").apply{mkdirs()}
        compose.runOnIdle {
            val view=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(outputs,name).outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }

}
