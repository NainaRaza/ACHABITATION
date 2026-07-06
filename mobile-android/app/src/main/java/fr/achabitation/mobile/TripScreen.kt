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

@Composable
fun TripDetailScreen(vm: MainViewModel) {
    val state = vm.state
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { StatusMessages(state, vm::clearMessage) }
        item { TripHeader(state, vm) }
        item { TripSectionSwitcher(state.selectedTab, vm::setTab) }
        when (state.selectedTab) {
            TripTab.Overview -> overviewContent(vm, this)
            TripTab.Persons -> personsContent(vm, this)
            TripTab.Expenses -> expensesContent(vm, this)
            TripTab.Summary -> summaryContent(vm, this)
            TripTab.Invitations -> invitationsContent(vm, this)
            TripTab.Audit -> auditContent(vm, this)
        }
    }
}

@Composable
fun TripSectionSwitcher(selected: TripTab, onSelect: (TripTab) -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Sections du voyage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Utilise le menu ☰ pour naviguer partout, ou ces raccourcis pour le voyage ouvert.", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TripTabButton(TripTab.Overview, selected, onSelect, Modifier.weight(1f))
                TripTabButton(TripTab.Persons, selected, onSelect, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TripTabButton(TripTab.Expenses, selected, onSelect, Modifier.weight(1f))
                TripTabButton(TripTab.Summary, selected, onSelect, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TripTabButton(TripTab.Invitations, selected, onSelect, Modifier.weight(1f))
                TripTabButton(TripTab.Audit, selected, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TripTabButton(tab: TripTab, selected: TripTab, onSelect: (TripTab) -> Unit, modifier: Modifier = Modifier) {
    if (selected == tab) {
        Button(onClick = { onSelect(tab) }, modifier = modifier) { Text(tab.label) }
    } else {
        OutlinedButton(onClick = { onSelect(tab) }, modifier = modifier) { Text(tab.label) }
    }
}

fun overviewContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    val state = vm.state
    item { CurrentUserClaimCard(vm) }
    item { TripConstraintsCard(vm) }
    item { DashboardStatsCard(state) }
    item { ExportCard(vm) }
}

@Composable
fun TripHeader(state: AppUiState, vm: MainViewModel) {
    val trip = state.selectedTrip ?: return
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(trip.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${dateFr(trip.startDate)} → ${dateFr(trip.endDate)} · devise ${trip.referenceCurrency}")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${state.persons.count { it.active }} pers.") })
                AssistChip(onClick = {}, label = { Text("${state.expenses.size} dép.") })
                AssistChip(onClick = {}, label = { Text(money(totalExpenses(state), trip.referenceCurrency)) })
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.setTab(TripTab.Expenses) }, modifier = Modifier.weight(1f)) { Text("Dépense") }
                OutlinedButton(onClick = { vm.setTab(TripTab.Persons) }, modifier = Modifier.weight(1f)) { Text("Personnes") }
                OutlinedButton(onClick = { vm.setTab(TripTab.Summary) }, modifier = Modifier.weight(1f)) { Text("Soldes") }
            }
            TextButton(onClick = vm::refreshTripData, enabled = !state.loading) { Text("Synchroniser le voyage") }
        }
    }
}

@Composable
fun DashboardStatsCard(state: AppUiState) {
    val trip = state.selectedTrip ?: return
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tableau de bord", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox("Personnes", state.persons.count { it.active }.toString(), Modifier.weight(1f))
                MetricBox("Dépenses", state.expenses.size.toString(), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBox("Total payé", money(totalExpenses(state), trip.referenceCurrency), Modifier.weight(1f))
                MetricBox("Remboursements", (state.summary?.settlements?.size ?: 0).toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier, colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TripConstraintsCard(vm: MainViewModel) {
    val trip = vm.state.selectedTrip ?: return
    var constraints by remember(trip.id, trip.customConstraints) { mutableStateOf(trip.customConstraints.joinToString(", ")) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Contraintes du voyage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Ces contraintes peuvent ensuite être affectées aux personnes et aux dépenses.")
            OutlinedTextField(constraints, { constraints = it }, label = { Text("Contraintes, séparées par virgule") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.updateTripConstraints(constraints) }, enabled = !vm.state.loading) { Text("Enregistrer les contraintes") }
        }
    }
}

@Composable
fun CurrentUserClaimCard(vm: MainViewModel) {
    val state = vm.state
    val linked = state.persons.any { it.linkedUserId == state.auth?.userId && it.active }
    val guests = state.persons.filter { it.active && it.guest }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mon rattachement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (linked) {
                Text("Ton compte est déjà lié à une personne dans ce voyage.")
            } else {
                Text("Tu peux créer ta personne directement ou te rattacher à un guest existant.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.createCurrentUserPerson(applyProfile = true) }, enabled = !state.loading) { Text("M’ajouter avec mon profil") }
                }
                OutlinedButton(onClick = { vm.createCurrentUserPerson(applyProfile = false) }, enabled = !state.loading) { Text("M’ajouter en moyenne") }
                if (guests.isNotEmpty()) {
                    Divider()
                    Text("Guests disponibles", fontWeight = FontWeight.Bold)
                    guests.forEach { guest ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(guest.name, fontWeight = FontWeight.Bold)
                                Text(guest.presencePeriods.joinToString(" · ") { "${dateFr(it.startDate)} → ${dateFr(it.endDate)}" })
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TextButton(onClick = { vm.linkGuest(guest.id, applyProfile = true) }) { Text("C’est moi + profil") }
                                TextButton(onClick = { vm.linkGuest(guest.id, applyProfile = false) }) { Text("C’est moi") }
                            }
                        }
                    }
                }
            }
        }
    }
}
