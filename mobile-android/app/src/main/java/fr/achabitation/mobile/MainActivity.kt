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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScaffold(vm: MainViewModel) {
    Scaffold { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding), color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize()) {
                AuthScreen(vm)
                LoadingOverlay(vm.state.loading)
            }
        }
    }
}

@Composable
fun LoadingOverlay(loading: Boolean) {
    if (loading) {
        Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f), modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                ElevatedCard {
                    Text("Synchronisation…", modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: MainViewModel, selected: HomeSection, onSelect: (HomeSection) -> Unit) {
    val state = vm.state
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { StatusMessages(state, vm::clearMessage) }
        item { HomeQuickActions(selected = selected, onSelect = onSelect) }
        when (selected) {
            HomeSection.Trips -> homeTripsContent(vm, this, onSelect)
            HomeSection.Create -> item { CreateTripCard(vm) }
            HomeSection.Join -> item { JoinTripCard(vm) }
            HomeSection.Profile -> item { ProfileCard(vm) }
            HomeSection.Account -> item { AccountCard(vm) }
        }
    }
}

fun homeTripsContent(vm: MainViewModel, scope: LazyListScope, onSelect: (HomeSection) -> Unit) = with(scope) {
    val state = vm.state
    item { WelcomeCard(state) }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onSelect(HomeSection.Create) }, modifier = Modifier.weight(1f)) { Text("Créer") }
            OutlinedButton(onClick = { onSelect(HomeSection.Join) }, modifier = Modifier.weight(1f)) { Text("Rejoindre") }
        }
    }
    item { Text("Mes voyages", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
    if (state.trips.isEmpty()) {
        item { EmptyCard("Aucun voyage. Crée un voyage ou rejoins-en un avec un code d’invitation.") }
    } else {
        items(state.trips, key = { it.id }) { trip -> TripListItem(trip, vm) }
    }
}

@Composable
fun HomeQuickActions(selected: HomeSection, onSelect: (HomeSection) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(HomeSection.Trips, HomeSection.Create, HomeSection.Join, HomeSection.Profile).forEach { section ->
            if (selected == section) {
                Button(onClick = { onSelect(section) }, modifier = Modifier.weight(1f)) { Text(section.label, maxLines = 1) }
            } else {
                OutlinedButton(onClick = { onSelect(section) }, modifier = Modifier.weight(1f)) { Text(section.label, maxLines = 1) }
            }
        }
    }
}

@Composable
fun WelcomeCard(state: AppUiState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Bonjour ${state.auth?.displayName ?: ""}".trim(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Sélectionne un voyage, crée-en un nouveau ou utilise un code d’invitation.")
            Text("${state.trips.size} voyage(s) disponible(s)", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TripListItem(trip: TripResponse, vm: MainViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { vm.selectTrip(trip) }) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${dateFr(trip.startDate)} → ${dateFr(trip.endDate)}")
                }
                AssistChip(onClick = {}, label = { Text(trip.referenceCurrency) })
            }
            if (trip.customConstraints.isNotEmpty()) Text("Contraintes : ${trip.customConstraints.joinToString()}")
            Text("Toucher pour ouvrir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

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

@Composable
fun StatusMessages(state: AppUiState, clear: () -> Unit) {
    val text = state.error ?: state.message
    if (text != null) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (state.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text, modifier = Modifier.weight(1f))
                TextButton(onClick = clear) { Text("OK") }
            }
        }
    }
}

