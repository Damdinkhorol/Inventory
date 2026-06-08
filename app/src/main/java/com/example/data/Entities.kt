package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Бараа материалын үндсэн бүртгэл
 */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val barcode: String,
    val name: String,
    val category: String, // Хэсэг / Ангилал (Жишээ нь: Барилга, Цахилгаан, ХАБЭА)
    val unit: String,     // Хэмжих нэгж (Жишээ нь: ш, кг, м, литр, тонн)
    val description: String = "",
    val minQuantity: Double = 0.0 // Доод үлдэгдлийн дохио
)

/**
 * Байршил / Агуулах
 */
@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,     // Агуулахын нэр (Жишээ нь: Төв агуулах, Салбар агуулах А)
    val code: String,     // Код (Жишээ нь: WH01, WH02)
    val manager: String = "" // Хариуцагч нярав
)

/**
 * Хөдөлгөөний гүйлгээ (Орлого, Шилжилт хөдөлгөөн, Зарлага)
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "INCOMING" (Орлого), "TRANSFER" (Шилжилт), "OUTBOUND" (Зарлага)
    val itemId: Int,  // Барааны ID
    val quantity: Double,
    val fromWarehouseId: Int?, // Төрөл нь TRANSFER эсвэл OUTBOUND бол ашиглагдана
    val toWarehouseId: Int?,   // Төрөл нь TRANSFER эсвэл INCOMING бол ашиглагдана
    val partnerName: String = "", // Нийлүүлэгч (Орлого) эсвэл Хүлээн авагч/Салбар хэлтэс (Зарлага)
    val timestamp: Long = System.currentTimeMillis(),
    val remarks: String = "", // Тайлбар / Тэмдэглэл
    val performedBy: String = "" // Бүртгэсэн ажилтан / Нярав
)
