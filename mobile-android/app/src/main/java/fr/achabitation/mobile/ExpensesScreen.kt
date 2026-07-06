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

fun expensesContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    item { ExpenseFormCard(vm) }
    item { Text("Dépenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (vm.state.expenses.isEmpty()) {
        item { EmptyCard("Aucune dépense enregistrée.") }
    } else {
        items(vm.state.expenses, key = { it.id }) { expense -> ExpenseCard(expense, vm) }
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
    var confirmDelete by remember(expense?.id) { mutableStateOf(false) }

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
                OutlinedButton(onClick = { confirmDelete = true }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                    Text("Supprimer cette dépense")
                }
            }
        }
    }

    if (confirmDelete && expense != null) {
        ConfirmDialog(
            title = "Supprimer cette dépense ?",
            message = "Cette action supprimera définitivement ${expense.title} du voyage.",
            confirmLabel = "Supprimer",
            onConfirm = {
                confirmDelete = false
                vm.deleteExpense(expense.id)
                onDone()
            },
            onDismiss = { confirmDelete = false }
        )
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