@Composable
fun AuthScreen(vm: MainViewModel) {
    val state = vm.state
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registerMode by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Achabitation", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Application mobile de partage de dépenses avec contraintes RAV.")
                }
            }
        }
        item { StatusMessages(state, vm::clearMessage) }
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (registerMode) "Créer un compte" else "Connexion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Sur émulateur Android, le backend local Spring Boot se joint via 10.0.2.2.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = vm::updateBaseUrl,
                        label = { Text("URL API") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    if (registerMode) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nom affiché") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loading,
                        onClick = { if (registerMode) vm.register(email, displayName, password) else vm.login(email, password) }
                    ) { Text(if (registerMode) "Créer le compte" else "Se connecter") }
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { registerMode = !registerMode }) {
                        Text(if (registerMode) "J’ai déjà un compte" else "Créer un compte")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(vm: MainViewModel) {
    var selected by remember { mutableStateOf(HomeSection.Trips) }
    HomeScreen(vm = vm, selected = selected, onSelect = { selected = it })
}

@Composable
fun AccountCard(vm: MainViewModel) {
    val auth = vm.state.auth
    var expanded by remember { mutableStateOf(false) }
    var email by remember(auth?.email) { mutableStateOf(auth?.email.orEmpty()) }
    var displayName by remember(auth?.displayName) { mutableStateOf(auth?.displayName.orEmpty()) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(auth?.displayName ?: "Compte", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(auth?.email ?: "")
                }
                TextButton(onClick = vm::logout) { Text("Déconnexion") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Fermer" else "Modifier le compte") }
            }
            if (expanded) {
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(displayName, { displayName = it }, label = { Text("Nom affiché") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { vm.updateAccount(email, displayName) }, enabled = email.isNotBlank() && displayName.isNotBlank() && !vm.state.loading) {
                    Text("Enregistrer le compte")
                }
            }
        }
    }
}

@Composable
fun ProfileCard(vm: MainViewModel) {
    val profile = vm.state.profile
    var expanded by remember { mutableStateOf(false) }
    if (profile == null) {
        EmptyCard("Profil en cours de chargement.")
        return
    }

    var displayName by remember(profile.userId, profile.displayName) { mutableStateOf(profile.displayName) }
    var weightMode by remember(profile.userId, profile.weightMode) { mutableStateOf(profile.weightMode ?: "AVERAGE") }
    var livingRest by remember(profile.userId, profile.livingRest) { mutableStateOf(profile.livingRest.toInput()) }
    var advanced by remember(profile.userId, profile.advancedLivingRest) { mutableStateOf(profile.advancedLivingRest) }
    var income by remember(profile.userId, profile.netIncomeAfterTax) { mutableStateOf(profile.netIncomeAfterTax.toInput()) }
    var rent by remember(profile.userId, profile.rent) { mutableStateOf(profile.rent.toInput()) }
    var credits by remember(profile.userId, profile.credits) { mutableStateOf(profile.credits.toInput()) }
    var fixedCharges by remember(profile.userId, profile.fixedCharges) { mutableStateOf(profile.fixedCharges.toInput()) }
    var transport by remember(profile.userId, profile.transport) { mutableStateOf(profile.transport.toInput()) }
    var insurance by remember(profile.userId, profile.insurance) { mutableStateOf(profile.insurance.toInput()) }
    var other by remember(profile.userId, profile.otherMandatoryExpenses) { mutableStateOf(profile.otherMandatoryExpenses.toInput()) }
    var menstrual by remember(profile.userId, profile.menstrualProtection) { mutableStateOf(profile.menstrualProtection.toInput()) }
    var vegetarian by remember(profile.userId, profile.vegetarian) { mutableStateOf(profile.vegetarian) }
    var noAlcohol by remember(profile.userId, profile.noAlcohol) { mutableStateOf(profile.noAlcohol) }
    var ravPublic by remember(profile.userId, profile.livingRestPublic) { mutableStateOf(profile.livingRestPublic) }
    var constraints by remember(profile.userId, profile.customConstraints) { mutableStateOf(profile.customConstraints.joinToString(", ")) }
    var selectedLinkedIds by remember(profile.linkedPersons) { mutableStateOf(profile.linkedPersons.map { it.personId }.toSet()) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Profil RAV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Mode : ${profile.weightMode ?: "—"} · RAV : ${money(profile.livingRest, "EUR")}")
                    val tags = listOfNotNull(if (profile.vegetarian) "végétarien·ne" else null, if (profile.noAlcohol) "sans alcool" else null).plus(profile.customConstraints)
                    if (tags.isNotEmpty()) Text("Contraintes : ${tags.joinToString()}")
                }
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Fermer" else "Modifier") }
            }
            if (expanded) {
                OutlinedTextField(displayName, { displayName = it }, label = { Text("Nom affiché") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                SegmentedChoice("Mode de pondération", weightMode, listOf("AVERAGE" to "Moyenne", "LIVING_REST" to "RAV")) { weightMode = it }
                if (weightMode == "LIVING_REST") {
                    CheckRow("Calcul RAV avancé", advanced) { advanced = it }
                    if (advanced) {
                        MoneyField("Revenu net après impôt", income) { income = it }
                        MoneyField("Loyer", rent) { rent = it }
                        MoneyField("Crédits", credits) { credits = it }
                        MoneyField("Charges fixes", fixedCharges) { fixedCharges = it }
                        MoneyField("Transport", transport) { transport = it }
                        MoneyField("Assurance", insurance) { insurance = it }
                        MoneyField("Autres dépenses obligatoires", other) { other = it }
                        MoneyField("Protections menstruelles", menstrual) { menstrual = it }
                    } else {
                        MoneyField("RAV mensuel", livingRest) { livingRest = it }
                    }
                }
                CheckRow("Végétarien·ne", vegetarian) { vegetarian = it }
                CheckRow("Sans alcool", noAlcohol) { noAlcohol = it }
                CheckRow("RAV visible par les autres membres", ravPublic) { ravPublic = it }
                OutlinedTextField(constraints, { constraints = it }, label = { Text("Contraintes personnalisées, séparées par virgule") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        vm.updateProfile(
                            UserProfileRequest(
                                displayName = displayName.trim(),
                                weightMode = weightMode,
                                livingRest = if (weightMode == "AVERAGE" || advanced) 0.0 else decimalOrNull(livingRest),
                                advancedLivingRest = weightMode == "LIVING_REST" && advanced,
                                netIncomeAfterTax = if (advanced) decimalOrNull(income) else null,
                                rent = if (advanced) decimalOrNull(rent) else null,
                                credits = if (advanced) decimalOrNull(credits) else null,
                                fixedCharges = if (advanced) decimalOrNull(fixedCharges) else null,
                                transport = if (advanced) decimalOrNull(transport) else null,
                                insurance = if (advanced) decimalOrNull(insurance) else null,
                                otherMandatoryExpenses = if (advanced) decimalOrNull(other) else null,
                                menstrualProtection = if (advanced) decimalOrNull(menstrual) else null,
                                vegetarian = vegetarian,
                                noAlcohol = noAlcohol,
                                livingRestPublic = ravPublic,
                                customConstraints = parseCsvSet(constraints)
                            )
                        )
                    },
                    enabled = !vm.state.loading
                ) { Text("Enregistrer le profil") }
                if (profile.linkedPersons.isNotEmpty()) {
                    Divider()
                    Text("Appliquer ce profil aux personnes liées", fontWeight = FontWeight.Bold)
                    profile.linkedPersons.forEach { linked ->
                        CheckRow("${linked.personName} · ${linked.tripName}", selectedLinkedIds.contains(linked.personId)) { checked ->
                            selectedLinkedIds = if (checked) selectedLinkedIds + linked.personId else selectedLinkedIds - linked.personId
                        }
                    }
                    OutlinedButton(onClick = { vm.applyProfileToLinkedPersons(selectedLinkedIds) }, enabled = selectedLinkedIds.isNotEmpty() && !vm.state.loading) {
                        Text("Appliquer aux personnes sélectionnées")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateTripCard(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("Vacances ${LocalDateLabel.year()}") }
    var startDate by remember { mutableStateOf(todayIso()) }
    var endDate by remember { mutableStateOf(plusDaysIso(7)) }
    var currency by remember { mutableStateOf("EUR") }
    var constraints by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Créer un voyage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { expanded = !expanded }) { Icon(Icons.Default.Add, contentDescription = "Créer") }
            }
            if (expanded) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(startDate, { startDate = it }, label = { Text("Début") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(endDate, { endDate = it }, label = { Text("Fin") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(currency, { currency = it.take(3).uppercase() }, label = { Text("Devise") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(constraints, { constraints = it }, label = { Text("Contraintes personnalisées") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { vm.createTrip(name, startDate, endDate, currency, constraints) }, enabled = name.isNotBlank() && !vm.state.loading) {
                    Text("Créer")
                }
            }
        }
    }
}

@Composable
fun JoinTripCard(vm: MainViewModel) {
    var code by remember { mutableStateOf("") }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Rejoindre un voyage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(code, { code = it }, label = { Text("Code d’invitation") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(onClick = { vm.joinTripByCode(code) }, enabled = code.isNotBlank() && !vm.state.loading) {
                Text("Rejoindre")
            }
        }
    }
}

fun overviewContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    val state = vm.state
    item { CurrentUserClaimCard(vm) }
    item { TripConstraintsCard(vm) }
    item { DashboardStatsCard(state) }
    item { ExportCard(vm) }
}

fun personsContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    item { CurrentUserClaimCard(vm) }
    item { PersonFormCard(vm) }
    item { Text("Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (vm.state.persons.isEmpty()) {
        item { EmptyCard("Aucune personne dans ce voyage.") }
    } else {
        items(vm.state.persons, key = { it.id }) { person -> PersonCard(person, vm) }
    }
}

fun expensesContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    item { ExpenseFormCard(vm) }
    item { Text("Dépenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (vm.state.expenses.isEmpty()) {
        item { EmptyCard("Aucune dépense enregistrée.") }
    } else {
        items(vm.state.expenses, key = { it.id }) { expense -> ExpenseCard(expense, vm) }
    }
}

fun summaryContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    val state = vm.state
    val summary = state.summary
    item { ExportCard(vm) }
    if (summary == null) {
        item { EmptyCard("Résumé indisponible pour le moment.") }
    } else {
        item { Text("Soldes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (summary.balances.isEmpty()) {
            item { EmptyCard("Aucun solde calculable.") }
        } else {
            items(summary.balances, key = { it.personId }) { balance -> BalanceCard(balance, summary.referenceCurrency) }
        }
        item { Text("Remboursements suggérés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (summary.settlements.isEmpty()) {
            item { EmptyCard("Aucun remboursement nécessaire.") }
        } else {
            items(summary.settlements) { settlement -> SettlementCard(settlement, summary.referenceCurrency) }
        }
    }
}

fun invitationsContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    item { InvitationCreateCard(vm) }
    item { Text("Invitations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (vm.state.invitations.isEmpty()) {
        item { EmptyCard("Aucune invitation active ou historique non chargé.") }
    } else {
        items(vm.state.invitations, key = { it.id }) { invitation -> InvitationCard(invitation, vm) }
    }
}

fun auditContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    item { Button(onClick = vm::loadAuditLogs, enabled = !vm.state.loading) { Text("Recharger l’audit") } }
    if (vm.state.auditLogs.isEmpty()) {
        item { EmptyCard("Aucun journal d’audit chargé.") }
    } else {
        items(vm.state.auditLogs, key = { it.id }) { log -> AuditCard(log) }
    }
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

@Composable
fun PersonFormCard(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PersonResponse?>(null) }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (editing == null) "Ajouter une personne" else "Modifier ${editing?.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { expanded = !expanded; if (!expanded) editing = null }) { Text(if (expanded) "Fermer" else "Ajouter") }
            }
            if (expanded) {
                PersonEditor(person = editing, vm = vm, onDone = { expanded = false; editing = null })
            }
        }
    }
    if (vm.state.persons.isNotEmpty()) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Modification rapide", fontWeight = FontWeight.Bold)
                vm.state.persons.forEach { person ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(person.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editing = person; expanded = true }) { Text("Modifier") }
                    }
                }
            }
        }
    }
}

