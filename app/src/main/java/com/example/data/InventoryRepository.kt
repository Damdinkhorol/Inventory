package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Барааны үлдэгдлийг агуулах тус бүрээр болон нийт дүнгээр агуулсан загвар
 */
data class ItemWithStock(
    val item: ItemEntity,
    val totalStock: Double,
    val stockByWarehouse: Map<Int, Double> // WarehouseId -> Stock quantity
)

class InventoryRepository(private val dao: InventoryDao) {

    val allItems: Flow<List<ItemEntity>> = dao.getAllItems()
    val allWarehouses: Flow<List<WarehouseEntity>> = dao.getAllWarehouses()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    /**
     * Бодит цаг хугацааны үлдэгдлийг тодорхойлох урсгал.
     * Бараа, Агуулах, Гүйлгээ өөрчлөгдөх бүрт шууд шинэчлэгдэнэ.
     */
    val itemsWithStock: Flow<List<ItemWithStock>> = combine(
        allItems,
        allWarehouses,
        allTransactions
    ) { items, warehouses, transactions ->
        items.map { item ->
            val stockMap = mutableMapOf<Int, Double>()
            // Агуулах бүрийн анхны үлдэгдэл 0.0
            warehouses.forEach { stockMap[it.id] = 0.0 }
            
            var total = 0.0

            // Шүүж авсан зөвхөн тухайн барааны гүйлгээнүүд
            val itemTxs = transactions.filter { it.itemId == item.id }
            
            // Багаас нь их рүү нь бодож эцсийн үлдэгдлийг тооцоолно
            itemTxs.reversed().forEach { tx ->
                when (tx.type) {
                    "INCOMING" -> {
                        tx.toWarehouseId?.let { whId ->
                            val current = stockMap[whId] ?: 0.0
                            stockMap[whId] = current + tx.quantity
                        }
                        total += tx.quantity
                    }
                    "OUTBOUND" -> {
                        tx.fromWarehouseId?.let { whId ->
                            val current = stockMap[whId] ?: 0.0
                            stockMap[whId] = current - tx.quantity
                        }
                        total -= tx.quantity
                    }
                    "TRANSFER" -> {
                        tx.fromWarehouseId?.let { fromId ->
                            val currentFrom = stockMap[fromId] ?: 0.0
                            stockMap[fromId] = currentFrom - tx.quantity
                        }
                        tx.toWarehouseId?.let { toId ->
                            val currentTo = stockMap[toId] ?: 0.0
                            stockMap[toId] = currentTo + tx.quantity
                        }
                        // Шилжилт хөдөлгөөн нь нийт барааны тоонд нөлөөлөхгүй
                    }
                }
            }

            ItemWithStock(
                item = item,
                totalStock = total,
                stockByWarehouse = stockMap
            )
        }
    }

    suspend fun insertItem(item: ItemEntity): Long = withContext(Dispatchers.IO) {
        dao.insertItem(item)
    }

