package com.danielcioban.routeplanner.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.danielcioban.routeplanner.data.account.AccountSession
import com.danielcioban.routeplanner.ui.map.MapLayersButton
import com.danielcioban.routeplanner.ui.map.MapLayersMenuDialog
import com.danielcioban.routeplanner.ui.map.MapViewMode
import com.danielcioban.routeplanner.ui.menu.AppMenuOverlay
import com.danielcioban.routeplanner.ui.theme.RoutePlannerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Overlay menus must be Dialog / fillMaxSize root siblings so they cannot shove
 * height-wrapping chrome (see docs/DEV_STATUS.md).
 *
 * Uses a vanilla [Application] so this class does not open Room via
 * [com.danielcioban.routeplanner.RoutePlannerApplication].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class OverlayMenuDoesNotReflowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapLayersDialog_doesNotMoveWrappingSibling() {
        var open by mutableStateOf(false)
        val siblingY = mutableListOf<Float>()
        composeRule.setContent {
            RoutePlannerTheme {
                Column(modifier = Modifier.wrapContentHeight()) {
                    MapLayersButton(
                        onClick = { open = true },
                        modifier = Modifier.testTag("layers_anchor"),
                    )
                    Text(
                        text = "Keep me",
                        modifier = Modifier
                            .testTag("layers_sibling")
                            .onGloballyPositioned { siblingY += it.positionInRoot().y },
                    )
                    if (open) {
                        MapLayersMenuDialog(
                            selected = MapViewMode.MAP,
                            onSelected = {},
                            onDismiss = { open = false },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val before = siblingY.last()
        composeRule.onNodeWithContentDescription("Map layers").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("map_layers_menu").assertExists()
        assertEquals(before, siblingY.last(), 0.5f)
    }

    @Test
    fun appMenuOverlay_doesNotMoveRootSibling() {
        var visible by mutableStateOf(false)
        val siblingY = mutableListOf<Float>()
        composeRule.setContent {
            RoutePlannerTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.wrapContentHeight()) {
                        Text(
                            text = "Open",
                            modifier = Modifier
                                .testTag("menu_anchor")
                                .onGloballyPositioned { /* layout */ },
                        )
                        Text(
                            text = "Keep me",
                            modifier = Modifier
                                .testTag("menu_sibling")
                                .onGloballyPositioned { siblingY += it.positionInRoot().y },
                        )
                    }
                    AppMenuOverlay(
                        visible = visible,
                        accountSession = AccountSession.SignedOut,
                        onDismiss = { visible = false },
                        onSettings = {},
                        onAbout = {},
                        onStopLibrary = {},
                        onTripHistory = {},
                        onTrash = {},
                        onStats = {},
                        onFuelLog = {},
                        onAccount = {},
                        onLogout = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        val before = siblingY.last()
        composeRule.runOnUiThread { visible = true }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("app_menu_overlay").assertExists()
        assertTrue(siblingY.isNotEmpty())
        assertEquals(before, siblingY.last(), 0.5f)
    }
}
