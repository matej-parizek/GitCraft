package cz.parizmat.gitcraft.feature.sidebar.ui

import cz.parizmat.gitcraft.feature.sidebar.domain.SideBarUiState
import cz.parizmat.gitcraft.feature.sidebar.domain.enums.Destinations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SidebarModelView {
    private val _state = MutableStateFlow(SideBarUiState())
    val state: StateFlow<SideBarUiState> = _state.asStateFlow()
    fun selectDestination(destination: SideBarUiState) {
        _state.value = destination
    }

    fun selectDestination(destination: Destinations) {
        _state.value = SideBarUiState(currentDestination = destination)
    }
}