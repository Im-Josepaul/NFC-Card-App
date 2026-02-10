package com.josepaul.nfccardapp.presentation
import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val CURRENT_TIMESTAMP = longPreferencesKey("current_timestamp")
private val CHECKOUT_TIMESTAMP = longPreferencesKey("checkout_timestamp")
private val CHECKIN_TIMESTAMP = longPreferencesKey("checkin_timestamp")
private val CURRENT_STATUS = intPreferencesKey("current_status")

class AppRepository(private val context: Context) {
    val currentTimestamp: Flow<Long> = context.dataStore.data.map {
        it[CURRENT_TIMESTAMP] ?: 0L
    }

    val checkoutTimestamp: Flow<Long> = context.dataStore.data.map {
        it[CHECKOUT_TIMESTAMP] ?: 0L
    }

//    val checkinTimestamp: Flow<Long> = context.dataStore.data.map {
//        it[CHECKIN_TIMESTAMP] ?: 0L
//    }

    val currentStatus: Flow<Int> = context.dataStore.data.map {
        it[CURRENT_STATUS] ?: 0
    }

    //Value editing functions
    suspend fun saveCurrentTimestamp(time: Long) {
        context.dataStore.edit { settings ->
            settings[CURRENT_TIMESTAMP] = time
        }
    }
    suspend fun saveCheckoutTimestamp(time: Long) {
        context.dataStore.edit { settings ->
            settings[CHECKOUT_TIMESTAMP] = time
        }
    }
    suspend fun saveCheckinTimestamp(time: Long) {
        context.dataStore.edit { settings ->
            settings[CHECKIN_TIMESTAMP] = time
        }
    }
    suspend fun saveCurrentStatus(status: Int) {
        context.dataStore.edit { settings ->
            settings[CURRENT_STATUS] = status
        }
    }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppRepository(application)
    val checkoutTimestamp = repo.checkoutTimestamp.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = 0L
    )
    val currentTimestamp = repo.currentTimestamp.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = 0L
    )
    val currentStatus = repo.currentStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = 0
    )

    fun performAction(){
        viewModelScope.launch {
            if (currentStatus.value == 0) {
                val now = System.currentTimeMillis()
                repo.saveCheckinTimestamp(now)
                repo.saveCheckoutTimestamp(now + 28_800_000L)
                repo.saveCurrentStatus(currentStatus.value + 1)
                return@launch
            } else {
                if (System.currentTimeMillis() > checkoutTimestamp.value) {
                    repo.saveCurrentStatus(0)
                    repo.saveCheckinTimestamp(0L)
                    repo.saveCheckoutTimestamp(0L)
                    repo.saveCurrentTimestamp(0L)
                    return@launch
                } else {
                    if (currentStatus.value % 2 != 0) {
                        repo.saveCurrentTimestamp(System.currentTimeMillis())
                        repo.saveCurrentStatus(currentStatus.value + 1)
                        return@launch
                    } else {
                        val difference = kotlin.math.max(
                            0L,
                            System.currentTimeMillis() - currentTimestamp.value
                        )
                        repo.saveCheckoutTimestamp(checkoutTimestamp.value + difference)
                        repo.saveCurrentStatus(currentStatus.value + 1)
                        return@launch
                    }
                }
            }
        }
    }
}

