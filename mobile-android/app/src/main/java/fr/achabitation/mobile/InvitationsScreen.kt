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

fun invitationsContent(vm: MainViewModel, scope: LazyListScope) = with(scope) {
    item { InvitationCreateCard(vm) }
    item { Text("Invitations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (vm.state.invitations.isEmpty()) {
        item { EmptyCard("Aucune invitation active ou historique non chargé.") }
    } else {
        items(vm.state.invitations, key = { it.id }) { invitation -> InvitationCard(invitation, vm) }
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
    var invitationToRevoke by remember { mutableStateOf<TripInvitationResponse?>(null) }
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
                    if (!invitation.revoked) TextButton(onClick = { invitationToRevoke = invitation }) { Text("Révoquer") }
                }
            }
        }
    }

    invitationToRevoke?.let { invitation ->
        ConfirmDialog(
            title = "Révoquer l’invitation ?",
            message = "Le code ${invitation.code} ne pourra plus être utilisé.",
            confirmLabel = "Révoquer",
            onConfirm = {
                invitationToRevoke = null
                vm.revokeInvitation(invitation.id)
            },
            onDismiss = { invitationToRevoke = null }
        )
    }
}
