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
