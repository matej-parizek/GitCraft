package cz.parizmat.gitcraft.feature.sidebar.domain

import cz.parizmat.gitcraft.feature.sidebar.domain.enums.Destinations

data class SideBarUiState(
    val currentDestination: Destinations = Destinations.CHANGES
)
