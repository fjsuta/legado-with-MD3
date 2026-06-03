package io.legado.app.ui.config.translation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.legado.app.R
import io.legado.app.ui.config.translation.components.TranslationConfigOptions
import io.legado.app.ui.widget.components.AppScaffold
import io.legado.app.ui.widget.components.topbar.GlassMediumFlexibleTopAppBar
import io.legado.app.ui.widget.components.topbar.TopBarNavigationButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationConfigScreen(
    onBackClick: () -> Unit,
    onNavigateToProviderList: () -> Unit
) {
    AppScaffold(
        topBar = {
            GlassMediumFlexibleTopAppBar(
                title = stringResource(R.string.translation_config),
                navigationIcon = { TopBarNavigationButton(onClick = onBackClick) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TranslationConfigOptions(
                onNavigateToProviderList = onNavigateToProviderList
            )
        }
    }
}
