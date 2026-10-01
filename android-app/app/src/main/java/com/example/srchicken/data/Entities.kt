package com.example.srchicken.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    /** Identifier chosen by the business, e.g. restaurant code or account number. */
    val customerCode: String = "",
    /** Balance carried into the app before the first invoice. */
    val openingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String = "",
    /** Snapshotted custom customer ID so historical bills remain accurate. */
    val customerCode: String = "",
    val subtotal: Double,
    val discountValue: Double = 0.0,
    val discountType: String = "flat", // "flat" or "percent"
    val discountAmount: Double = 0.0,
    val taxPercentage: Double = 0.0,
    val taxAmount: Double = 0.0,
    val total: Double,
    /** Customer balance before this invoice; retained for a clear audit trail. */
    val openingBalance: Double = 0.0,
    /** Amount due after adding the previous outstanding balance. */
    val totalDue: Double = total,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bill_items")
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val productId: Long,
    val productName: String,
    val qty: Double,
    val rate: Double,
    val amount: Double
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unit: String = "kg",
    val active: Boolean = true
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "SR Billing",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val shopGstin: String = "",
    val taxPercentage: Double = 0.0
)
