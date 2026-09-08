package com.companion.cc.ui.settings

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.companion.cc.ui.components.V9PMChoiceRow

@Composable
internal fun SettingsSelectionRow(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    V9PMChoiceRow(
        title = title,
        subtitle = description,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        trailing = {
            RadioButton(selected = selected, onClick = null)
        }
    )
}
