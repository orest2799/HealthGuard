package com.example.healthguard.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.healthguard.viewmodel.MatchOverlayViewModel
import kotlin.math.roundToInt

@Composable
fun MatchBubbleOverlay(
    vm: MatchOverlayViewModel,
    onOpenChat: (sessionId: String, title: String) -> Unit
) {
    val visible by vm.visible.collectAsState()
    val payload by vm.payload.collectAsState()
    if (!visible || payload == null) return


    val density = LocalDensity.current

    var sizePx by remember { mutableStateOf(Offset.Zero) }
    var containerSizePx by remember { mutableStateOf(Offset.Zero) }
    var pos by remember { mutableStateOf(Offset.Zero) }
    val vmPos by vm.position.collectAsState()

    LaunchedEffect(vmPos) { pos = vmPos }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                containerSizePx = Offset(it.size.width.toFloat(), it.size.height.toFloat())
                if (pos == Offset.Zero && containerSizePx != Offset.Zero) {

                    val startX = with(density) { (containerSizePx.x - 96.dp.toPx() - 24.dp.toPx()) }
                    val startY = with(density) { (containerSizePx.y - 96.dp.toPx() - 150.dp.toPx()) }
                    pos = Offset(startX, startY)
                    vm.setPosition(pos)
                }
            }
    ) {
        Surface(
            shape = CircleShape,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(64.dp)
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { vm.setPosition(pos) }
                    ) { change, dragAmount ->
                        change.consume() // ok; or use consumeAllChanges() on newer Compose
                        val next = pos + dragAmount
                        val maxX = (containerSizePx.x - sizePx.x).coerceAtLeast(0f)
                        val maxY = (containerSizePx.y - sizePx.y).coerceAtLeast(0f)
                        pos = Offset(
                            x = next.x.coerceIn(0f, maxX),
                            y = next.y.coerceIn(0f, maxY)
                        )
                    }
                }
                .onGloballyPositioned {
                    sizePx = Offset(it.size.width.toFloat(), it.size.height.toFloat())
                }
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .shadow(8.dp, CircleShape)
                .clickable {

                    vm.hide()
                    onOpenChat(payload!!.sessionId, payload!!.title)
                }
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(payload!!.title.take(2).uppercase(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