@Composable
fun PersonEditor(person: PersonResponse?, vm: MainViewModel, onDone: () -> Unit) {
    val trip = vm.state.selectedTrip ?: return
    var name by remember(person?.id) { mutableStateOf(person?.name.orEmpty()) }
    var weightMode by remember(person?.id) { mutableStateOf(person?.weightMode ?: "AVERAGE") }
    var livingRest by remember(person?.id) { mutableStateOf(person?.livingRest.toInput()) }
    var advanced by remember(person?.id) { mutableStateOf(person?.advancedLivingRest ?: false) }
    var income by remember(person?.id) { mutableStateOf(person?.netIncomeAfterTax.toInput()) }
    var rent by remember(person?.id) { mutableStateOf(person?.rent.toInput()) }
    var credits by remember(person?.id) { mutableStateOf(person?.credits.toInput()) }
    var fixedCharges by remember(person?.id) { mutableStateOf(person?.fixedCharges.toInput()) }
    var transport by remember(person?.id) { mutableStateOf(person?.transport.toInput()) }
    var insurance by remember(person?.id) { mutableStateOf(person?.insurance.toInput()) }
    var other by remember(person?.id) { mutableStateOf(person?.otherMandatoryExpenses.toInput()) }
    var menstrual by remember(person?.id) { mutableStateOf(person?.menstrualProtection.toInput()) }
    var vegetarian by remember(person?.id) { mutableStateOf(person?.vegetarian ?: false) }
    var noAlcohol by remember(person?.id) { mutableStateOf(person?.noAlcohol ?: false) }
    var ravPublic by remember(person?.id) { mutableStateOf(person?.livingRestPublic ?: true) }
    var active by remember(person?.id) { mutableStateOf(person?.active ?: true) }
    var constraints by remember(person?.id) { mutableStateOf(person?.customConstraints?.joinToString(", ").orEmpty()) }
    val firstPeriod = person?.presencePeriods?.firstOrNull()
    var presenceStart by remember(person?.id) { mutableStateOf(firstPeriod?.startDate ?: trip.startDate) }
    var presenceEnd by remember(person?.id) { mutableStateOf(firstPeriod?.endDate ?: trip.endDate) }
    var presenceStart2 by remember(person?.id) { mutableStateOf(person?.presencePeriods?.getOrNull(1)?.startDate.orEmpty()) }
    var presenceEnd2 by remember(person?.id) { mutableStateOf(person?.presencePeriods?.getOrNull(1)?.endDate.orEmpty()) }

    OutlinedTextField(name, { name = it }, label = { Text("Nom") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(presenceStart, { presenceStart = it }, label = { Text("Début présence") }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(presenceEnd, { presenceEnd = it }, label = { Text("Fin présence") }, modifier = Modifier.weight(1f), singleLine = true)
    }
    Text("Période optionnelle 2")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(presenceStart2, { presenceStart2 = it }, label = { Text("Début 2") }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(presenceEnd2, { presenceEnd2 = it }, label = { Text("Fin 2") }, modifier = Modifier.weight(1f), singleLine = true)
    }
    SegmentedChoice("Mode", weightMode, listOf("AVERAGE" to "Moyenne", "LIVING_REST" to "RAV")) { weightMode = it }
    if (weightMode == "LIVING_REST") {
        CheckRow("Calcul RAV avancé", advanced) { advanced = it }
        if (advanced) {
            MoneyField("Revenu net après impôt", income) { income = it }
            MoneyField("Loyer", rent) { rent = it }
            MoneyField("Crédits", credits) { credits = it }
            MoneyField("Charges fixes", fixedCharges) { fixedCharges = it }
            MoneyField("Transport", transport) { transport = it }
            MoneyField("Assurance", insurance) { insurance = it }
            MoneyField("Autres dépenses obligatoires", other) { other = it }
            MoneyField("Protections menstruelles", menstrual) { menstrual = it }
        } else {
            MoneyField("RAV mensuel", livingRest) { livingRest = it }
        }
    }
    CheckRow("Végétarien·ne", vegetarian) { vegetarian = it }
    CheckRow("Sans alcool", noAlcohol) { noAlcohol = it }
    CheckRow("RAV visible", ravPublic) { ravPublic = it }
    if (person != null) CheckRow("Active", active) { active = it }
    OutlinedTextField(constraints, { constraints = it }, label = { Text("Contraintes personnalisées") }, modifier = Modifier.fillMaxWidth())
    if (trip.customConstraints.isNotEmpty()) Text("Contraintes disponibles : ${trip.customConstraints.joinToString()}")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val periods = mutableListOf(PresencePeriodRequest(presenceStart.trim(), presenceEnd.trim()))
                if (presenceStart2.isNotBlank() && presenceEnd2.isNotBlank()) periods += PresencePeriodRequest(presenceStart2.trim(), presenceEnd2.trim())
                vm.savePerson(
                    person?.id,
                    PersonUpdateRequest(
                        name = name.trim(),
                        weightMode = weightMode,
                        livingRest = if (weightMode == "AVERAGE" || advanced) 0.0 else decimalOrNull(livingRest),
                        advancedLivingRest = weightMode == "LIVING_REST" && advanced,
                        netIncomeAfterTax = if (advanced) decimalOrNull(income) else null,
                        rent = if (advanced) decimalOrNull(rent) else null,
                        credits = if (advanced) decimalOrNull(credits) else null,
                        fixedCharges = if (advanced) decimalOrNull(fixedCharges) else null,
                        transport = if (advanced) decimalOrNull(transport) else null,
                        insurance = if (advanced) decimalOrNull(insurance) else null,
                        otherMandatoryExpenses = if (advanced) decimalOrNull(other) else null,
                        menstrualProtection = if (advanced) decimalOrNull(menstrual) else null,
                        vegetarian = vegetarian,
                        noAlcohol = noAlcohol,
                        livingRestPublic = ravPublic,
                        customConstraints = parseCsvSet(constraints),
                        active = active,
                        presencePeriods = periods
                    )
                )
                onDone()
            },
            enabled = name.isNotBlank() && !vm.state.loading
        ) { Text(if (person == null) "Créer" else "Enregistrer") }
        if (person != null && person.active) {
            OutlinedButton(onClick = { vm.disablePerson(person.id); onDone() }, enabled = !vm.state.loading) { Text("Désactiver") }
        }
    }
}

