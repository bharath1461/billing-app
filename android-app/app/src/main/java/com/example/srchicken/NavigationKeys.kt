package com.example.srchicken

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

// ---- Route definitions ----
@Serializable object DashboardRoute
@Serializable object NewBillRoute
@Serializable object CustomersRoute
@Serializable object HistoryRoute
@Serializable object SettingsRoute
@Serializable data class BillDetailRoute(val billId: Long)

// ---- Bottom nav items ----
data class BottomNavItem(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(DashboardRoute, "Home",      Icons.Outlined.Home,        Icons.Filled.Home),
    BottomNavItem(NewBillRoute,  "New Bill",   Icons.Outlined.AddCircle,   Icons.Filled.AddCircle),
    BottomNavItem(CustomersRoute,"Customers",  Icons.Outlined.Group,       Icons.Filled.Group),
    BottomNavItem(HistoryRoute,  "History",    Icons.Outlined.Receipt,     Icons.Filled.Receipt),
)
