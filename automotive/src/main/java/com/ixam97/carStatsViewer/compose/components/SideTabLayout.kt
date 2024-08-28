package com.mbuehler.carStatsViewer.compose.components

import android.util.Log
import android.view.WindowInsets.Side
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mbuehler.carStatsViewer.R
import com.mbuehler.carStatsViewer.compose.theme.CarTheme

@Composable
fun SideTabLayout(
    modifier: Modifier = Modifier,
    tabs: List<SideTab>,
    tabsColumnWidth: Dp? = null,
    topLevelBackAction: () -> Unit,
    topLevelTitle: String,
    tabsColumnBackground: Color = MaterialTheme.colors.surface
) {
    val navController = rememberNavController()

    var size by remember {
        mutableStateOf(IntSize.Zero)
    }


    var selectedIndex by remember { mutableStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
    ) {
        val width = with(LocalDensity.current){ size.width.toDp() }
        Log.d("WINDOW SIZE", "$width dp")

        if (width < 1300.dp) {
            // Nested navigation for slim displays
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "Parent"
                ) {
                    composable("Parent") {
                        Column {
                            CarHeader(title = topLevelTitle, onBackClick = topLevelBackAction)
                            Column {
                                tabs.filter { it.type == SideTab.Type.Tab }.forEachIndexed { index, tab ->
                                    if (index > 0) Divider(modifier = Modifier.padding(horizontal = 24.dp))
                                    CarRow(
                                        title = tab.tabTitle,
                                        onClick = { navController.navigate(tab.route)},
                                        browsable = true
                                    )
                                }
                            }
                        }
                    }
                    tabs.forEach { tab ->
                        composable(tab.route) {
                            Column {
                                CarHeader(title = tab.tabTitle, onBackClick = { navController.popBackStack() })
                                tab.content(navController)
                            }
                        }
                    }
                }
            }
        } else {
            // Wide screens allow for side Tabs
            Row {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .defaultMinSize(minWidth = 500.dp)
                        .fillMaxHeight()
                        .background(tabsColumnBackground)
                        // .padding(top = 10.dp),
                    // verticalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    CarHeader(title = topLevelTitle, onBackClick = topLevelBackAction)
                    Spacer(modifier = Modifier.size(30.dp))
                    tabs.filter{it.type == SideTab.Type.Tab}.forEachIndexed { index, tab ->
                        Text(
                            modifier = Modifier
                                .clickable {
                                    selectedIndex = index
                                    navController.navigate(tab.route) {
                                        navController.popBackStack()
                                    }
                                }
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(CarTheme.buttonCornerRadius))
                                .background(if (index == selectedIndex) MaterialTheme.colors.secondary else Color.Transparent)
                                .padding(CarTheme.buttonPaddingValues)
                                .padding(end = 10.dp),
                            text = tab.tabTitle,
                            style = MaterialTheme.typography.h2,
                            // color = if (index == selectedIndex) MaterialTheme.colors.secondary else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Visible
                        )
                    }
                }
                // Divider(
                //     modifier = Modifier
                //         .padding(20.dp)
                //         .fillMaxHeight()
                //         .width(2.dp)
                // )
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = tabs.filter { it.type == SideTab.Type.Tab }[0].route
                    ) {
                        tabs.forEach { tab ->
                            composable(tab.route) {
                                Column {
                                    if (tab.type == SideTab.Type.Tab) CarHeader(title = tab.tabTitle)
                                    else CarHeader(
                                        title = tab.tabTitle,
                                        onBackClick = { navController.popBackStack() }
                                    )
                                    tab.content(navController)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class SideTab(
    val tabTitle: String,
    val route: String,
    val type: Type,
    val content: @Composable (navController: NavController) -> Unit,
) {
    sealed class Type() {
        object Tab: Type()
        object Detail: Type()
    }
}