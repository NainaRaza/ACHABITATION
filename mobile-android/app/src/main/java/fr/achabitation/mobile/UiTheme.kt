package fr.achabitation.mobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AchabitationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2457C5),
            secondary = Color(0xFF49617E),
            tertiary = Color(0xFF00796B),
            surface = Color(0xFFFAFBFF),
            background = Color(0xFFF5F7FC)
        ),
        content = content
    )
}

enum class HomeSection(val label: String, val description: String) {
    Trips("Voyages", "Créer, rejoindre et ouvrir un voyage"),
    Create("Créer", "Démarrer un nouveau voyage"),
    Join("Rejoindre", "Utiliser un code d’invitation"),
    Profile("Profil RAV", "Préférences et reste à vivre"),
    Account("Compte", "Identité, email et déconnexion")
}

enum class ExpenseStep(val label: String, val hint: String) {
    Essential("1. Base", "Libellé, date, montant et devise"),
    Payer("2. Payeur", "Qui a avancé l’argent"),
    Split("3. Participants", "Toutes les personnes ou une sélection"),
    Options("4. Options", "Viande, alcool, contraintes et validation")
}
