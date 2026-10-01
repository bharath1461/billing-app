package com.example.srchicken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.srchicken.data.AppRepository
import com.example.srchicken.data.BillItemEntity
import com.example.srchicken.data.CustomerEntity
import com.example.srchicken.data.ProductEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val productId: Long = 0L,
    val productName: String = "",
    val qty: String = "",
    val rate: String = "",
) {
    val amount: Double get() = (qty.toDoubleOrNull() ?: 0.0) * (rate.toDoubleOrNull() ?: 0.0)
    val isValid: Boolean get() = productId != 0L && (qty.toDoubleOrNull() ?: 0.0) > 0.0 && (rate.toDoubleOrNull() ?: 0.0) > 0.0
}

data class NewBillUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val selectedCustomerId: Long = 0L,
    val selectedCustomer: CustomerEntity? = null,
    val billDate: Long = System.currentTimeMillis(),
    val items: List<BillItem> = listOf(BillItem()),
    val discountValue: String = "0",
    val discountType: String = "flat",  // "flat" or "percent"
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxPercentage: Double = 0.0,
    val taxAmount: Double = 0.0,
    val openingBalance: Double = 0.0,
    val invoiceTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val isSaving: Boolean = false,
    val savedBillId: Long? = null,
    val error: String? = null,
    // Price memory: productId -> last rate used
    val lastPrices: Map<Long, Double> = emptyMap()
)

class NewBillViewModel(private val repo: AppRepository) : ViewModel() {

    private val _state = MutableStateFlow(NewBillUiState())
    val state: StateFlow<NewBillUiState> = _state.asStateFlow()

    // Price memory stored in-memory (in real app could persist to SharedPrefs)
    private val priceMemory = mutableMapOf<Long, Double>()

    init {
        viewModelScope.launch {
            combine(
                repo.getAllCustomers(),
                repo.getActiveProducts(),
                repo.getSettings()
            ) { customers, products, settings -> Triple(customers, products, settings) }
            .collect { (customers, products, settings) ->
                val tax = settings?.taxPercentage ?: 0.0
                _state.update { it.copy(customers = customers, products = products, taxPercentage = tax) }
                recalculate()
            }
        }
    }

    fun selectCustomer(customerId: Long) {
        val customer = _state.value.customers.find { it.id == customerId }
        _state.update { it.copy(selectedCustomerId = customerId, selectedCustomer = customer) }
        viewModelScope.launch {
            val balance = repo.getCustomerOutstandingBalance(customerId)
            _state.update { it.copy(openingBalance = balance) }
            recalculate()
        }
    }

    fun updateBillDate(date: Long) = _state.update { it.copy(billDate = date) }

    fun addItem() {
        _state.update { it.copy(items = it.items + BillItem()) }
    }

    fun removeItem(itemId: String) {
        if (_state.value.items.size <= 1) return
        _state.update { it.copy(items = it.items.filter { i -> i.id != itemId }) }
        recalculate()
    }

    fun updateItemProduct(itemId: String, productId: Long) {
        val product = _state.value.products.find { it.id == productId } ?: return
        val lastRate = priceMemory[productId]?.toString() ?: ""
        _state.update { state ->
            state.copy(items = state.items.map { item ->
                if (item.id == itemId) item.copy(
                    productId = productId,
                    productName = product.name,
                    rate = if (lastRate.isNotEmpty()) lastRate else item.rate
                ) else item
            })
        }
        recalculate()
    }

    fun updateItemQty(itemId: String, qty: String) {
        _state.update { state ->
            state.copy(items = state.items.map { item ->
                if (item.id == itemId) item.copy(qty = qty) else item
            })
        }
        recalculate()
    }

    fun updateItemRate(itemId: String, rate: String) {
        _state.update { state ->
            state.copy(items = state.items.map { item ->
                if (item.id == itemId) item.copy(rate = rate) else item
            })
        }
        recalculate()
    }

    fun updateDiscount(value: String) {
        _state.update { it.copy(discountValue = value) }
        recalculate()
    }

    fun updateDiscountType(type: String) {
        _state.update { it.copy(discountType = type) }
        recalculate()
    }

    private fun recalculate() {
        val state = _state.value
        val subtotal = state.items.sumOf { it.amount }
        val discVal = state.discountValue.toDoubleOrNull() ?: 0.0
        val discountAmount = if (state.discountType == "percent") {
            (subtotal * discVal / 100.0).coerceAtMost(subtotal)
        } else {
            discVal.coerceAtMost(subtotal)
        }
        val taxableAmount = subtotal - discountAmount
        val taxAmount = taxableAmount * (state.taxPercentage / 100.0)
        val invoiceTotal = taxableAmount + taxAmount
        _state.update {
            it.copy(
                subtotal = subtotal,
                discountAmount = discountAmount,
                taxAmount = taxAmount,
                invoiceTotal = invoiceTotal,
                grandTotal = invoiceTotal + it.openingBalance
            )
        }
    }

    fun generateBill(onSuccess: (Long) -> Unit) {
        val state = _state.value
        val customer = state.selectedCustomer
        if (customer == null) {
            _state.update { it.copy(error = "Please select a customer") }
            return
        }
        val validItems = state.items.filter { it.isValid }
        if (validItems.isEmpty()) {
            _state.update { it.copy(error = "Please add at least one item with quantity and rate") }
            return
        }
        _state.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch {
            try {
                // Save price memory
                validItems.forEach { item -> priceMemory[item.productId] = item.rate.toDoubleOrNull() ?: 0.0 }

                val billId = repo.createBill(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    customerCode = customer.customerCode,
                    items = validItems.map { item ->
                        BillItemEntity(
                            billId = 0L,
                            productId = item.productId,
                            productName = item.productName,
                            qty = item.qty.toDoubleOrNull() ?: 0.0,
                            rate = item.rate.toDoubleOrNull() ?: 0.0,
                            amount = item.amount
                        )
                    },
                    subtotal = state.subtotal,
                    discountValue = state.discountValue.toDoubleOrNull() ?: 0.0,
                    discountType = state.discountType,
                    discountAmount = state.discountAmount,
                    taxPercentage = state.taxPercentage,
                    taxAmount = state.taxAmount,
                    total = state.invoiceTotal,
                    openingBalance = state.openingBalance,
                    createdAt = state.billDate
                )
                resetForm()
                onSuccess(billId)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Failed to create bill: ${e.message}") }
            }
        }
    }

    fun resetForm() {
        _state.update { it.copy(
            selectedCustomerId = 0L,
            selectedCustomer = null,
            billDate = System.currentTimeMillis(),
            items = listOf(BillItem()),
            discountValue = "0",
            discountType = "flat",
            subtotal = 0.0,
            discountAmount = 0.0,
            openingBalance = 0.0,
            invoiceTotal = 0.0,
            grandTotal = 0.0,
            isSaving = false,
            savedBillId = null,
            error = null
        )}
    }

    fun clearError() { _state.update { it.copy(error = null) } }
}
