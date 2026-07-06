package fr.achabitation.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AchabitationTheme {
                val vm: MainViewModel = viewModel()
                AchabitationApp(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchabitationApp(vm: MainViewModel) {
    val state = vm.state
    var homeSection by remember { mutableStateOf(HomeSection.Trips) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (state.sessionExpired) {
        SessionExpiredScaffold(vm)
        return
    }

    if (state.auth == null) {
        AuthScaffold(vm)
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                state = state,
                homeSection = homeSection,
                onHomeSection = { section ->
                    homeSection = section
                    if (state.selectedTrip != null) vm.backToTrips()
                    scope.launch { drawerState.close() }
                },
                onTripTab = { tab ->
                    vm.setTab(tab)
                    scope.launch { drawerState.close() }
                },
                onBackToTrips = {
                    vm.backToTrips()
                    homeSection = HomeSection.Trips
                    scope.launch { drawerState.close() }
                },
                onLogout = {
                    scope.launch { drawerState.close() }
                    vm.logout()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(if (state.selectedTrip == null) "Achabitation" else state.selectedTrip.name, fontWeight = FontWeight.Bold)
                            Text(
                                if (state.selectedTrip == null) homeSection.description else state.selectedTab.label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text("☰", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = {
                        if (state.selectedTrip != null) {
                            IconButton(onClick = vm::backToTrips) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Retour aux voyages")
                            }
                        }
                        IconButton(onClick = { if (state.selectedTrip == null) vm.refreshDashboard() else vm.refreshTripData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Synchroniser")
                        }
                    }
                )
            }
        ) { padding ->
            Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (state.selectedTrip == null) {
                        HomeScreen(vm = vm, selected = homeSection, onSelect = { homeSection = it })
                    } else {
                        TripDetailScreen(vm = vm)
                    }
                    LoadingOverlay(state.loading)
                }
            }
        }
    }
}

@Composable
fun AppDrawer(
    state: AppUiState,
    homeSection: HomeSection,
    onHomeSection: (HomeSection) -> Unit,
    onTripTab: (TripTab) -> Unit,
    onBackToTrips: () -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Achabitation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(state.auth?.displayName ?: state.auth?.email ?: "Compte connecté", style = MaterialTheme.typography.bodyMedium)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Application", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            HomeSection.values().forEach { section ->
                NavigationDrawerItem(
                    selected = state.selectedTrip == null && homeSection == section,
                    onClick = { onHomeSection(section) },
                    label = { Text(section.label) }
                )
            }

            val selectedTrip = state.selectedTrip
            if (selectedTrip != null) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(selectedTrip.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                TripTab.values().forEach { tab ->
                    NavigationDrawerItem(
                        selected = state.selectedTab == tab,
                        onClick = { onTripTab(tab) },
                        label = { Text(tab.label) }
                    )
                }
                NavigationDrawerItem(
                    selected = false,
                    onClick = onBackToTrips,
                    label = { Text("Changer de voyage") }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            NavigationDrawerItem(selected = false, onClick = onLogout, label = { Text("Déconnexion") })
        }
    }
}
