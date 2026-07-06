package fr.achabitation.mobile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun EmptyCard(text: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(text, modifier = Modifier.padding(14.dp))
    }
}

@Composable
fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onChange(!checked) }) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
fun MoneyField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
fun SegmentedChoice(label: String, value: String, options: List<Pair<String, String>>, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (id, text) ->
                if (value == id) Button(onClick = { onChange(id) }) { Text(text) } else OutlinedButton(onClick = { onChange(id) }) { Text(text) }
            }
        }
    }
}

fun money(value: Double?, currency: String): String {
    if (value == null) return "—"
    return try {
        NumberFormat.getCurrencyInstance(Locale.FRANCE).apply {
            this.currency = java.util.Currency.getInstance(currency)
        }.format(value)
    } catch (_: Exception) {
        "%.2f %s".format(Locale.FRANCE, value, currency)
    }
}

fun dateFr(value: String): String {
    val parts = value.split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else value
}

fun decimalOrNull(value: String): Double? = value.trim().replace(',', '.').toDoubleOrNull()

fun parseCustomAmountMap(value: String): Map<String, Double> = value.split(',', ';', '\n')
    .mapNotNull { raw ->
        val parts = raw.split('=', ':', limit = 2)
        if (parts.size != 2) null else {
            val key = parts[0].trim()
            val amount = decimalOrNull(parts[1])
            if (key.isBlank() || amount == null) null else key to amount
        }
    }
    .toMap()

fun totalExpenses(state: AppUiState): Double = state.expenses.sumOf { it.totalAmount * it.exchangeRateToTripCurrency }

object LocalDateLabel {
    fun year(): Int = java.time.LocalDate.now().year
}
