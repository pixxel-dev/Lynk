package ru.doGood.Lynk.feature.dashboard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.doGood.Lynk.feature.dashboard.R
import ru.doGood.Lynk.feature.dashboard.openAppSystemSettings
import ru.doGood.Lynk.feature.dashboard.subverForceStopApp
import ru.doGood.Lynk.feature.dashboard.subverGivePermissions
import ru.doGood.Lynk.feature.dashboard.subverInstallApk
import ru.doGood.Lynk.feature.dashboard.subverUninstallApp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubverPackageControlCard(
    initialApkPath: String = "",
    initialPackageName: String = "ru.vk.store",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apkPath by remember(initialApkPath) { mutableStateOf(initialApkPath) }
    var targetPackage by remember(initialPackageName) { mutableStateOf(initialPackageName) }
    var statusLog by remember { mutableStateOf("") }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.subver_panel_title),
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = apkPath,
                onValueChange = { apkPath = it },
                label = { Text(stringResource(R.string.subver_apk_path_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    scope.subverInstallApk(context, apkPath) { result ->
                        statusLog = result
                    }
                },
                enabled = apkPath.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.subver_btn_install_and_grant))
            }

            OutlinedTextField(
                value = targetPackage,
                onValueChange = { targetPackage = it },
                label = { Text(stringResource(R.string.subver_package_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.subverGivePermissions(targetPackage) { logs ->
                            statusLog = logs.joinToString("\n")
                        }
                    },
                    enabled = targetPackage.isNotBlank()
                ) {
                    Text(stringResource(R.string.subver_btn_give_permissions))
                }

                OutlinedButton(
                    onClick = {
                        scope.subverForceStopApp(targetPackage) { result ->
                            statusLog = result
                        }
                    },
                    enabled = targetPackage.isNotBlank()
                ) {
                    Text(stringResource(R.string.subver_btn_force_stop))
                }

                OutlinedButton(
                    onClick = {
                        scope.subverUninstallApp(targetPackage) { result ->
                            statusLog = result
                        }
                    },
                    enabled = targetPackage.isNotBlank()
                ) {
                    Text(stringResource(R.string.subver_btn_uninstall_app))
                }

                OutlinedButton(
                    onClick = {
                        openAppSystemSettings(context, targetPackage)
                    },
                    enabled = targetPackage.isNotBlank()
                ) {
                    Text(stringResource(R.string.subver_btn_system_settings))
                }
            }

            if (statusLog.isNotBlank()) {
                Text(
                    text = statusLog,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}