    suspend fun updateItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        dao.updateItem(item)
    }

    suspend fun deleteItem(item: ItemEntity) = withContext(Dispatchers.IO) {
        dao.deleteItem(item)
    }

    suspend fun insertWarehouse(warehouse: WarehouseEntity): Long = withContext(Dispatchers.IO) {
        dao.insertWarehouse(warehouse)
    }

    suspend fun updateWarehouse(warehouse: WarehouseEntity) = withContext(Dispatchers.IO) {
        dao.updateWarehouse(warehouse)
    }

    suspend fun deleteWarehouse(warehouse: WarehouseEntity) = withContext(Dispatchers.IO) {
        dao.deleteWarehouse(warehouse)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(Dispatchers.IO) {
        dao.insertTransaction(transaction)
    }

    suspend fun getWarehouseById(id: Int): WarehouseEntity? = withContext(Dispatchers.IO) {
        dao.getWarehouseById(id)
    }

    suspend fun getItemById(id: Int): ItemEntity? = withContext(Dispatchers.IO) {
        dao.getItemById(id)
    }

    suspend fun getItemByBarcode(barcode: String): ItemEntity? = withContext(Dispatchers.IO) {
        dao.getItemByBarcode(barcode)
    }

    /**
     * Анхны демо өгөгдлийг бэлтгэж хадгалах
     */
    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        // Уг функцийг өгөгдлийн сан хоосон бол ажиллуулна
        val currentWarehouses = dao.getAllWarehouses()
        // Бусад зүйлийн урсгал авахгүйгээр шалгахын тулд корутин дотор үйлдэл хийнэ
        val isWhEmpty = dao.insertWarehouse(WarehouseEntity(name = "Түр шалгах", code = "CHECK")).also {
            dao.deleteWarehouse(WarehouseEntity(id = it.toInt(), name = "Түр шалгах", code = "CHECK"))
        } <= 0
        
        // Манай хэрэглэгчийн агуулахуудыг шалгая:
        // Үнэхээр хоосон байгаа бол бэлтгэнэ:
        val existingItems = dao.getItemById(1)
        if (existingItems == null) {
            // 1. Агуулахууд бүртгэх
            val whMainId = dao.insertWarehouse(WarehouseEntity(name = "Төв Агуулах", code = "TA-01", manager = "Б.Эрдэнэ"))
            val whSubAId = dao.insertWarehouse(WarehouseEntity(name = "Баруун Салбар", code = "BS-02", manager = "О.Баттулга"))
            val whConstructionId = dao.insertWarehouse(WarehouseEntity(name = "Барилгын Талбай Б", code = "BT-03", manager = "Г.Ананд"))

            // 2. Бараа материал бүртгэх
            val item1Id = dao.insertItem(ItemEntity(
                barcode = "4860012345671",
                name = "Цемент Портланд БЦ-400",
                category = "Барилга",
                unit = "Тонн",
                description = "Сайн чанарын барилгын цемент",
                minQuantity = 10.0
            ))
            val item2Id = dao.insertItem(ItemEntity(
                barcode = "4860012345672",
                name = "Арматур төмөр Ф12",
                category = "Барилга",
                unit = "Тонн",
                description = "Орос арматур, 12мм голчтой",
                minQuantity = 5.0
            ))
            val item3Id = dao.insertItem(ItemEntity(
                barcode = "4860012345673",
                name = "Кабель утас 3х2.5мм",
                category = "Цахилгаан",
                unit = "Метр",
                description = "Дотор кабель утас, зэс",
                minQuantity = 200.0
            ))
            val item4Id = dao.insertItem(ItemEntity(
                barcode = "4860012345674",
                name = "Хамгаалалтын Дуулга (Улаан)",
                category = "ХАБЭА",
                unit = "Ширхэг",
                description = "Хөдөлмөр хамгааллын стандартын дуулга",
                minQuantity = 15.0
            ))

            // 3. Анхны хөдөлгөөн оруулах (Орлого, Зарлага, Шилжилт)
            val now = System.currentTimeMillis()
            
            // Орлого: Цемент Төв Агуулахад 50 тонн авсан
            dao.insertTransaction(TransactionEntity(
                type = "INCOMING",
                itemId = item1Id.toInt(),
                quantity = 50.0,
                fromWarehouseId = null,
                toWarehouseId = whMainId.toInt(),
                partnerName = "Хөтөл Цемент ХК",
                timestamp = now - 3600000 * 12, // 12 цагийн өмнө
                remarks = "Эхний үлдэгдлийн хүлээн авалт",
                performedBy = "Б.Эрдэнэ"
            ))

            // Орлого: Арматур Төв Агуулахад 30 тонн авсан
            dao.insertTransaction(TransactionEntity(
                type = "INCOMING",
                itemId = item2Id.toInt(),
                quantity = 30.0,
                fromWarehouseId = null,
                toWarehouseId = whMainId.toInt(),
                partnerName = "Эрдэнэт Гариг ХХК",
                timestamp = now - 3600000 * 10,
                remarks = "Төслийн нөөц бүрдүүлэлт",
                performedBy = "Б.Эрдэнэ"
            ))

            // Орлого: Дуулга Төв Агуулахад 40 ширхэг авсан
            dao.insertTransaction(TransactionEntity(
                type = "INCOMING",
                itemId = item4Id.toInt(),
                quantity = 40.0,
                fromWarehouseId = null,
                toWarehouseId = whMainId.toInt(),
                partnerName = "Номин Стор",
                timestamp = now - 3600000 * 8,
                remarks = "ХАБЭА ажилчдын хэрэгсэл",
                performedBy = "Б.Эрдэнэ"
            ))

            // Шилжилт: Цемент Төв Агуулахаас Барилгын талбай Б рүү 15 тонн шилжүүлэв
            dao.insertTransaction(TransactionEntity(
                type = "TRANSFER",
                itemId = item1Id.toInt(),
                quantity = 15.0,
                fromWarehouseId = whMainId.toInt(),
                toWarehouseId = whConstructionId.toInt(),
                partnerName = "Барилгын талбайн шилжилт",
                timestamp = now - 3600000 * 5,
                remarks = "Суурийн цутгалтанд хэрэглэхээр шилжүүлэв",
                performedBy = "Б.Эрдэнэ"
            ))

            // Зарлага: Дуулга Барилгын Ажилчдад 12ш гаргасан
            dao.insertTransaction(TransactionEntity(
                type = "OUTBOUND",
                itemId = item4Id.toInt(),
                quantity = 12.0,
                fromWarehouseId = whMainId.toInt(),
                toWarehouseId = null,
                partnerName = "Барилга Угсралтын Хэлтэс",
                timestamp = now - 3600000 * 3,
                remarks = "Дотоодын хэрэглээ, шинэ туслах ажилчдад олгосон",
                performedBy = "Б.Эрдэнэ"
            ))

            // Орлого: Кабель Баруун Салбарт 500 метр авсан
            dao.insertTransaction(TransactionEntity(
                type = "INCOMING",
                itemId = item3Id.toInt(),
                quantity = 500.0,
                fromWarehouseId = null,
                toWarehouseId = whSubAId.toInt(),
                partnerName = "Нарлаг Сууц ХХК",
                timestamp = now - 3600000 * 1,
                remarks = "Цахилгааны ажлын эд анги",
                performedBy = "О.Баттулга"
            ))
        }
    }
}
