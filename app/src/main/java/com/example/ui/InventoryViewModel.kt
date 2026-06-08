package com.example.ui

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

    init {
        viewModelScope.launch {
            // Демо мэдээллүүд урьдчилан бэлтгэх функцийг ажиллуулна
            repository.prepopulateIfEmpty()
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

    // Гүйлгээний түүх (Төрлөөр нь шүүж харуулна)
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        repository.allTransactions,
        _txTypeFilter
    ) { txs, filter ->
        if (filter == "ALL") txs else txs.filter { it.type == filter }
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
