package fr.achabitation.mobile

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stockage local chiffré pour les éléments sensibles de session.
 * EncryptedSharedPreferences est demandé pour cette V1 mobile; l'API AndroidX est dépréciée,
 * mais reste disponible en security-crypto 1.1.0. Une migration future vers DataStore + Keystore
 * pourra être faite sans changer le contrat public du ViewModel.
 */
@Suppress("DEPRECATION")
object SecurePreferences {
    fun open(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "achabitation-mobile-secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