@Composable
fun ExpenseFormCard(vm: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExpenseResponse?>(null) }
    val trip = vm.state.selectedTrip
    val total = trip?.let { money(totalExpenses(vm.state), it.referenceCurrency) } ?: "—"

    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dépenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${vm.state.expenses.size} dépense(s) · total $total", style = MaterialTheme.typography.bodyMedium)
                    Text("Ajoute les dépenses au fil de l’eau. Les options RAV restent disponibles mais ne gênent pas le parcours simple.", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { expanded = true; editing = null }, enabled = !vm.state.loading) { Text("Ajouter") }
            }
        }
    }

    if (expanded) {
        ExpenseEditor(expense = editing, vm = vm, onDone = { expanded = false; editing = null })
    }

    if (vm.state.expenses.isNotEmpty()) {
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dépenses existantes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Touche une dépense pour la modifier.", style = MaterialTheme.typography.bodySmall)
                vm.state.expenses.forEach { expense ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { editing = expense; expanded = true }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(expense.title, fontWeight = FontWeight.Bold)
                            Text("${dateFr(expense.date)} · payé par ${expense.payerName ?: "—"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(money(expense.totalAmount, expense.currency), fontWeight = FontWeight.Bold)
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
fun ExpenseEditor(expense: ExpenseResponse?, vm: MainViewModel, onDone: () -> Unit) {
    val state = vm.state
    val trip = state.selectedTrip ?: return
    val activePersons = state.persons.filter { it.active }
    if (activePersons.isEmpty()) {
        EmptyCard("Ajoute d’abord au moins une personne active avant de créer une dépense.")
        return
    }

    var step by remember(expense?.id) { mutableStateOf(ExpenseStep.Essential) }
    var title by remember(expense?.id) { mutableStateOf(expense?.title.orEmpty()) }
    var date by remember(expense?.id) { mutableStateOf(expense?.date ?: trip.startDate) }
    var amount by remember(expense?.id) { mutableStateOf(expense?.totalAmount.toInput()) }
    var payerId by remember(expense?.id, activePersons.size) { mutableStateOf(expense?.payerPersonId ?: activePersons.first().id) }
    var type by remember(expense?.id) { mutableStateOf(expense?.type ?: "NORMAL") }
    var meat by remember(expense?.id) { mutableStateOf(expense?.meatAmount.toInput()) }
    var alcohol by remember(expense?.id) { mutableStateOf(expense?.alcoholAmount.toInput()) }
    var customAmounts by remember(expense?.id) { mutableStateOf(expense?.customConstraintAmounts?.entries?.joinToString(", ") { "${it.key}=${it.value}" }.orEmpty()) }
    var advanced by remember(expense?.id) { mutableStateOf(expense?.advancedMode ?: false) }
    var selectedParticipants by remember(expense?.id) { mutableStateOf(expense?.manualParticipantIds ?: activePersons.map { it.id }.toSet()) }
    var currency by remember(expense?.id) { mutableStateOf(expense?.currency ?: trip.referenceCurrency) }
    var exchangeRate by remember(expense?.id) { mutableStateOf(expense?.exchangeRateToTripCurrency?.toString() ?: "1.0") }

    val payerName = activePersons.firstOrNull { it.id == payerId }?.name ?: "à choisir"
    val amountValue = decimalOrNull(amount) ?: 0.0
    val participantCount = if (advanced) selectedParticipants.size else activePersons.size

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (expense == null) "Nouvelle dépense" else "Modifier la dépense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(step.hint, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onDone) { Text("Fermer") }
            }

            ExpenseStepSwitcher(step = step, onChange = { step = it })

            when (step) {
                ExpenseStep.Essential -> ExpenseEssentialStep(
                    title = title,
                    onTitle = { title = it },
                    date = date,
                    onDate = { date = it },
                    amount = amount,
                    onAmount = { amount = it },
                    type = type,
                    onType = { type = it },
                    currency = currency,
                    onCurrency = { currency = it.take(3).uppercase() },
                    exchangeRate = exchangeRate,
                    onExchangeRate = { exchangeRate = it },
                    tripCurrency = trip.referenceCurrency
                )
                ExpenseStep.Payer -> ExpensePayerStep(activePersons = activePersons, payerId = payerId, onPayer = { payerId = it })
                ExpenseStep.Split -> ExpenseSplitStep(
                    activePersons = activePersons,
                    advanced = advanced,
                    onAdvanced = { advanced = it },
                    selectedParticipants = selectedParticipants,
                    onSelected = { selectedParticipants = it }
                )
                ExpenseStep.Options -> ExpenseOptionsStep(
                    type = type,
                    meat = meat,
                    onMeat = { meat = it },
                    alcohol = alcohol,
                    onAlcohol = { alcohol = it },
                    customAmounts = customAmounts,
                    onCustomAmounts = { customAmounts = it },
                    trip = trip,
                    currency = currency
                )
            }

            ExpenseReviewPanel(
                title = title,
                amount = amountValue,
                currency = currency.ifBlank { trip.referenceCurrency }.uppercase(),
                payerName = payerName,
                participantCount = participantCount,
                type = type,
                tripCurrency = trip.referenceCurrency,
                exchangeRate = decimalOrNull(exchangeRate) ?: 1.0
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { step = previousExpenseStep(step) },
                    enabled = step != ExpenseStep.Essential,
                    modifier = Modifier.weight(1f)
                ) { Text("Retour") }
                if (step != ExpenseStep.Options) {
                    Button(onClick = { step = nextExpenseStep(step) }, modifier = Modifier.weight(1f)) { Text("Suivant") }
                } else {
                    Button(
                        onClick = {
                            vm.saveExpense(
                                expense?.id,
                                ExpenseCreateRequest(
                                    title = title.trim(),
                                    date = date.trim(),
                                    payerPersonId = payerId,
                                    totalAmount = amountValue,
                                    meatAmount = if (type == "GLOBAL") 0.0 else decimalOrNull(meat) ?: 0.0,
                                    alcoholAmount = if (type == "GLOBAL") 0.0 else decimalOrNull(alcohol) ?: 0.0,
                                    customConstraintAmounts = if (type == "GLOBAL") emptyMap() else parseCustomAmountMap(customAmounts),
                                    type = type,
                                    advancedMode = advanced,
                                    manualParticipantIds = if (advanced) selectedParticipants else emptySet(),
                                    currency = currency.ifBlank { trip.referenceCurrency }.uppercase(),
                                    exchangeRateToTripCurrency = decimalOrNull(exchangeRate) ?: 1.0
                                )
                            )
                            onDone()
                        },
                        enabled = title.isNotBlank() && amountValue > 0.0 && payerId.isNotBlank() && participantCount > 0 && !state.loading,
                        modifier = Modifier.weight(1f)
                    ) { Text(if (expense == null) "Ajouter" else "Enregistrer") }
                }
            }

            if (expense != null) {
                OutlinedButton(onClick = { vm.deleteExpense(expense.id); onDone() }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text("Supprimer cette dépense")
                }
            }
        }
    }
}

