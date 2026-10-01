package com.example.srchicken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.srchicken.data.AppRepository
import com.example.srchicken.data.CustomerEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomersUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val showAddDialog: Boolean = false,
    val editingCustomer: CustomerEntity? = null,
    val nameInput: String = "",
    val phoneInput: String = "",
    val customerCodeInput: String = "",
    val openingBalanceInput: String = "0",
    val inputError: String? = null
)

class CustomersViewModel(private val repo: AppRepository) : ViewModel() {

    private val _state = MutableStateFlow(CustomersUiState())
    val state: StateFlow<CustomersUiState> = _state.asStateFlow()

    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(
        repo.getAllCustomers(),
        _state.map { it.searchQuery }
    ) { customers, query ->
        if (query.isBlank()) customers
        else customers.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.phone.contains(query, ignoreCase = true)
                || it.customerCode.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repo.getAllCustomers().collect { customers ->
                _state.update { it.copy(customers = customers) }
            }
        }
    }

    fun setSearchQuery(query: String) = _state.update { it.copy(searchQuery = query) }

    fun openAddDialog() = _state.update { it.copy(
        showAddDialog = true,
        editingCustomer = null,
        nameInput = "",
        phoneInput = "",
        customerCodeInput = "",
        openingBalanceInput = "0",
        inputError = null
    )}

    fun openEditDialog(customer: CustomerEntity) = _state.update { it.copy(
        showAddDialog = true,
        editingCustomer = customer,
        nameInput = customer.name,
        phoneInput = customer.phone,
        customerCodeInput = customer.customerCode,
        openingBalanceInput = customer.openingBalance.toString(),
        inputError = null
    )}

    fun closeDialog() = _state.update { it.copy(showAddDialog = false, inputError = null) }

    fun updateName(name: String) = _state.update { it.copy(nameInput = name, inputError = null) }
    fun updatePhone(phone: String) = _state.update { it.copy(phoneInput = phone) }
    fun updateCustomerCode(code: String) = _state.update { it.copy(customerCodeInput = code) }
    fun updateOpeningBalance(balance: String) = _state.update { it.copy(openingBalanceInput = balance) }

    fun saveCustomer() {
        val state = _state.value
        val name = state.nameInput.trim()
        val openingBalance = state.openingBalanceInput.toDoubleOrNull()
        if (name.isBlank()) {
            _state.update { it.copy(inputError = "Name is required") }
            return
        }
        if (openingBalance == null || openingBalance < 0) {
            _state.update { it.copy(inputError = "Opening balance must be a valid positive amount") }
            return
        }
        viewModelScope.launch {
            if (state.editingCustomer != null) {
                repo.updateCustomer(state.editingCustomer.id, name, state.phoneInput.trim(), state.customerCodeInput.trim(), openingBalance)
            } else {
                repo.addCustomer(name, state.phoneInput.trim(), state.customerCodeInput.trim(), openingBalance)
            }
            closeDialog()
        }
    }

    fun deleteCustomer(id: Long) {
        viewModelScope.launch { repo.deleteCustomer(id) }
    }
}
