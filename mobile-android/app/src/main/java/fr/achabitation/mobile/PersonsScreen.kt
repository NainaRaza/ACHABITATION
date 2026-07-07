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
    var selectedConstraints by remember(person?.id, trip.customConstraints) { mutableStateOf(person?.customConstraints.orEmpty()) }
    val firstPeriod = person?.presencePeriods?.firstOrNull()
    var presenceStart by remember(person?.id) { mutableStateOf(firstPeriod?.startDate ?: trip.startDate) }
    var presenceEnd by remember(person?.id) { mutableStateOf(firstPeriod?.endDate ?: trip.endDate) }
    var presenceStart2 by remember(person?.id) { mutableStateOf(person?.presencePeriods?.getOrNull(1)?.startDate.orEmpty()) }
    var presenceEnd2 by remember(person?.id) { mutableStateOf(person?.presencePeriods?.getOrNull(1)?.endDate.orEmpty()) }
    var confirmDisable by remember(person?.id) { mutableStateOf(false) }

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
    TripConstraintCheckboxes(
        availableConstraints = trip.customConstraints,
        selectedConstraints = selectedConstraints,
        onSelectedConstraintsChange = { selectedConstraints = it }
    )
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
                        customConstraints = selectedConstraints,
                        active = active,
                        presencePeriods = periods
                    )
                )
                onDone()
            },
            enabled = name.isNotBlank() && !vm.state.loading
        ) { Text(if (person == null) "Créer" else "Enregistrer") }
        if (person != null && person.active) {
            OutlinedButton(onClick = { confirmDisable = true }, enabled = !vm.state.loading) { Text("Désactiver") }
        }
    }

    if (confirmDisable && person != null) {
        ConfirmDialog(
            title = "Désactiver cette personne ?",
            message = "${person.name} ne sera plus actif dans le voyage. Les données existantes restent conservées.",
            confirmLabel = "Désactiver",
            onConfirm = {
                confirmDisable = false
                vm.disablePerson(person.id)
                onDone()
            },
            onDismiss = { confirmDisable = false }
        )
    }
}

@Composable
fun TripConstraintCheckboxes(
    availableConstraints: Set<String>,
    selectedConstraints: Set<String>,
    onSelectedConstraintsChange: (Set<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Contraintes personnalisées", fontWeight = FontWeight.Bold)
        if (availableConstraints.isEmpty()) {
            EmptyCard("Aucune contrainte personnalisée n’est encore déclarée sur ce voyage. Ajoute-les dans l’écran Voyage pour pouvoir les cocher ici.")
        } else {
            Text("Coche les contraintes qui concernent cette personne.", style = MaterialTheme.typography.bodySmall)
            availableConstraints.toList().sortedWith(String.CASE_INSENSITIVE_ORDER).forEach { name ->
                val checked = selectedConstraints.any { it.equals(name, ignoreCase = true) }
                CheckRow(name, checked) {
                    onSelectedConstraintsChange(if (checked) selectedConstraints.filterNot { it.equals(name, ignoreCase = true) }.toSet() else selectedConstraints + name)
                }
            }
        }
    }
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
