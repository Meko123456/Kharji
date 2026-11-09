package io.github.meko123456.kharji.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The receipt scanner's status line, above the Capture button. TalkBack's speech can't be
 * observed from a test, so this checks what TalkBack acts on: the line is a polite live region,
 * and each step is a change of the same node's words, not a node appearing.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ScanStatusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachStepOfAScanIsANewWordingOfOnePoliteLiveRegion() {
        var status by mutableStateOf<String?>(null)
        rule.setContent { ScanStatus(status) }
        val line = rule.onNodeWithTag(SCAN_STATUS_TAG, useUnmergedTree = true)
        line.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        // Nothing to say: no height, so the Capture button sits where it always did.
        line.assertHeightIsEqualTo(0.dp)
        val node = line.fetchSemanticsNode().id

        status = "Reading receipt…"
        line.assertTextEquals("Reading receipt…")
        assertEquals(node, line.fetchSemanticsNode().id)

        status = "Couldn't read a total — try again, straighter."
        line.assertTextEquals("Couldn't read a total — try again, straighter.")
        assertEquals(node, line.fetchSemanticsNode().id)

        status = "Reading receipt…"
        line.assertTextEquals("Reading receipt…")
        assertEquals(node, line.fetchSemanticsNode().id)
        line.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }
}
