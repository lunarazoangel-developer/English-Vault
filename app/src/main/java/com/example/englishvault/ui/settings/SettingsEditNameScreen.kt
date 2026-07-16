package com.example.englishvault.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.englishvault.R
import com.example.englishvault.ui.progress.arcade.ArcadeFonts
import com.example.englishvault.ui.progress.arcade.LocalArcadePalette
import com.example.englishvault.ui.progress.arcade.components.ArcadeButton
import com.example.englishvault.ui.progress.arcade.components.ArcadeLabel
import com.example.englishvault.ui.settings.viewmodel.SettingsEditNameViewModel

/**
 * Sub-screen that lets the user rename their profile.
 *
 * The form is driven by [SettingsEditNameViewModel]. Validation runs
 * inside the VM (empty / whitespace-only input is rejected) and the
 * Save button calls [SettingsEditNameViewModel.save]. When the VM
 * flips its `saved` flag, the screen pops back to the Settings hub.
 *
 * Phase 8.x: the screen now follows the arcade style — same
 * palette as the Settings hub and the rest of the arcade-aware
 * UI. The text field uses the primary color for the focused
 * outline + cursor; the Save button is a full-width
 * [ArcadeButton] instead of the M3 `PrimaryButton` to stay
 * consistent with the rest of the arcade language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsEditNameScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsEditNameViewModel = hiltViewModel()
) {
    val palette = LocalArcadePalette.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        modifier = modifier,
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_edit_name_title),
                        color = palette.textMain,
                        fontFamily = ArcadeFonts.Display,
                        fontWeight = ArcadeFonts.DisplayWeight,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(
                                id = R.string.settings_back
                            ),
                            tint = palette.textMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.surface,
                    titleContentColor = palette.textMain,
                    navigationIconContentColor = palette.textMain
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = {
                    Text(
                        text = stringResource(id = R.string.settings_edit_name_hint),
                        color = palette.textDim
                    )
                },
                singleLine = true,
                isError = state.error != null,
                supportingText = {
                    if (state.error == SettingsEditNameViewModel.ERROR_EMPTY) {
                        ArcadeLabel(
                            text = stringResource(id = R.string.settings_edit_name_empty_error),
                            color = palette.primary
                        )
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = palette.textMain,
                    unfocusedTextColor = palette.textMain,
                    focusedBorderColor = palette.primary,
                    unfocusedBorderColor = palette.border,
                    focusedLabelColor = palette.primary,
                    unfocusedLabelColor = palette.textDim,
                    cursorColor = palette.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ArcadeButton(
                text = stringResource(id = R.string.settings_edit_name_save),
                onClick = viewModel::save,
                color = palette.primary,
                shadow = palette.shadowOf(palette.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }
    }
}
