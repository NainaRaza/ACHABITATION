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
