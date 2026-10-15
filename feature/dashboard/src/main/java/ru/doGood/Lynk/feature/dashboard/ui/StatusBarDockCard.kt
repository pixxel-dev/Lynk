package ru.doGood.Lynk.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.doGood.Lynk.feature.dashboard.FlymeStatusBarDockManager
import ru.doGood.Lynk.feature.dashboard.R

/**
 * Карточка включения и позиционирования статичной системной врезки в статус-бар Flyme Auto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusBarDockCard(
    modifier: Modifier = Modifier,
    flymeManager: FlymeStatusBarDockManager = FlymeStatusBarDockManager
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(flymeManager.isEnabled(context)) }
    var position by remember { mutableIntStateOf(flymeManager.getPosition(context)) }
    var screenPosition by remember { mutableStateOf(flymeManager.getScreenPosition(context)) }
    var offsetX by remember { mutableFloatStateOf(flymeManager.getOffsetX(context).toFloat()) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.dock_bar_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.dock_bar_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { isChecked ->
                        enabled = isChecked
                        flymeManager.toggleDock(context, isChecked)
                    }
                )
            }

            // Выбор положения на экране (Вверху / Внизу)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = screenPosition == FlymeStatusBarDockManager.POSITION_TOP,
                    onClick = {
                        screenPosition = FlymeStatusBarDockManager.POSITION_TOP
                        flymeManager.setPosition(context, FlymeStatusBarDockManager.POSITION_TOP)
                    },
                    label = { Text(stringResource(R.string.dock_screen_pos_top)) }
                )
                FilterChip(
                    selected = screenPosition == FlymeStatusBarDockManager.POSITION_BOTTOM,
                    onClick = {
                        screenPosition = FlymeStatusBarDockManager.POSITION_BOTTOM
                        flymeManager.setPosition(context, FlymeStatusBarDockManager.POSITION_BOTTOM)
                    },
                    label = { Text(stringResource(R.string.dock_screen_pos_bottom)) }
                )
            }

            // Выбор зоны крепления в статус-баре (Справа / По центру / Слева)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = position == 0,
                    onClick = {
                        position = 0
                        flymeManager.setPosition(context, 0, offsetX.toInt())
                    },
                    label = { Text(stringResource(R.string.dock_pos_right)) }
                )
                FilterChip(
                    selected = position == 1,
                    onClick = {
                        position = 1
                        flymeManager.setPosition(context, 1, offsetX.toInt())
                    },
                    label = { Text(stringResource(R.string.dock_pos_center)) }
                )
                FilterChip(
                    selected = position == 2,
                    onClick = {
                        position = 2
                        flymeManager.setPosition(context, 2, offsetX.toInt())
                    },
                    label = { Text(stringResource(R.string.dock_pos_left)) }
                )
            }

            // Ползунок отступа от часов/иконок статус-бара
            if (position != 1) {
                Column {
                    Text(
                        text = "${stringResource(R.string.dock_offset_label)}: ${offsetX.toInt()} dp",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = offsetX,
                        onValueChange = { newVal ->
                            offsetX = newVal
                            flymeManager.setPosition(context, position, newVal.toInt())
                        },
                        valueRange = 20f..600f
                    )
                }
            }
        }
    }
}