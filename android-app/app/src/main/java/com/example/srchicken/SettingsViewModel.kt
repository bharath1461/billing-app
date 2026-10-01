package com.example.srchicken

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.srchicken.data.AppRepository
import com.example.srchicken.data.ProductEntity
import com.example.srchicken.data.SettingsEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: SettingsEntity = SettingsEntity(),
    val products: List<ProductEntity> = emptyList(),
    val newProductName: String = "",
    val newProductUnit: String = "pcs",
    val isSaved: Boolean = false,
    val showClearConfirm: Boolean = false,
    // Editable fields
    val shopName: String = "SR Billing",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val shopGstin: String = "",
    val taxPercentage: String = "0"
)

class SettingsViewModel(private val repo: AppRepository) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(repo.getSettings(), repo.getAllProducts()) { settings, products ->
                Pair(settings, products)
            }.collect { (settings, products) ->
                val s = settings ?: SettingsEntity()
                _state.update { it.copy(
                    settings = s,
                    products = products,
                    shopName = s.shopName,
                    shopPhone = s.shopPhone,
                    shopAddress = s.shopAddress,
                    shopGstin = s.shopGstin,
                    taxPercentage = s.taxPercentage.toString()
                )}
            }
        }
    }

    fun updateShopName(v: String) = _state.update { it.copy(shopName = v) }
    fun updateShopPhone(v: String) = _state.update { it.copy(shopPhone = v) }
    fun updateShopAddress(v: String) = _state.update { it.copy(shopAddress = v) }
    fun updateShopGstin(v: String) = _state.update { it.copy(shopGstin = v) }
    fun updateTaxPercentage(v: String) = _state.update { it.copy(taxPercentage = v) }

    fun saveSettings() {
        val s = _state.value
        viewModelScope.launch {
            repo.saveSettings(SettingsEntity(
                shopName = s.shopName.trim(),
                shopPhone = s.shopPhone.trim(),
                shopAddress = s.shopAddress.trim(),
                shopGstin = s.shopGstin.trim(),
                taxPercentage = s.taxPercentage.toDoubleOrNull() ?: 0.0
            ))
            _state.update { it.copy(isSaved = true) }
        }
    }

    fun clearSavedFlag() = _state.update { it.copy(isSaved = false) }

    fun updateNewProductName(name: String) = _state.update { it.copy(newProductName = name) }
    fun updateNewProductUnit(unit: String) = _state.update { it.copy(newProductUnit = unit) }

    fun addProduct() {
        val name = _state.value.newProductName.trim()
        val unit = _state.value.newProductUnit.trim().ifBlank { "pcs" }
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.addProduct(name, unit)
            _state.update { it.copy(newProductName = "", newProductUnit = "pcs") }
        }
    }

    fun toggleProduct(product: ProductEntity) {
        viewModelScope.launch { repo.toggleProduct(product) }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch { repo.deleteProduct(id) }
    }

    fun showClearConfirm() = _state.update { it.copy(showClearConfirm = true) }
    fun hideClearConfirm() = _state.update { it.copy(showClearConfirm = false) }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAllData()
            _state.update { it.copy(showClearConfirm = false) }
        }
    }
}
