package com.xiaomanjun.sleepdownschedule.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

/** Compatibility Intents hand off to the one root host rather than adding another page stack. */
object RootDestinationRequests {
    val pending = MutableStateFlow<String?>(null)
    fun open(id: String) { pending.value = id }
}

val LocalRootDestination = staticCompositionLocalOf<(String) -> Unit> { RootDestinationRequests::open }

@Composable
internal fun RootDestinationPane(
    selected: Boolean,
    content: @Composable BoxScope.() -> Unit
) {
    var visited by rememberSaveable { mutableStateOf(selected) }
    if (selected) SideEffect { visited = true }
    if (!visited && !selected) return
    val visibility = remember { Animatable(0f) }
    LaunchedEffect(selected) { visibility.animateTo(if (selected) 1f else 0f, tween(240)) }
    val draws = selected || visibility.value > .001f
    // A hidden retained page is not placed: alpha=0 alone still participates in hit testing.
    Box(Modifier.fillMaxSize().layout { measurable, constraints ->
        val child = measurable.measure(constraints)
        layout(child.width, child.height) { if (draws) child.placeRelative(0, 0) }
    }.graphicsLayer {
        alpha = visibility.value
        translationX = (1f - visibility.value) * 24.dp.toPx()
    }.then(if (!selected && draws) Modifier.clearAndSetSemantics {}.pointerInput(Unit) {
        awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() } }
    } else Modifier)) {
        CompositionLocalProvider(LocalHomePaneVisible provides (LocalHomePaneVisible.current && selected)) { content() }
    }
}
