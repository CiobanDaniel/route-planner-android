package com.danielcioban.routeplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danielcioban.routeplanner.ui.layout.AppPanes
import com.danielcioban.routeplanner.ui.theme.IslandColors
import com.danielcioban.routeplanner.ui.theme.LocalReduceMotion

@Composable
fun IslandDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String? = null,
    onDismissButton: (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    var dismissing by remember { androidx.compose.runtime.mutableStateOf(false) }
    fun requestDismiss() {
        if (dismissing) return
        if (reduceMotion) onDismissRequest() else {
            dismissing = true
            visibility.targetState = false
        }
    }
    LaunchedEffect(dismissing) {
        if (dismissing) {
            snapshotFlow { visibility.isIdle && !visibility.currentState }.first { it }
            onDismissRequest()
        }
    }

    Dialog(
        onDismissRequest = ::requestDismiss,
        properties = properties,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(IslandColors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::requestDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visibleState = visibility,
                enter = if (reduceMotion) EnterTransition.None else androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(200)) +
                        androidx.compose.animation.scaleIn(
                            initialScale = 0.92f,
                            animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                            )
                        ),
                exit = if (reduceMotion) ExitTransition.None else androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) +
                       androidx.compose.animation.scaleOut(targetScale = 0.92f),
            ) {
                FloatingIsland(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = AppPanes.ReadableMaxWidth)
                        .padding(horizontal = 24.dp)
                        .heightIn(max = 560.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        ),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = 20.dp,
                    elevation = 16.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = title,
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IslandColors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        content()
                        if (confirmLabel != null || dismissLabel != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (dismissLabel != null) {
                                    TextButton(
                                        onClick = {
                                            if (onDismissButton != null) {
                                                onDismissButton.invoke()
                                            } else {
                                                requestDismiss()
                                            }
                                        }
                                    ) {
                                        Text(dismissLabel, color = IslandColors.onSurfaceMuted)
                                    }
                                }
                                if (confirmLabel != null && onConfirm != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    TextButton(onClick = {
                                        onConfirm()
                                    }) {
                                        Text(confirmLabel, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }
}
