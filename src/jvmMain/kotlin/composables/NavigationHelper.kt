package composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch

val DEFAULT_PAGE = Page.PasswordOperation

enum class Page(private val iconVector: ImageVector, private val title: String) {
    PasswordOperation(Icons.Default.Key, "密码处理"),
    ProcessOperation(Icons.Default.Terminal, "进程操作");

    private val icon: @Composable () -> Unit = { Icon(iconVector, title) }
    private val label: @Composable () -> Unit = { Text(title) }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DrawerItem(currentPage: Page, changePage: (Page) -> Unit) {
        NavigationDrawerItem(
            label = label,
            selected = currentPage == this,
            onClick = {
                changePage(this)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            icon = icon
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModalDrawerItem(drawerState: DrawerState, currentPage: Page, changePage: (Page) -> Unit) {
        val coroutineScope = rememberCoroutineScope()

        DrawerItem(currentPage) {
            changePage(this)
            coroutineScope.launch {
                drawerState.close()
            }
        }
    }

    @Composable
    fun RailItem(currentPage: Page, changePage: (Page) -> Unit) {
        NavigationRailItem(
            selected = currentPage == this,
            onClick = { changePage(this) },
            icon = icon,
            label = label
        )
    }
}