@Composable
fun ExpenseStepSwitcher(step: ExpenseStep, onChange: (ExpenseStep) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseStepButton(ExpenseStep.Essential, step, onChange, Modifier.weight(1f))
            ExpenseStepButton(ExpenseStep.Payer, step, onChange, Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExpenseStepButton(ExpenseStep.Split, step, onChange, Modifier.weight(1f))
            ExpenseStepButton(ExpenseStep.Options, step, onChange, Modifier.weight(1f))
        }
    }
}

@Composable
fun ExpenseStepButton(target: ExpenseStep, selected: ExpenseStep, onChange: (ExpenseStep) -> Unit, modifier: Modifier = Modifier) {
    if (target == selected) {
        Button(onClick = { onChange(target) }, modifier = modifier) { Text(target.label) }
    } else {
        OutlinedButton(onClick = { onChange(target) }, modifier = modifier) { Text(target.label) }
    }
}

@Composable
fun ExpenseEssentialStep(
    title: String,
    onTitle: (String) -> Unit,
    date: String,
    onDate: (String) -> Unit,
    amount: String,
    onAmount: (String) -> Unit,
    type: String,
    onType: (String) -> Unit,
    currency: String,
    onCurrency: (String) -> Unit,
    exchangeRate: String,
    onExchangeRate: (String) -> Unit,
    tripCurrency: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Informations principales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(title, onTitle, label = { Text("Libellé") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(date, onDate, label = { Text("Date") }, modifier = Modifier.weight(1f), singleLine = true)
            MoneyField("Montant", amount, Modifier.weight(1f), onAmount)
        }
        SegmentedChoice("Type de dépense", type, listOf("NORMAL" to "Normale", "GLOBAL" to "Globale"), onType)
        Text("Une dépense globale ignore les parts viande/alcool/contraintes. Une dépense normale peut utiliser les règles RAV.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(currency, onCurrency, label = { Text("Devise") }, modifier = Modifier.weight(1f), singleLine = true)
            MoneyField("Taux vers $tripCurrency", exchangeRate, Modifier.weight(1f), onExchangeRate)
        }
    }
}

