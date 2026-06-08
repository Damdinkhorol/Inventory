package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class InventoryViewModel(private val repository: InventoryRepository) : ViewModel() {

    // --- Search & Filters ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("БУХИЙЛ") // "БУХИЙЛ" or specific categories
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _txTypeFilter = MutableStateFlow("ALL") // "ALL", "INCOMING", "TRANSFER", "OUTBOUND"
    val txTypeFilter = _txTypeFilter.asStateFlow()

    // --- System Status Message ---
    private val _uiMessage = MutableSharedFlow<UiMessage>()
    val uiMessage = _uiMessage.asSharedFlow()

    fun initializeDatabase(context: Context) {
        viewModelScope.launch {
            // Демо мэдээллүүд урьдчилан бэлтгэх функцийг ажиллуулна
            repository.prepopulateIfEmpty(context)
        }
    }

    // --- Reactive Data Streams ---
    val warehouses: StateFlow<List<WarehouseEntity>> = repository.allWarehouses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<ItemEntity>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Хайлт болон Ангилал бүхий барааны урсгал
    val itemsWithStock: StateFlow<List<ItemWithStock>> = combine(
        repository.itemsWithStock,
        _searchQuery,
        _selectedCategory
    ) { items, query, category ->
        items.filter { itemWithStock ->
            val matchesQuery = itemWithStock.item.name.contains(query, ignoreCase = true) || 
                               itemWithStock.item.barcode.contains(query, ignoreCase = true) || 
                               itemWithStock.item.category.contains(query, ignoreCase = true)
            
            val matchesCategory = category == "БУХИЙЛ" || itemWithStock.item.category == category
            
            matchesQuery && matchesCategory
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Бодит цаг үеийн АНХААРУУЛГА: Доод хэмжээнээс багассан дутагдалтай бараанууд
    val lowStockItems: StateFlow<List<ItemWithStock>> = repository.itemsWithStock
        .map { items ->
            items.filter { it.totalStock < it.item.minQuantity }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _txYearFilter = MutableStateFlow<Int?>(null)
    val txYearFilter = _txYearFilter.asStateFlow()

    private val _txMonthFilter = MutableStateFlow<Int?>(null)
    val txMonthFilter = _txMonthFilter.asStateFlow()

    private val _txDayFilter = MutableStateFlow<Int?>(null)
    val txDayFilter = _txDayFilter.asStateFlow()

    // --- Dashboard Specific Date Filters ---
    private val _dbYearFilter = MutableStateFlow<Int?>(null)
    val dbYearFilter = _dbYearFilter.asStateFlow()

    private val _dbMonthFilter = MutableStateFlow<Int?>(null)
    val dbMonthFilter = _dbMonthFilter.asStateFlow()

    private val _dbDayFilter = MutableStateFlow<Int?>(null)
    val dbDayFilter = _dbDayFilter.asStateFlow()

    // Гүйлгээний түүх (Төрөл болон огноогоор нь шүүж харуулна)
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _txTypeFilter,
        _txYearFilter,
        _txMonthFilter,
        _txDayFilter
    ) { txs, filter, year, month, day ->
        val filteredByType = if (filter == "ALL") txs else txs.filter { it.type == filter }
        filteredByType.filter { tx ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val txYear = cal.get(java.util.Calendar.YEAR)
            val txMonth = cal.get(java.util.Calendar.MONTH) + 1
            val txDay = cal.get(java.util.Calendar.DAY_OF_MONTH)

            val matchesYear = year == null || txYear == year
            val matchesMonth = month == null || txMonth == month
            val matchesDay = day == null || txDay == day

            matchesYear && matchesMonth && matchesDay
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Хяналтын самбарын огноогоор шүүсэн гүйлгээнүүд
    val dashboardTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _dbYearFilter,
        _dbMonthFilter,
        _dbDayFilter
    ) { txs, year, month, day ->
        txs.filter { tx ->
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val txYear = cal.get(java.util.Calendar.YEAR)
            val txMonth = cal.get(java.util.Calendar.MONTH) + 1
            val txDay = cal.get(java.util.Calendar.DAY_OF_MONTH)

            val matchesYear = year == null || txYear == year
            val matchesMonth = month == null || txMonth == month
            val matchesDay = day == null || txDay == day

            matchesYear && matchesMonth && matchesDay
        }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Санах ой дахь барааны бүх ангилал
    val categories: StateFlow<List<String>> = repository.allItems
        .map { items ->
            listOf("БУХИЙЛ") + items.map { it.category }.distinct().filter { it.isNotEmpty() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("БУХИЙЛ"))

    // --- Search / Filter Handlers ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateTxTypeFilter(filter: String) {
        _txTypeFilter.value = filter
    }

    fun updateDateFilters(year: Int?, month: Int?, day: Int?) {
        _txYearFilter.value = year
        _txMonthFilter.value = month
        _txDayFilter.value = day
    }

    fun updateDbDateFilters(year: Int?, month: Int?, day: Int?) {
        _dbYearFilter.value = year
        _dbMonthFilter.value = month
        _dbDayFilter.value = day
    }

    // --- Action Handlers ---

    /**
     * Шинэ бараа материалын бүртгэл үүсгэх
     */
    fun registerNewItem(
        barcode: String,
        name: String,
        category: String,
        unit: String,
        description: String,
        minQuantity: Double
    ) {
        viewModelScope.launch {
            if (name.isBlank() || category.isBlank() || unit.isBlank()) {
                _uiMessage.emit(UiMessage.Error("Улаан талбаруудыг бүрэн бөглөнө үү!"))
                return@launch
            }
            
            val existing = repository.getItemByBarcode(barcode)
            if (existing != null && barcode.isNotBlank()) {
                _uiMessage.emit(UiMessage.Error("Уг баркод бүхий бараа аль хэдийн бүртгэгдсэн байна!"))
                return@launch
            }

            val finalBarcode = if (barcode.isBlank()) {
                "BC-${System.currentTimeMillis().toString().takeLast(6)}"
            } else {
                barcode
            }

            val item = ItemEntity(
                barcode = finalBarcode,
                name = name.trim(),
                category = category.trim(),
                unit = unit.trim(),
                description = description.trim(),
                minQuantity = minQuantity
            )
            repository.insertItem(item)
            _uiMessage.emit(UiMessage.Success("'$name' барааг амжилттай бүртгэлээ."))
        }
    }

    /**
     * Шинэ байршил / агуулах бүртгэх
     */
    fun registerNewWarehouse(name: String, code: String, manager: String) {
        viewModelScope.launch {
            if (name.isBlank() || code.isBlank()) {
                _uiMessage.emit(UiMessage.Error("Агуулахын нэр болон кодыг заавал оруулна уу!"))
                return@launch
            }

            val warehouse = WarehouseEntity(
                name = name.trim(),
                code = code.trim().uppercase(),
                manager = manager.trim()
            )
            repository.insertWarehouse(warehouse)
            _uiMessage.emit(UiMessage.Success("'$name' агуулахыг амжилттай бүртгэлээ."))
        }
    }

    /**
     * Барааны орлого бүртгэх (INCOMING)
     */
    fun recordIncoming(
        itemId: Int,
        quantity: Double,
        warehouseId: Int,
        supplierName: String,
        remarks: String,
        performedBy: String
    ) {
        viewModelScope.launch {
            if (itemId == 0 || quantity <= 0.0 || warehouseId == 0) {
                _uiMessage.emit(UiMessage.Error("Орлогын тоо хэмжээ, бараа, агуулахыг зөв сонгоно уу!"))
                return@launch
            }

            val tx = TransactionEntity(
                type = "INCOMING",
                itemId = itemId,
                quantity = quantity,
                fromWarehouseId = null,
                toWarehouseId = warehouseId,
                partnerName = supplierName.trim(),
                remarks = remarks.trim(),
                performedBy = performedBy.trim().ifBlank { "Систем" }
            )
            repository.insertTransaction(tx)
            _uiMessage.emit(UiMessage.Success("Орлогын гүйлгээ амжилттай бүртгэгдлээ."))
        }
    }

    /**
     * Барааны шилжилт хөдөлгөөн бүртгэх (TRANSFER)
     */
    fun recordTransfer(
        itemId: Int,
        quantity: Double,
        fromWarehouseId: Int,
        toWarehouseId: Int,
        remarks: String,
        performedBy: String
    ) {
        viewModelScope.launch {
            if (itemId == 0 || quantity <= 0.0 || fromWarehouseId == 0 || toWarehouseId == 0) {
                _uiMessage.emit(UiMessage.Error("Шилжилтийн тоо хэмжээ болон агуулахуудыг зөв сонгоно уу!"))
                return@launch
            }

            if (fromWarehouseId == toWarehouseId) {
                _uiMessage.emit(UiMessage.Error("Ижил агуулах хооронд шилжүүлэх боломжгүй!"))
                return@launch
            }

            // Тухайн барааны илгээж буй агуулах дахь үлдэгдлийг шалгах
            val currentStockList = repository.itemsWithStock.first()
            val targetItem = currentStockList.find { it.item.id == itemId }
            val currentInSource = targetItem?.stockByWarehouse?.get(fromWarehouseId) ?: 0.0

            if (currentInSource < quantity) {
                _uiMessage.emit(UiMessage.Error("Уучлаарай, илгээх агуулах дахь үлдэгдэл хүрэлцэхгүй байна! (Үлдэгдэл: $currentInSource)"))
                return@launch
            }

            val tx = TransactionEntity(
                type = "TRANSFER",
                itemId = itemId,
                quantity = quantity,
                fromWarehouseId = fromWarehouseId,
                toWarehouseId = toWarehouseId,
                partnerName = "Агуулах хоорондын шилжилт",
                remarks = remarks.trim(),
                performedBy = performedBy.trim().ifBlank { "Систем" }
            )
            repository.insertTransaction(tx)
            _uiMessage.emit(UiMessage.Success("Шилжилт хөдөлгөөн амжилттай бүртгэгдлээ."))
        }
    }

    /**
     * Барааны зарлага бүртгэх (OUTBOUND)
     */
    fun recordOutbound(
        itemId: Int,
        quantity: Double,
        fromWarehouseId: Int,
        recipientName: String,
        remarks: String,
        performedBy: String
    ) {
        viewModelScope.launch {
            if (itemId == 0 || quantity <= 0.0 || fromWarehouseId == 0) {
                _uiMessage.emit(UiMessage.Error("Зарлагын тоо хэмжээ, бараа, агуулахыг зөв сонгоно уу!"))
                return@launch
            }

            // Байршил дахь үлдэгдлийг шалгах
            val currentStockList = repository.itemsWithStock.first()
            val targetItem = currentStockList.find { it.item.id == itemId }
            val currentInSource = targetItem?.stockByWarehouse?.get(fromWarehouseId) ?: 0.0

            if (currentInSource < quantity) {
                _uiMessage.emit(UiMessage.Error("Уучлаарай, тус агуулах дахь үлдэгдэл хүрэлцэхгүй байна! (Үлдэгдэл: $currentInSource)"))
                return@launch
            }

            val tx = TransactionEntity(
                type = "OUTBOUND",
                itemId = itemId,
                quantity = quantity,
                fromWarehouseId = fromWarehouseId,
                toWarehouseId = null,
                partnerName = recipientName.trim(),
                remarks = remarks.trim(),
                performedBy = performedBy.trim().ifBlank { "Систем" }
            )
            repository.insertTransaction(tx)
            _uiMessage.emit(UiMessage.Success("Зарлагын гүйлгээ амжилттай бүртгэгдлээ."))
        }
    }

    /**
     * Агуулахын мэдээлэл шинэчлэх
     */
    fun updateWarehouse(warehouse: WarehouseEntity) {
        viewModelScope.launch {
            if (warehouse.name.isBlank() || warehouse.code.isBlank()) {
                _uiMessage.emit(UiMessage.Error("Агуулахын нэр болон кодыг заавал оруулна уу!"))
                return@launch
            }
            repository.updateWarehouse(warehouse)
            _uiMessage.emit(UiMessage.Success("'${warehouse.name}' агуулахын мэдээлэл амжилттай шинэчлэгдлээ."))
        }
    }

    /**
     * Агуулах устгах
     */
    fun deleteWarehouse(warehouse: WarehouseEntity) {
        viewModelScope.launch {
            repository.deleteWarehouse(warehouse)
            _uiMessage.emit(UiMessage.Success("'${warehouse.name}' агуулахыг амжилттай устгалаа."))
        }
    }

    /**
     * Гүйлгээний мэдээлэл шинэчлэх
     */
    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (transaction.quantity <= 0.0) {
                _uiMessage.emit(UiMessage.Error("Тоо хэмжээ 0-ээс их байх ёстой!"))
                return@launch
            }
            repository.updateTransaction(transaction)
            _uiMessage.emit(UiMessage.Success("Гүйлгээний мэдээллийг амжилттай шинэчлэлээ."))
        }
    }

    /**
     * Гүйлгээ устгах
     */
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _uiMessage.emit(UiMessage.Success("Гүйлгээний бүртгэлийг амжилттай устгалаа."))
        }
    }

    /**
     * Гүйлгээний түүхийг CSV файл болгон татах
     */
    fun downloadTransactionsReport(context: Context) {
        viewModelScope.launch {
            try {
                val transactionsList = repository.allTransactions.first()
                val itemsList = repository.allItems.first()
                val warehousesList = repository.allWarehouses.first()
                
                val resolver = context.contentResolver
                val filename = "inventory_transactions_${System.currentTimeMillis()}.csv"
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                } else {
                    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadDir, filename)
                    android.net.Uri.fromFile(file)
                }
                
                if (uri == null) {
                    _uiMessage.emit(UiMessage.Error("Файл үүсгэхэд алдаа гарлаа."))
                    return@launch
                }
                
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream, "UTF-8"))
                    writer.write("\uFEFF") // Write UTF-8 BOM for Excel compatibility
                    writer.write("Дугаар,Төрөл,Бараа,Тоо хэмжээ,Хаанаас,Хаашаа,Түнш/Нийлүүлэгч,Тайлбар,Огноо,Нярав\n")
                    
                    transactionsList.forEach { tx ->
                        val item = itemsList.find { it.id == tx.itemId }
                        val fromWh = warehousesList.find { it.id == tx.fromWarehouseId }
                        val toWh = warehousesList.find { it.id == tx.toWarehouseId }
                        val typeName = when (tx.type) {
                            "INCOMING" -> "Орлого"
                            "TRANSFER" -> "Шилжилт"
                            "OUTBOUND" -> "Зарлага"
                            else -> tx.type
                        }
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(tx.timestamp))
                        
                        val itemName = item?.let { "${it.name} (${it.unit})" } ?: "Устсан бараа"
                        val fromWhName = fromWh?.name ?: "-"
                        val toWhName = toWh?.name ?: "-"
                        
                        writer.write(
                            "${tx.id}," +
                            "${typeName}," +
                            "\"${itemName.replace("\"", "\"\"")}\"," +
                            "${tx.quantity}," +
                            "\"${fromWhName.replace("\"", "\"\"")}\"," +
                            "\"${toWhName.replace("\"", "\"\"")}\"," +
                            "\"${tx.partnerName.replace("\"", "\"\"")}\"," +
                            "\"${tx.remarks.replace("\"", "\"\"")}\"," +
                            "${dateStr}," +
                            "\"${tx.performedBy.replace("\"", "\"\"")}\"\n"
                        )
                    }
                    writer.flush()
                }
                
                shareCsvFile(context, uri, filename)
                _uiMessage.emit(UiMessage.Success("Гүйлгээний тайлан амжилттай татагдлаа! (Downloads/$filename)"))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                _uiMessage.emit(UiMessage.Error("Тайлан татахад алдаа гарлаа: ${e.message}"))
            }
        }
    }

    /**
     * Барааны үлдэгдлийн тайланг CSV файл болгон татах
     */
    fun downloadStockReport(context: Context) {
        viewModelScope.launch {
            try {
                val stockList = repository.itemsWithStock.first()
                val resolver = context.contentResolver
                val filename = "inventory_stock_${System.currentTimeMillis()}.csv"
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                } else {
                    val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadDir, filename)
                    android.net.Uri.fromFile(file)
                }
                
                if (uri == null) {
                    _uiMessage.emit(UiMessage.Error("Файл үүсгэхэд алдаа гарлаа."))
                    return@launch
                }
                
                resolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream, "UTF-8"))
                    writer.write("\uFEFF") // Write UTF-8 BOM for Excel compatibility
                    writer.write("Баркод,Барааны нэр,Ангилал,Хэмжих нэгж,Доод хязгаар,Нийт үлдэгдэл,Төлөв\n")
                    
                    stockList.forEach { stock ->
                        val item = stock.item
                        val status = if (stock.totalStock < item.minQuantity) "Дутагдалтай" else "Хэвийн"
                        
                        writer.write(
                            "\"${item.barcode.replace("\"", "\"\"")}\"," +
                            "\"${item.name.replace("\"", "\"\"")}\"," +
                            "\"${item.category.replace("\"", "\"\"")}\"," +
                            "\"${item.unit.replace("\"", "\"\"")}\"," +
                            "${item.minQuantity}," +
                            "${stock.totalStock}," +
                            "\"${status}\"\n"
                        )
                    }
                    writer.flush()
                }
                
                shareCsvFile(context, uri, filename)
                _uiMessage.emit(UiMessage.Success("Үлдэгдлийн тайлан амжилттай татагдлаа! (Downloads/$filename)"))
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                _uiMessage.emit(UiMessage.Error("Үлдэгдлийн тайлан татахад алдаа гарлаа: ${e.message}"))
            }
        }
    }

    private fun shareCsvFile(context: Context, uri: android.net.Uri, filename: String) {
        try {
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(android.content.Intent.EXTRA_SUBJECT, filename)
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Тайланг хуваалцах / хадгалах"))
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Системийн бүх өгөгдлийг устгах
     */
    fun clearAllData(context: Context) {
        viewModelScope.launch {
            repository.clearAllDatabase(context)
            _uiMessage.emit(UiMessage.Success("Бүх оруулсан өгөгдлийг амжилттай устгаж, системийг цэвэрлэлээ."))
        }
    }

    /**
     * Анхны демо өгөгдлийг сэргээж оруулах
     */
    fun resetToDemoData(context: Context) {
        viewModelScope.launch {
            repository.forcePrepopulate(context)
            _uiMessage.emit(UiMessage.Success("Анхны демо өгөгдлийг амжилттай сэргээж орууллаа."))
        }
    }
}

/**
 * ViewModel Factory
 */
class InventoryViewModelFactory(private val repository: InventoryRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
