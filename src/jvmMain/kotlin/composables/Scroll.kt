package composables

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun VerticalScroll(
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable () -> Unit
) {
    Box(Modifier.verticalScroll(scrollState)) {
        content()
    }
}