@Composable
fun ExpensePayerStep(activePersons: List<PersonResponse>, payerId: String, onPayer: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Payeur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Sélectionne la personne qui a payé la dépense.", style = MaterialTheme.typography.bodySmall)
        activePersons.forEach { person ->
            PersonSelectableRow(
                label = person.name,
                detail = if (person.guest) "Guest" else "Compte lié",
                checked = payerId == person.id,
                onChange = { onPayer(person.id) }
            )
        }
    }
}

@Composable
fun ExpenseSplitStep(
    activePersons: List<PersonResponse>,
    advanced: Boolean,
    onAdvanced: (Boolean) -> Unit,
    selectedParticipants: Set<String>,
    onSelected: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        CheckRow("Répartition manuelle", advanced, onAdvanced)
        if (!advanced) {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text("Par défaut, la dépense concerne toutes les personnes actives du voyage.", modifier = Modifier.padding(12.dp))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSelected(activePersons.map { it.id }.toSet()) }, modifier = Modifier.weight(1f)) { Text("Tout") }
                OutlinedButton(onClick = { onSelected(emptySet()) }, modifier = Modifier.weight(1f)) { Text("Aucun") }
            }
            activePersons.forEach { person ->
                PersonSelectableRow(
                    label = person.name,
                    detail = person.presencePeriods.joinToString(" · ") { "${dateFr(it.startDate)} → ${dateFr(it.endDate)}" },
                    checked = selectedParticipants.contains(person.id),
                    onChange = {
                        onSelected(if (selectedParticipants.contains(person.id)) selectedParticipants - person.id else selectedParticipants + person.id)
                    }
                )
            }
        }
    }
}

