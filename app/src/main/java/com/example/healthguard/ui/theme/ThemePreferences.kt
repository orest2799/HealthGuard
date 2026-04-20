import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object ThemePreferences {
    private val THEME_KEY = booleanPreferencesKey("dark_theme_enabled")


    suspend fun saveTheme(dataStore: DataStore<Preferences>, isDark: Boolean) {
        dataStore.edit { prefs ->
            prefs[THEME_KEY] = isDark
        }
    }

    fun getTheme(dataStore: DataStore<Preferences>): Flow<Boolean> {
        return dataStore.data.map { prefs ->
            prefs[THEME_KEY] ?: false
        }
    }
}