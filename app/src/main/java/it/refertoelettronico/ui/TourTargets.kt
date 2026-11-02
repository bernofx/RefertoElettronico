package it.refertoelettronico.ui

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

internal class TourTarget(val requester:BringIntoViewRequester) {
    var bounds by mutableStateOf(Rect.Zero)
    var placed by mutableStateOf(false)
    var activate:(()->Unit)?=null
}
internal class TourTargets { val entries=mutableStateMapOf<String,TourTarget>() }
internal val LocalTourTargets=staticCompositionLocalOf<TourTargets?>{null}

/** No effect outside the demonstration. Bounds come from the actual control after layout/scroll. */
internal fun Modifier.tourTarget(id:String,activate:(()->Unit)?=null):Modifier=composed {
    val registry=LocalTourTargets.current
    if(registry==null)this else {
        val entry=remember(registry,id){TourTarget(BringIntoViewRequester())}
        SideEffect{entry.activate=activate}
        DisposableEffect(registry,id){registry.entries[id]=entry;onDispose{if(registry.entries[id]===entry)registry.entries.remove(id)}}
        this.bringIntoViewRequester(entry.requester).onGloballyPositioned{entry.bounds=it.boundsInRoot();entry.placed=true}
    }
}