@Composable
fun ExpenseOptionsStep(
    type: String,
    meat: String,
    onMeat: (String) -> Unit,
    alcohol: String,
    onAlcohol: (String) -> Unit,
    customAmounts: String,
    onCustomAmounts: (String) -> Unit,
    trip: TripResponse,
    currency: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Options RAV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (type == "GLOBAL") {
            EmptyCard("Cette dépense est globale : aucune ventilation viande, alcool ou contrainte personnalisée ne sera appliquée.")
        } else {
            Text("Ces champs sont facultatifs. Laisse à 0 si la dépense doit être partagée normalement.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField("Part viande", meat, Modifier.weight(1f), onMeat)
                MoneyField("Part alcool", alcohol, Modifier.weight(1f), onAlcohol)
            }
            OutlinedTextField(customAmounts, onCustomAmounts, label = { Text("Contraintes : Vegan=12, PMR=5") }, modifier = Modifier.fillMaxWidth())
            if (trip.customConstraints.isNotEmpty()) {
                Text("Contraintes disponibles : ${trip.customConstraints.joinToString()} · devise : ${currency.ifBlank { trip.referenceCurrency }}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ExpenseReviewPanel(
    title: String,
    amount: Double,
    currency: String,
    payerName: String,
    participantCount: Int,
    type: String,
    tripCurrency: String,
    exchangeRate: Double
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Aperçu", fontWeight = FontWeight.Bold)
            Text(if (title.isBlank()) "Libellé à renseigner" else title)
            Text("${money(amount, currency)} · payé par $payerName · $participantCount participant(s)")
            if (currency != tripCurrency) Text("Converti vers $tripCurrency avec le taux $exchangeRate")
            Text(if (type == "GLOBAL") "Dépense globale" else "Dépense normale avec options RAV possibles")
        }
    }
}

@Composable
fun PersonSelectableRow(label: String, detail: String, checked: Boolean, onChange: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable { onChange() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { onChange() })
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.Bold)
                if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun nextExpenseStep(step: ExpenseStep): ExpenseStep = when (step) {
    ExpenseStep.Essential -> ExpenseStep.Payer
    ExpenseStep.Payer -> ExpenseStep.Split
    ExpenseStep.Split -> ExpenseStep.Options
    ExpenseStep.Options -> ExpenseStep.Options
}

fun previousExpenseStep(step: ExpenseStep): ExpenseStep = when (step) {
    ExpenseStep.Essential -> ExpenseStep.Essential
    ExpenseStep.Payer -> ExpenseStep.Essential
    ExpenseStep.Split -> ExpenseStep.Payer
    ExpenseStep.Options -> ExpenseStep.Split
}

@Composable
fun PersonCard(person: PersonResponse, vm: MainViewModel) {
    val isLinkedToCurrent = person.linkedUserId == vm.state.auth?.userId
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (person.active) "Active" else "Inactive")
                    if (!person.linkedUserEmail.isNullOrBlank()) Text(person.linkedUserEmail)
                }
                AssistChip(onClick = {}, label = { Text(if (person.guest) "Guest" else "Compte") })
            }
            Text("RAV : ${if (person.livingRestHidden) "masqué" else money(person.livingRest, "EUR")} · Mode : ${person.weightMode ?: "—"}")
            Text("Présence : ${person.presencePeriods.joinToString(" · ") { "${dateFr(it.startDate)} → ${dateFr(it.endDate)}" }}")
            val tags = listOfNotNull(if (person.vegetarian) "végétarien·ne" else null, if (person.noAlcohol) "sans alcool" else null).plus(person.customConstraints)
            if (tags.isNotEmpty()) Text("Contraintes : ${tags.joinToString()}")
            if (isLinkedToCurrent) Text("Lié à ton compte", fontWeight = FontWeight.Bold)
            if (person.guest && !isLinkedToCurrent) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { vm.linkGuest(person.id, applyProfile = true) }) { Text("Me rattacher + profil") }
                    TextButton(onClick = { vm.linkGuest(person.id, applyProfile = false) }) { Text("Me rattacher") }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: ExpenseResponse, vm: MainViewModel) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(money(expense.totalAmount, expense.currency), fontWeight = FontWeight.Bold)
            }
            Text("${dateFr(expense.date)} · payé par ${expense.payerName ?: "—"} · ${expense.type}")
            if (expense.currency != vm.state.selectedTrip?.referenceCurrency) {
                Text("Taux vers devise du voyage : ${expense.exchangeRateToTripCurrency}")
            }
            if (expense.meatAmount > 0 || expense.alcoholAmount > 0) {
                Text("Viande ${money(expense.meatAmount, expense.currency)} · Alcool ${money(expense.alcoholAmount, expense.currency)}")
            }
            if (expense.customConstraintAmounts.isNotEmpty()) {
                Text("Contraintes : ${expense.customConstraintAmounts.entries.joinToString { "${it.key} ${money(it.value, expense.currency)}" }}")
            }
            if (expense.advancedMode) Text("Participants manuels : ${expense.manualParticipantIds.size}")
        }
    }
}

