package com.example.srchicken.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT COALESCE(SUM(total), 0) FROM bills WHERE customerId = :customerId")
    suspend fun getCustomerInvoiceTotal(customerId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomer(id: Long)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY createdAt DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Long): BillEntity?

    @Query("UPDATE bills SET createdAt = :createdAt WHERE id = :id")
    suspend fun updateBillDate(id: Long, createdAt: Long)

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    suspend fun getItemsForBill(billId: Long): List<BillItemEntity>

    @Query("SELECT * FROM bill_items WHERE billId = :billId")
    fun getItemsForBillFlow(billId: Long): Flow<List<BillItemEntity>>

    @Insert
    suspend fun insertBill(bill: BillEntity): Long

    @Insert
    suspend fun insertBillItems(items: List<BillItemEntity>)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBill(id: Long)

    @Query("DELETE FROM bill_items WHERE billId = :billId")
    suspend fun deleteBillItems(billId: Long)

    @Query("SELECT COUNT(*) FROM bills WHERE createdAt >= :startOfDay AND createdAt <= :endOfDay")
    fun getTodayBillCount(startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(total), 0) FROM bills WHERE createdAt >= :startOfDay AND createdAt <= :endOfDay")
    fun getTodayRevenue(startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(total), 0) FROM bills")
    fun getTotalRevenue(): Flow<Double>

    @Query("SELECT * FROM bills ORDER BY createdAt DESC LIMIT 5")
    fun getRecentBills(): Flow<List<BillEntity>>

    @Query("SELECT COUNT(*) FROM bills WHERE billNumber LIKE :datePattern || '%'")
    suspend fun getBillCountForDate(datePattern: String): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    fun getActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    suspend fun getActiveProductsOnce(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)
}
