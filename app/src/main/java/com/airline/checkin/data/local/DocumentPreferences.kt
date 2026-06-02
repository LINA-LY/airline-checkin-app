package com.airline.checkin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.docDataStore by preferencesDataStore(name = "saved_docs")

data class SavedPassport(
    val number: String,
    val firstName: String,
    val lastName: String,
    val dob: String,
    val nationality: String,
    val gender: String
)

@Singleton
class DocumentPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val numKey = stringPreferencesKey("doc_num")
    private val fnKey = stringPreferencesKey("doc_fn")
    private val lnKey = stringPreferencesKey("doc_ln")
    private val dobKey = stringPreferencesKey("doc_dob")
    private val natKey = stringPreferencesKey("doc_nat")
    private val genKey = stringPreferencesKey("doc_gen")

    val savedPassport: Flow<SavedPassport?> = context.docDataStore.data.map { prefs ->
        val num = prefs[numKey] ?: return@map null
        SavedPassport(
            number = num,
            firstName = prefs[fnKey] ?: "",
            lastName = prefs[lnKey] ?: "",
            dob = prefs[dobKey] ?: "",
            nationality = prefs[natKey] ?: "",
            gender = prefs[genKey] ?: ""
        )
    }

    suspend fun savePassport(num: String, fn: String, ln: String, dob: String, nat: String, gen: String) {
        context.docDataStore.edit { prefs ->
            prefs[numKey] = num
            prefs[fnKey] = fn
            prefs[lnKey] = ln
            prefs[dobKey] = dob
            prefs[natKey] = nat
            prefs[genKey] = gen
        }
    }

    suspend fun clear() {
        context.docDataStore.edit { it.clear() }
    }
}