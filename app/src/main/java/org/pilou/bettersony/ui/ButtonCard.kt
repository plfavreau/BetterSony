package org.pilou.bettersony.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.pilou.bettersony.data.CustomButton

private val SHORT_LABELS = mapOf(
    CustomButton.AMBIENT to "Ambient",
    CustomButton.ASSISTANT to "Google",
    CustomButton.ALEXA to "Alexa",
)

private fun hint(b: CustomButton): String = when (b) {
    CustomButton.AMBIENT -> "Press to switch noise cancelling and ambient sound. Hold to run the noise cancelling optimizer."
    CustomButton.ASSISTANT -> "Press to talk to Google Assistant or hear your notifications."
    CustomButton.ALEXA -> "Press to talk to Alexa."
}

/** The CUSTOM button on the left earcup. Changing it restarts the headphones, so it goes through [onSelect] to confirm. */
@Composable
fun ButtonCard(current: CustomButton, onSelect: (CustomButton) -> Unit) {
    Column(SectionPadding) {
        SectionLabel("Custom button")
        Card {
            Column {
                Segmented(
                    options = CustomButton.entries.map { it to SHORT_LABELS.getValue(it) },
                    selected = current,
                    onSelect = { if (it != current) onSelect(it) },
                )
                Spacer(Modifier.height(10.dp))
                BasicText(hint(current), style = Type.callout, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}
