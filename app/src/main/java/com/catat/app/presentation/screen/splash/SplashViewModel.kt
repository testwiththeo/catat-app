package com.catat.app.presentation.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catat.app.data.datastore.AppPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesDataStore: AppPreferencesDataStore
) : ViewModel() {

    enum class SplashDestination { Loading, Onboarding, History }

    private val _destination = MutableStateFlow(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = preferencesDataStore.preferences.first()
            _destination.value = if (prefs.onboardingCompleted) {
                SplashDestination.History
            } else {
                SplashDestination.Onboarding
            }
        }
    }
}