@Composable
fun BalanceCard(balance: BalanceResponse, currency: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(balance.personName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Payé : ${money(balance.totalPaid, currency)} · Dû : ${money(balance.totalOwed, currency)}")
            Text("Solde : ${money(balance.balance, currency)}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettlementCard(settlement: SettlementResponse, currency: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "${settlement.fromPersonName} rembourse ${money(settlement.amount, currency)} à ${settlement.toPersonName}",
            modifier = Modifier.padding(14.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InvitationCreateCard(vm: MainViewModel) {
    var days by remember { mutableStateOf("7") }
    var role by remember { mutableStateOf("PARTICIPANT") }
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Créer une invitation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SegmentedChoice("Rôle", role, listOf("PARTICIPANT" to "Participant", "ADMIN" to "Admin", "READ_ONLY" to "Lecture", "OWNER" to "Owner")) { role = it }
            OutlinedTextField(days, { days = it }, label = { Text("Expiration en jours, 1 à 30") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Button(onClick = { vm.createInvitation(role, days) }, enabled = !vm.state.loading) { Text("Créer le code") }
        }
    }
}

@Composable
fun InvitationCard(invitation: TripInvitationResponse, vm: MainViewModel) {
    val clipboard = LocalClipboardManager.current
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(invitation.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Rôle : ${invitation.roleToGrant} · ${if (invitation.usable) "utilisable" else "non utilisable"}")
                    Text("Expire : ${invitation.expiresAt ?: "—"}")
                    if (invitation.revoked) Text("Révoquée")
                }
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { clipboard.setText(AnnotatedString(invitation.code)) }) { Text("Copier") }
                    if (!invitation.revoked) TextButton(onClick = { vm.revokeInvitation(invitation.id) }) { Text("Révoquer") }
                }
            }
        }
    }
}

@Composable
fun AuditCard(log: AuditLogResponse) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(log.action, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(log.description)
            Text("${log.entityType} · ${log.createdAt ?: "—"}")
        }
    }
}

@Composable
fun ExportCard(vm: MainViewModel) {
    val preview = vm.state.exportPreview
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Exports CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = vm::exportExpensesCsv, enabled = !vm.state.loading) { Text("Dépenses CSV") }
                OutlinedButton(onClick = vm::exportSummaryCsv, enabled = !vm.state.loading) { Text("Résumé CSV") }
            }
            if (preview != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(preview.title, fontWeight = FontWeight.Bold)
                    TextButton(onClick = vm::clearExportPreview) { Text("Fermer") }
                }
                SelectionContainer {
                    Text(preview.content.take(4000))
                }
            }
        }
    }
}

