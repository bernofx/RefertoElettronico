package it.refertoelettronico.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.refertoelettronico.ExportScreen
import it.refertoelettronico.model.*
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.io.File
import kotlin.math.roundToInt

/** Real screens in a separate window and memory-only match: no navigation or archive side effects. */
@Composable
fun GuideOverlay(onClose:()->Unit) {
    var step by rememberSaveable{mutableIntStateOf(0)}
    val item=AppGuide.steps[step.coerceIn(AppGuide.steps.indices)]
    val targets=remember{TourTargets()}
    val journal=remember(item.phase){GuideExample.journal(item.phase)}
    val state=remember(journal){MatchEngine.replay(journal.config,journal.events)}
    val context=LocalContext.current
    val directory=remember{File(context.cacheDir,"tutorial-${java.util.UUID.randomUUID()}")}
    DisposableEffect(directory){onDispose{directory.deleteRecursively()}}
    var error by remember{mutableStateOf<String?>(null)}
    var rootOrigin by remember{mutableStateOf(Offset.Zero)}
    var targetReady by remember(step){mutableStateOf(false)}
    var pdfStarted by remember{mutableStateOf(false)}
    LaunchedEffect(step) {
        if(item.target=="pdf-generate")pdfStarted=false
        if(item.target=="pdf-preview" && !pdfStarted) {
            val generator=snapshotFlow{targets.entries["pdf-generate"]?.activate}.filterNotNull().first()
            pdfStarted=true;generator()
        }
        val entry=snapshotFlow{targets.entries[item.target]}.filterNotNull().first()
        snapshotFlow{entry.placed.takeIf{it}}.filterNotNull().first()
        entry.requester.bringIntoView()
        snapshotFlow{entry.bounds.takeIf{it.width>0f&&it.height>0f}}.filterNotNull().first()
        targetReady=true
    }
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize()) {
            BoxWithConstraints(Modifier.fillMaxSize().onGloballyPositioned{rootOrigin=it.boundsInRoot().topLeft}) {
                CompositionLocalProvider(LocalTourTargets provides targets) {
                    Column(Modifier.fillMaxSize().semantics{hideFromAccessibility()}) {
                        Surface(color=MaterialTheme.colorScheme.secondaryContainer) {
                            Text("Esercitazione · dati dimostrativi",Modifier.fillMaxWidth().padding(10.dp),style=MaterialTheme.typography.labelMedium)
                        }
                        Box(Modifier.weight(1f)) {
                            // Keep a screen composed across its steps (especially the generated PDF).
                            key(item.screen) {
                                when(item.screen) {
                                    AppGuide.Screen.HOME->HomeScreen(emptyList(),{},{},{},{},{})
                                    AppGuide.Screen.DATA,AppGuide.Screen.ROSTER->MatchEditor(GuideExample.config,{},false,
                                        initialSection=if(item.screen==AppGuide.Screen.ROSTER)1 else 0,showDashboard=false)
                                    AppGuide.Screen.LINEUP->Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                                        StartSetPanel(GuideExample.config,MatchEngine.initial(GuideExample.config),{},tutorialDefaults=Demo.start(),signatureJournal=journal,onCollectSignatures={})
                                    }
                                    AppGuide.Screen.LIVE->LiveScreen(journal.config,state,journal.events.lastOrNull(),{},{},{},journal.events.isNotEmpty(),journal.redo.isNotEmpty(),{},actionTeam=1)
                                    AppGuide.Screen.HISTORY->HistoryScreen(journal,{},{})
                                    AppGuide.Screen.TIMEOUT,AppGuide.Screen.SUB,AppGuide.Screen.LIBERO,AppGuide.Screen.NOTE,AppGuide.Screen.SANCTION->
                                        OperationDialog(when(item.screen){AppGuide.Screen.TIMEOUT->"timeout";AppGuide.Screen.SUB->"sub";AppGuide.Screen.LIBERO->"libero";AppGuide.Screen.NOTE->"note";else->"sanction"},
                                            1,journal.config,state,{},{},embedded=true,
                                            exampleText=if(item.screen==AppGuide.Screen.NOTE)"Esempio guidato: annotazione comunicata dall’arbitro." else "",exampleRecipient="1")
                                    AppGuide.Screen.PDF->ExportScreen(journal,{error=it.message?:"Generazione non riuscita"},{},{},tutorialDirectory=directory,onSignatures={false})
                                }
                            }
                        }
                    }
                }
                val raw=if(targetReady)targets.entries[item.target]?.bounds else null
                val hole=raw?.translate(-rootOrigin)?.inflate(with(LocalDensity.current){4.dp.toPx()})
                Spotlight(hole)
                val density=LocalDensity.current
                var cardHeight by remember{mutableIntStateOf(0)}
                val availableHeight=with(density){maxHeight.toPx()}
                val above=hole!=null && hole.center.y>availableHeight*.5f
                val gap=with(density){12.dp.toPx()}
                val preferred=if(above)hole!!.top-cardHeight-gap else (hole?.bottom ?: 0f)+gap
                val top=preferred.coerceIn(gap,(availableHeight-cardHeight-gap).coerceAtLeast(gap))
                Surface(Modifier.align(Alignment.TopCenter).widthIn(max=560.dp).fillMaxWidth().padding(horizontal=12.dp)
                    .offset{IntOffset(0,top.roundToInt())}.heightIn(max=maxHeight*.48f)
                    .onGloballyPositioned{cardHeight=it.size.height}.testTag("guide-tooltip").semantics{paneTitle="Tutorial guidato";liveRegion=LiveRegionMode.Polite},
                    shape=MaterialTheme.shapes.large,shadowElevation=8.dp,color=MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                            Text("${step+1}/${AppGuide.steps.size} · Guida",style=MaterialTheme.typography.labelMedium)
                            TextButton(onClick=onClose,modifier=Modifier.testTag("guide-close")){Text("Esci")}
                        }
                        Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(item.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.testTag("guide-step-title"))
                            Text(item.body,style=MaterialTheme.typography.bodyMedium)
                            Text(item.tip,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            if(!targetReady && error==null)Text("Preparazione dell’esempio…",style=MaterialTheme.typography.bodySmall)
                            if(error!=null)Text("${error}. Torna indietro per riprovare o esci dalla guida.",color=MaterialTheme.colorScheme.error)
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            if(step>0)OutlinedButton(onClick={error=null;step--},modifier=Modifier.weight(1f).testTag("guide-previous")){Text("Indietro")}
                            Button(enabled=targetReady,onClick={if(step==AppGuide.steps.lastIndex)onClose() else step++},modifier=Modifier.weight(1f).testTag("guide-next")) {
                                Text(if(step==AppGuide.steps.lastIndex)"Fine" else "Avanti")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Even-odd mask leaves the real control untouched; it does not redraw a copy of the control. */
@Composable
private fun Spotlight(hole:Rect?) {
    Canvas(Modifier.fillMaxSize().testTag("guide-scrim").pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed=false).consume()
            do {
                val event=awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while(event.changes.any { it.pressed })
        }
    }) {
        val path=Path().apply {
            fillType=PathFillType.EvenOdd
            addRect(Rect(Offset.Zero,size))
            if(hole!=null)addRoundRect(RoundRect(hole,CornerRadius(12.dp.toPx())))
        }
        drawPath(path,Color.Black.copy(alpha=.68f))
        if(hole!=null)drawRoundRect(Color(0xFFFFD23F),hole.topLeft,hole.size,CornerRadius(12.dp.toPx()),style=Stroke(2.dp.toPx()))
    }
}
