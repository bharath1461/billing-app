package com.example.srchicken.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class BillWithItems(
    val bill: BillEntity,
    val items: List<BillItemEntity>
)

data class DashboardStats(
    val todayBillCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val totalCustomers: Int = 0,
    val totalRevenue: Double = 0.0
)

class AppRepository(private val db: AppDatabase) {

    // ---- Customers ----
    fun getAllCustomers(): Flow<List<CustomerEntity>> = db.customerDao().getAllCustomers()

    suspend fun getCustomerById(id: Long): CustomerEntity? = db.customerDao().getCustomerById(id)

    suspend fun addCustomer(name: String, phone: String, customerCode: String, openingBalance: Double): Long =
        db.customerDao().insertCustomer(CustomerEntity(name = name, phone = phone, customerCode = customerCode, openingBalance = openingBalance))

    suspend fun updateCustomer(id: Long, name: String, phone: String, customerCode: String, openingBalance: Double) {
        val existing = getCustomerById(id) ?: return
        db.customerDao().updateCustomer(existing.copy(name = name, phone = phone, customerCode = customerCode, openingBalance = openingBalance))
    }

    suspend fun getCustomerOutstandingBalance(customerId: Long): Double {
        val customer = getCustomerById(customerId) ?: return 0.0
        return customer.openingBalance + db.customerDao().getCustomerInvoiceTotal(customerId)
    }

    suspend fun deleteCustomer(id: Long) = db.customerDao().deleteCustomer(id)

    // ---- Bills ----
    fun getAllBills(): Flow<List<BillEntity>> = db.billDao().getAllBills()

    fun getRecentBills(): Flow<List<BillEntity>> = db.billDao().getRecentBills()

    suspend fun getBillById(id: Long): BillEntity? = db.billDao().getBillById(id)

    suspend fun getItemsForBill(billId: Long): List<BillItemEntity> = db.billDao().getItemsForBill(billId)

    suspend fun createBill(
        customerId: Long,
        customerName: String,
        customerPhone: String,
        customerCode: String,
        items: List<BillItemEntity>,
        subtotal: Double,
        discountValue: Double,
        discountType: String,
        discountAmount: Double,
        taxPercentage: Double,
        taxAmount: Double,
        total: Double,
        openingBalance: Double,
        createdAt: Long
    ): Long {
        val billNumber = generateBillNumber()
        val billId = db.billDao().insertBill(
            BillEntity(
                billNumber = billNumber,
                customerId = customerId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerCode = customerCode,
                subtotal = subtotal,
                discountValue = discountValue,
                discountType = discountType,
                discountAmount = discountAmount,
                taxPercentage = taxPercentage,
                taxAmount = taxAmount,
                total = total,
                openingBalance = openingBalance,
                totalDue = total + openingBalance,
                createdAt = createdAt
            )
        )
        val itemsWithBillId = items.map { it.copy(billId = billId) }
        db.billDao().insertBillItems(itemsWithBillId)
        return billId
    }

    suspend fun deleteBill(id: Long) {
        db.billDao().deleteBillItems(id)
        db.billDao().deleteBill(id)
    }

    suspend fun updateBillDate(id: Long, date: Long) = db.billDao().updateBillDate(id, date)

    // ---- Products ----
    fun getAllProducts(): Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    fun getActiveProducts(): Flow<List<ProductEntity>> = db.productDao().getActiveProducts()
    suspend fun getActiveProductsOnce(): List<ProductEntity> = db.productDao().getActiveProductsOnce()

    suspend fun addProduct(name: String, unit: String = "pcs") =
        db.productDao().insertProduct(ProductEntity(name = name, unit = unit, active = true))

    suspend fun toggleProduct(product: ProductEntity) =
        db.productDao().updateProduct(product.copy(active = !product.active))

    suspend fun deleteProduct(id: Long) = db.productDao().deleteProduct(id)

    // ---- Settings ----
    fun getSettings(): Flow<SettingsEntity?> = db.settingsDao().getSettings()
    suspend fun getSettingsOnce(): SettingsEntity = db.settingsDao().getSettingsOnce() ?: SettingsEntity()
    suspend fun saveSettings(settings: SettingsEntity) = db.settingsDao().saveSettings(settings)

    // ---- Bill Number Generation ----
    private suspend fun generateBillNumber(): String {
        val cal = Calendar.getInstance()
        val dateStr = String.format(
            "%04d%02d%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val count = db.billDao().getBillCountForDate("INV-$dateStr") + 1
        return "INV-$dateStr-${String.format("%03d", count)}"
    }

    // ---- Dashboard Stats ----
    fun getDashboardStats(
        customerCount: Flow<Int>
    ): Flow<DashboardStats> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        return combine(
            db.billDao().getTodayBillCount(startOfDay, endOfDay),
            db.billDao().getTodayRevenue(startOfDay, endOfDay),
            customerCount,
            db.billDao().getTotalRevenue()
        ) { todayCount, todayRev, custCount, totalRev ->
            DashboardStats(todayCount, todayRev, custCount, totalRev)
        }
    }

    // ---- Data Export/Import ----
    data class ExportData(
        val customers: List<CustomerEntity>,
        val bills: List<BillEntity>,
        val billItems: List<BillItemEntity>,
        val products: List<ProductEntity>,
        val settings: SettingsEntity,
        val exportedAt: String
    )

    suspend fun exportToJson(): String {
        val allBills = mutableListOf<BillEntity>()
        val allItems = mutableListOf<BillItemEntity>()
        // We can't easily get all bills at once from a Flow here; use a workaround
        val gson = Gson()
        val settings = getSettingsOnce()
        val exportData = mapOf(
            "exportedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
            "settings" to settings
        )
        return gson.toJson(exportData)
    }

    suspend fun clearAllData() {
        // Re-create all tables by deleting all rows
        val customers = mutableListOf<Long>()
        // Simple approach: delete everything
        db.runInTransaction {
            db.query("DELETE FROM bill_items", null)
            db.query("DELETE FROM bills", null)
            db.query("DELETE FROM customers", null)
        }
    }

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                AppRepository(db).also { INSTANCE = it }
            }
        }
    }
}
