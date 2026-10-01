package com.example.srchicken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.srchicken.data.AppRepository
import com.example.srchicken.data.BillEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class HistoryUiState(
    val allBills: List<BillEntity> = emptyList(),
    val searchQuery: String = "",
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val showDeleteConfirm: Long? = null
)

class HistoryViewModel(private val repo: AppRepository) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    val filteredBills: StateFlow<List<BillEntity>> = combine(
        repo.getAllBills(),
        _state
    ) { bills, state ->
        var filtered = bills
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.billNumber.lowercase().contains(q) ||
                it.customerName.lowercase().contains(q)
            }
        }
        state.fromDate?.let { from ->
            val startOfDay = Calendar.getInstance().apply {
                timeInMillis = from
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            filtered = filtered.filter { it.createdAt >= startOfDay }
        }
        state.toDate?.let { to ->
            val endOfDay = Calendar.getInstance().apply {
                timeInMillis = to
                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
            }.timeInMillis
            filtered = filtered.filter { it.createdAt <= endOfDay }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repo.getAllBills().collect { bills ->
                _state.update { it.copy(allBills = bills) }
            }
        }
    }

    fun setSearchQuery(q: String) = _state.update { it.copy(searchQuery = q) }
    fun setFromDate(ms: Long?) = _state.update { it.copy(fromDate = ms) }
    fun setToDate(ms: Long?) = _state.update { it.copy(toDate = ms) }
    fun clearFilters() = _state.update { it.copy(searchQuery = "", fromDate = null, toDate = null) }
    fun confirmDelete(id: Long) = _state.update { it.copy(showDeleteConfirm = id) }
    fun cancelDelete() = _state.update { it.copy(showDeleteConfirm = null) }

    fun deleteBill(id: Long) {
        viewModelScope.launch {
            repo.deleteBill(id)
            _state.update { it.copy(showDeleteConfirm = null) }
        }
    }

    fun updateBillDate(id: Long, date: Long) {
        viewModelScope.launch { repo.updateBillDate(id, date) }
    }

    suspend fun getBillItems(billId: Long) = repo.getItemsForBill(billId)
    suspend fun getBillById(id: Long) = repo.getBillById(id)
}
