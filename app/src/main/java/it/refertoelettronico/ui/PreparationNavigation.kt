package it.refertoelettronico.ui

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag

internal class PreparationTarget(val bring:BringIntoViewRequester,val focus:FocusRequester?) {
    var placed by mutableStateOf(false)
}
internal class PreparationTargets {val entries=mutableStateMapOf<String,PreparationTarget>()}
internal fun Modifier.preparationTarget(targets:PreparationTargets,key:String,focus:Boolean=false):Modifier=composed {
    val entry=remember(targets,key){PreparationTarget(BringIntoViewRequester(),if(focus)FocusRequester()else null)}
    DisposableEffect(targets,key){targets.entries[key]=entry;onDispose{if(targets.entries[key]===entry)targets.entries.remove(key)}}
    var modifier=this.bringIntoViewRequester(entry.bring).onGloballyPositioned{entry.placed=true}.testTag("setup-field-$key")
    if(entry.focus!=null)modifier=modifier.focusRequester(entry.focus)
    modifier
}
