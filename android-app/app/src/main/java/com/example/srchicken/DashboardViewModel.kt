package com.example.srchicken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.srchicken.data.AppRepository
import com.example.srchicken.data.BillEntity
import com.example.srchicken.data.CustomerEntity
import com.example.srchicken.data.DashboardStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val stats: DashboardStats = DashboardStats(),
    val recentBills: List<BillEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList()
)

class DashboardViewModel(private val repo: AppRepository) : ViewModel() {

    private val _customers = repo.getAllCustomers().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _recentBills = repo.getRecentBills().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _customerCount = _customers.map { it.size }

    private val _stats = repo.getDashboardStats(_customerCount).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats()
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        _stats, _recentBills, _customers
    ) { stats, recent, customers ->
        DashboardUiState(stats, recent, customers)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
