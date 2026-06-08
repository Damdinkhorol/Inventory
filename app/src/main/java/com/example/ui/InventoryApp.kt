package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryApp(viewModel: InventoryViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // UI-д үзүүлэх төлвүүд (State Flows)
    val itemsWithStock by viewModel.itemsWithStock.collectAsStateWithLifecycle()
    val lowStockItems by viewModel.lowStockItems.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    // Хайлт болон Шүүлтүүрийн төлөв
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val txTypeFilter by viewModel.txTypeFilter.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Хянах самбар, 1: Бараанууд, 2: Гүйлгээ, 3: Агуулахууд

    // Диалог нээх/хаах төлвүүд
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddWarehouseDialog by remember { mutableStateOf(false) }

    // Амжилттай эсвэл алдаа гарсан мэдэгдэл хүлээн авах
    LaunchedEffect(Unit) {
        viewModel.uiMessage.collectLatest { message ->
            when (message) {
                is UiMessage.Success -> {
                    snackbarHostState.showSnackbar(
                        message = message.message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                }
                is UiMessage.Error -> {
                    snackbarHostState.showSnackbar(
                        message = "⚠️ " + message.message,
                        withDismissAction = true,
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapCalls,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "StockFlow",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                actions = {
                    // Хурдан үйлдэл үүсгэх товчлуурууд
                    IconButton(
                        onClick = { showAddItemDialog = true },
                        modifier = Modifier.testTag("top_add_item_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LibraryAdd,
                            contentDescription = "Бараа бүртгэх",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.TrendingUp, contentDescription = "Dashboard") },
                    label = { Text("Хяналт", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_dash")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.Inventory, contentDescription = "Inventory") },
                    label = { Text("Үлдэгдэл", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_stocks")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.Autorenew, contentDescription = "Movements") },
                    label = { Text("Гүйлгээ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_txs")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(imageVector = Icons.Default.Storefront, contentDescription = "Warehouses") },
                    label = { Text("Агуулахууд", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("nav_whs")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Идэвхтэй цонхнуудыг навигацаар харуулах
            Crossfade(targetState = activeTab, label = "TabNavigation") { tab ->
                when (tab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        itemsCount = itemsWithStock.size,
                        transactionsCount = transactions.size,
                        lowStockItems = lowStockItems,
                        recentTransactions = transactions.take(5),
                        warehouses = warehouses,
                        onNavigateToTab = { activeTab = it }
                    )
                    1 -> StocksScreen(
                        items = itemsWithStock,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        warehouses = warehouses,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onCategorySelect = { viewModel.updateSelectedCategory(it) },
                        onAddNewItemClick = { showAddItemDialog = true }
                    )
                    2 -> TransactionsScreen(
                        viewModel = viewModel,
                        items = itemsWithStock.map { it.item },
                        warehouses = warehouses,
                        transactions = transactions,
                        txFilter = txTypeFilter,
                        onFilterChange = { viewModel.updateTxTypeFilter(it) }
                    )
                    3 -> WarehousesScreen(
                        warehouses = warehouses,
                        itemsWithStock = itemsWithStock,
                        onAddWarehouseClick = { showAddWarehouseDialog = true }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Шинэ бараа бүртгэх диалог
    if (showAddItemDialog) {
        AddItemDialog(
            categories = categories.filter { it != "БУХИЙЛ" },
            onDismiss = { showAddItemDialog = false },
            onConfirm = { barcode, name, category, unit, desc, minQty ->
                viewModel.registerNewItem(barcode, name, category, unit, desc, minQty)
                showAddItemDialog = false
            }
        )
    }

    // 2. Шинэ агуулах бүртгэх диалог
    if (showAddWarehouseDialog) {
        AddWarehouseDialog(
            onDismiss = { showAddWarehouseDialog = false },
            onConfirm = { name, code, manager ->
                viewModel.registerNewWarehouse(name, code, manager)
                showAddWarehouseDialog = false
            }
        )
    }
}

// ==================== SCREEN 1: DASHBOARD ====================
@Composable
fun DashboardScreen(
    viewModel: InventoryViewModel,
    itemsCount: Int,
    transactionsCount: Int,
    lowStockItems: List<ItemWithStock>,
    recentTransactions: List<TransactionEntity>,
    warehouses: List<WarehouseEntity>,
    onNavigateToTab: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Урилга болон Бодит цагийн төлөв
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Бодит Цагийн Хяналт",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Бараа материалын шилжилт, орлого, зарлагыг систем шууд (Real-time) тооцоолж байна.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .shadow(4.dp, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularSignalIndicator()
                    }
                }
            }
        }

        // Үндсэн статистик үзүүлэлтүүд (Мянганы орчинд)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Нэр төрөл",
                    value = "$itemsCount ш",
                    icon = Icons.Default.GridOn,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onNavigateToTab(1) }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Нийт гүйлгээ",
                    value = "$transactionsCount удаа",
                    icon = Icons.Default.SwapHoriz,
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = { onNavigateToTab(2) }
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Дутагдалтай",
                    value = "${lowStockItems.size} бараа",
                    icon = Icons.Default.Warning,
                    color = if (lowStockItems.isNotEmpty()) Color(0xFFE53935) else Color(0xFF43A047),
                    onClick = { if (lowStockItems.isNotEmpty()) onNavigateToTab(1) }
                )
            }
        }

        // Агуулах ачааллын тойм (График дүрслэл)
        item {
            WarehouseLoadChartCard(warehouses = warehouses, recentTransactions = recentTransactions)
        }

        // ДУТАГДЛЫН АНХААРУУЛГА (Бодит цагт ажиллах чухал хэсэг)
        if (lowStockItems.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🚨 Дутагдалтай бараа материалын анхааруулга",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = "Дүүргэх шаардлагатай",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Хэвтээ жагсаалтаар дутагдалтай бараануудыг үзүүлнэ
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(lowStockItems) { stock ->
                            LowStockWarningCard(stock = stock, onRefill = { onNavigateToTab(2) })
                        }
                    }
                }
            }
        }

        // СҮҮЛИЙН ХӨДӨЛГӨӨНҮҮД / АКТИВ УРСГАЛ
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📈 Сүүлийн үеийн гүйлгээний түүх",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                TextButton(onClick = { onNavigateToTab(2) }) {
                    Text("Бүгдийг харах", fontSize = 12.sp)
                }
            }
        }

        if (recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Гүйлгээний түүх хоосон байна.",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(recentTransactions) { tx ->
                // Энд барааны нэрийг уншиж харуулах хэрэгтэй байдаг учраас тусад нь зохионо
                TransactionStreamItem(tx = tx, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun CircularSignalIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "signal")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signalAlpha"
    )
    
    Canvas(modifier = Modifier.size(24.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        // Гаднах долгион
        drawCircle(
            color = Color(0xFF4CAF50).copy(alpha = alpha * 0.25f),
            radius = size.minDimension / 1.5f,
            center = center
        )
        // Дунд долгион
        drawCircle(
            color = Color(0xFF4CAF50).copy(alpha = alpha * 0.5f),
            radius = size.minDimension / 2.2f,
            center = center
        )
        // Төв цэг
        drawCircle(
            color = Color(0xFF4CAF50),
            radius = size.minDimension / 4.5f,
            center = center
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WarehouseLoadChartCard(
    warehouses: List<WarehouseEntity>,
    recentTransactions: List<TransactionEntity>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Агуулахын хөдөлгөөний идэвх",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (warehouses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Бүртгэлтэй агуулах байхгүй байна.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                // Агуулах бүрт ногдох сүүлийн гүйлгээнүүдийн тоог гаргаж өндрөөр дүрслэх
                val totalTxs = recentTransactions.size.coerceAtLeast(1)
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    warehouses.forEach { wh ->
                        val whTxs = recentTransactions.count { it.fromWarehouseId == wh.id || it.toWarehouseId == wh.id }
                        val ratio = whTxs.toFloat() / totalTxs.toFloat()
                        val percent = (ratio * 100).toInt()

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = wh.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(text = "$percent% идэвх", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Гоёмсог агуулахын идэвхжүүлэлтийн шугам
                            LinearProgressIndicator(
                                progress = { if (totalTxs > 1) ratio.coerceIn(0.1f, 1f) else 0.2f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                strokeCap = StrokeCap.Round,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LowStockWarningCard(stock: ItemWithStock, onRefill: () -> Unit) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stock.item.category,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stock.item.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF1A1A1A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${stock.totalStock}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stock.item.unit,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
            Text(
                text = "Доод хэмжээ: ${stock.item.minQuantity}",
                fontSize = 10.sp,
                color = Color.DarkGray.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRefill,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                Text("Орлого авах", fontSize = 10.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun TransactionStreamItem(tx: TransactionEntity, viewModel: InventoryViewModel) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()
    
    val matchedItem = items.find { it.id == tx.itemId }
    val fromWh = warehouses.find { it.id == tx.fromWarehouseId }
    val toWh = warehouses.find { it.id == tx.toWarehouseId }

    val sdf = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val formattedTime = sdf.format(Date(tx.timestamp))

    val (bgIconColor, icon, labelColor, titleText) = when (tx.type) {
        "INCOMING" -> Quadruple(
            Color(0xFFE8F5E9),
            Icons.Default.AddCircle,
            Color(0xFF388E3C),
            "Орлого хүлээн авсан"
        )
        "OUTBOUND" -> Quadruple(
            Color(0xFFFFEBEE),
            Icons.Default.RemoveCircle,
            Color(0xFFD32F2F),
            "Зарлага гаргасан"
        )
        else -> Quadruple(
            Color(0xFFE3F2FD),
            Icons.Default.CompareArrows,
            Color(0xFF1976D2),
            "Шилжилт хийсэн"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(0.5.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgIconColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = labelColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = matchedItem?.name ?: "Тодорхойгүй бараа",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val locationText = when (tx.type) {
                    "INCOMING" -> "➡️ ${toWh?.name ?: '?'}"
                    "OUTBOUND" -> "⬅️ ${fromWh?.name ?: '?'}"
                    else -> "${fromWh?.name ?: '?'} ➡️ ${toWh?.name ?: '?'}"
                }
                Text(
                    text = locationText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (tx.remarks.isNotBlank()) {
                    Text(
                        text = "Тэмдэглэл: ${tx.remarks}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val sign = if (tx.type == "INCOMING") "+" else if (tx.type == "OUTBOUND") "-" else ""
                Text(
                    text = "$sign${tx.quantity} ${matchedItem?.unit ?: ""}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = labelColor
                )
                Text(
                    text = formattedTime,
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ==================== SCREEN 2: BARAANUUD (STOCKS) ====================
@Composable
fun StocksScreen(
    items: List<ItemWithStock>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    warehouses: List<WarehouseEntity>,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onAddNewItemClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Хайлтын хэсэг
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("item_search_input"),
            placeholder = { Text("Барааны нэр, баркодоор хайх...", fontSize = 14.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        
        Spacer(modifier = Modifier.height(10.dp))

        // Ангилал шүүх хэсэг (Horizontal scroll chips)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelect(category) },
                    label = { Text(category, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Нийт жагсаалт (${items.size} бараа)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Button(
                onClick = onAddNewItemClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Шинэ бараа", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ийм нэртэй эсвэл ангилалтай бараа олдсонгүй.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { stock ->
                    StockListItem(stock = stock, warehouses = warehouses)
                }
            }
        }
    }
}

@Composable
fun StockListItem(stock: ItemWithStock, warehouses: List<WarehouseEntity>) {
    var expanded by remember { mutableStateOf(false) }
    val isLow = stock.totalStock < stock.item.minQuantity

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(0.5.dp, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLow) Color(0xFFFFF5F5) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Үндсэн хэсэг
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stock.item.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isLow) Color(0xFFC62828) else Color.Unspecified
                        )
                        if (isLow) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE53935))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Дутарлаа",
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Код: ${stock.item.barcode}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = stock.item.category,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${stock.totalStock}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = if (isLow) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stock.item.unit,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Дэлгэрэнгүй: Агуулах тус бүрээр үлдэгдэл задлах
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Divider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        text = "🏢 Агуулахын байршлаар үлдэгдэл:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (warehouses.isEmpty()) {
                        Text("Бүртгэлтэй агуулах байхгүй байна.", fontSize = 11.sp, color = Color.Gray)
                    } else {
                        warehouses.forEach { wh ->
                            val amt = stock.stockByWarehouse[wh.id] ?: 0.0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = wh.name,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                                Text(
                                    text = "$amt ${stock.item.unit}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (amt < 0) Color.Red else Color.Black
                                )
                            }
                        }
                    }

                    if (stock.item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Нэмэлт тэмдэглэл: ${stock.item.description}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ==================== SCREEN 3: GUILGEE (TRANSACTIONS) ====================
@Composable
fun TransactionsScreen(
    viewModel: InventoryViewModel,
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>,
    transactions: List<TransactionEntity>,
    txFilter: String,
    onFilterChange: (String) -> Unit
) {
    var activeTabFlow by remember { mutableStateOf(0) } // 0: Орлого бүртгэх, 1: Шилжилт хөдөлгөөн, 2: Зарлага бичих, 3: Бүх түүх харах

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Удирдах хэсгүүдийн сонголт
        TabRow(selectedTabIndex = activeTabFlow) {
            Tab(selected = activeTabFlow == 0, onClick = { activeTabFlow = 0 }) {
                Text("📥 Орлого", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTabFlow == 1, onClick = { activeTabFlow = 1 }) {
                Text("🔄 Шилжилт", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTabFlow == 2, onClick = { activeTabFlow = 2 }) {
                Text("📤 Зарлага", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Tab(selected = activeTabFlow == 3, onClick = { activeTabFlow = 3 }) {
                Text("📜 Бүх түүх", modifier = Modifier.padding(12.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeTabFlow) {
            0 -> IncomingForm(viewModel, items, warehouses)
            1 -> TransferForm(viewModel, items, warehouses)
            2 -> OutboundForm(viewModel, items, warehouses)
            3 -> HistoryList(transactions, txFilter, onFilterChange, viewModel)
        }
    }
}

// 1. Орлого бүртгэх маягт
@Composable
fun IncomingForm(
    viewModel: InventoryViewModel,
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>
) {
    var selectedItemId by remember { mutableStateOf(0) }
    var qtyString by remember { mutableStateOf("") }
    var selectedWhId by remember { mutableStateOf(0) }
    var partner by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var nurseName by remember { mutableStateOf("") }

    var itemExpanded by remember { mutableStateOf(false) }
    var whExpanded by remember { mutableStateOf(false) }

    val selectedItem = items.find { it.id == selectedItemId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Бараа материал шинээр хүлээн авах (Орлого)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            
            // Бараа сонгох
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { itemExpanded = true },
                    modifier = Modifier.fillMaxWidth().testTag("add_incoming_item_select_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = selectedItem?.name ?: "Бараа сонгох (*)",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = itemExpanded,
                    onDismissRequest = { itemExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("${item.name} (${item.unit})") },
                            onClick = {
                                selectedItemId = item.id
                                itemExpanded = false
                            }
                        )
                    }
                }
            }

            // Агуулах сонгох
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { whExpanded = true },
                    modifier = Modifier.fillMaxWidth().testTag("add_incoming_wh_select_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    val wh = warehouses.find { it.id == selectedWhId }
                    Text(text = wh?.name ?: "Агуулах / Байршил сонгох (*)")
                }
                DropdownMenu(
                    expanded = whExpanded,
                    onDismissRequest = { whExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    warehouses.forEach { wh ->
                        DropdownMenuItem(
                            text = { Text("${wh.name} (${wh.code})") },
                            onClick = {
                                selectedWhId = wh.id
                                whExpanded = false
                            }
                        )
                    }
                }
            }

            // Тоо хэмжээ
            OutlinedTextField(
                value = qtyString,
                onValueChange = { qtyString = it },
                label = { Text("Орлогын тоо хэмжээ (*)") },
                trailingIcon = { selectedItem?.let { Text(it.unit, modifier = Modifier.padding(end = 12.dp)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("incoming_qty_input")
            )

            // Нийлүүлэгч түнш
            OutlinedTextField(
                value = partner,
                onValueChange = { partner = it },
                label = { Text("Нийлүүлэгч (Байгууллага/Түнш)") },
                modifier = Modifier.fillMaxWidth().testTag("incoming_partner_input")
            )

            // Тэмдэглэл
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Тайлбар / Баримтын дугаар") },
                modifier = Modifier.fillMaxWidth()
            )

            // БҮРТГЭСЭН НЯРАВ
            OutlinedTextField(
                value = nurseName,
                onValueChange = { nurseName = it },
                label = { Text("Хүлээн авсан нярав") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val qty = qtyString.toDoubleOrNull() ?: 0.0
                    viewModel.recordIncoming(
                        itemId = selectedItemId,
                        quantity = qty,
                        warehouseId = selectedWhId,
                        supplierName = partner,
                        remarks = remarks,
                        performedBy = nurseName
                    )
                    // Маягтыг цэвэрлэх
                    qtyString = ""
                    partner = ""
                    remarks = ""
                },
                modifier = Modifier.fillMaxWidth().testTag("incoming_submit_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Орлого оруулах")
            }
        }
    }
}

// 2. Агуулахын шилжилт хөдөлгөөний маягт
@Composable
fun TransferForm(
    viewModel: InventoryViewModel,
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>
) {
    var selectedItemId by remember { mutableStateOf(0) }
    var qtyString by remember { mutableStateOf("") }
    var sourceWhId by remember { mutableStateOf(0) }
    var destWhId by remember { mutableStateOf(0) }
    var remarks by remember { mutableStateOf("") }
    var nurseName by remember { mutableStateOf("") }

    var itemExpanded by remember { mutableStateOf(false) }
    var srcWhExpanded by remember { mutableStateOf(false) }
    var destWhExpanded by remember { mutableStateOf(false) }

    val selectedItem = items.find { it.id == selectedItemId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Агуулах хооронд шилжилт хөдөлгөөн хийх", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            
            // Бараа сонгох
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { itemExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = selectedItem?.name ?: "Бараа сонгох (*)",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = itemExpanded,
                    onDismissRequest = { itemExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("${item.name} (${item.unit})") },
                            onClick = {
                                selectedItemId = item.id
                                itemExpanded = false
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Илгээх Агуулах
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { srcWhExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        val wh = warehouses.find { it.id == sourceWhId }
                        Text(text = wh?.name ?: "Илгээх агуулах (*)", fontSize = 12.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = srcWhExpanded,
                        onDismissRequest = { srcWhExpanded = false }
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                text = { Text(wh.name) },
                                onClick = {
                                    sourceWhId = wh.id
                                    srcWhExpanded = false
                                }
                            )
                        }
                    }
                }

                // Хөдөлгөөн заах сум
                Box(
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Хүлээн авах Агуулах
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { destWhExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        val wh = warehouses.find { it.id == destWhId }
                        Text(text = wh?.name ?: "Хүлээн авах (*)", fontSize = 12.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                    }
                    DropdownMenu(
                        expanded = destWhExpanded,
                        onDismissRequest = { destWhExpanded = false }
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                text = { Text(wh.name) },
                                onClick = {
                                    destWhId = wh.id
                                    destWhExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Тоо хэмжээ
            OutlinedTextField(
                value = qtyString,
                onValueChange = { qtyString = it },
                label = { Text("Шилжүүлэх тоо хэмжээ (*)") },
                trailingIcon = { selectedItem?.let { Text(it.unit, modifier = Modifier.padding(end = 12.dp)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Тэмдэглэл
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Шилжилтийн тайлбар") },
                modifier = Modifier.fillMaxWidth()
            )

            // БҮРТГЭСЭН НЯРАВ
            OutlinedTextField(
                value = nurseName,
                onValueChange = { nurseName = it },
                label = { Text("Гүйлгээ хийсэн нярав") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val qty = qtyString.toDoubleOrNull() ?: 0.0
                    viewModel.recordTransfer(
                        itemId = selectedItemId,
                        quantity = qty,
                        fromWarehouseId = sourceWhId,
                        toWarehouseId = destWhId,
                        remarks = remarks,
                        performedBy = nurseName
                    )
                    // Маягтыг цэвэрлэх
                    qtyString = ""
                    remarks = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Шилжүүлэлт бүртгэх")
            }
        }
    }
}

// 3. Зарлага гаргах маягт
@Composable
fun OutboundForm(
    viewModel: InventoryViewModel,
    items: List<ItemEntity>,
    warehouses: List<WarehouseEntity>
) {
    var selectedItemId by remember { mutableStateOf(0) }
    var qtyString by remember { mutableStateOf("") }
    var sourceWhId by remember { mutableStateOf(0) }
    var recipient by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var nurseName by remember { mutableStateOf("") }

    var itemExpanded by remember { mutableStateOf(false) }
    var srcWhExpanded by remember { mutableStateOf(false) }

    val selectedItem = items.find { it.id == selectedItemId }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Бараа материал гаргах (Зарлага)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            
            // Бараа сонгох
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { itemExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = selectedItem?.name ?: "Бараа сонгох (*)",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                DropdownMenu(
                    expanded = itemExpanded,
                    onDismissRequest = { itemExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { Text("${item.name} (${item.unit})") },
                            onClick = {
                                selectedItemId = item.id
                                itemExpanded = false
                            }
                        )
                    }
                }
            }

            // Агуулах сонгох (Хаанаас гаргах вэ)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { srcWhExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    val wh = warehouses.find { it.id == sourceWhId }
                    Text(text = wh?.name ?: "Аль агуулахаас гаргах вэ (*)")
                }
                DropdownMenu(
                    expanded = srcWhExpanded,
                    onDismissRequest = { srcWhExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    warehouses.forEach { wh ->
                        DropdownMenuItem(
                            text = { Text("${wh.name} (${wh.code})") },
                            onClick = {
                                sourceWhId = wh.id
                                srcWhExpanded = false
                            }
                        )
                    }
                }
            }

            // Тоо хэмжээ
            OutlinedTextField(
                value = qtyString,
                onValueChange = { qtyString = it },
                label = { Text("Зарлагын тоо хэмжээ (*)") },
                trailingIcon = { selectedItem?.let { Text(it.unit, modifier = Modifier.padding(end = 12.dp)) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Хүлээн авагч түнш
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("Хүлээн авагч (Ажилтан/Хэлтэс/Орон нутаг)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Тэмдэглэл
            OutlinedTextField(
                value = remarks,
                onValueChange = { remarks = it },
                label = { Text("Зарлагын зориулалт / Тайлбар") },
                modifier = Modifier.fillMaxWidth()
            )

            // БҮРТГЭСЭН НЯРАВ
            OutlinedTextField(
                value = nurseName,
                onValueChange = { nurseName = it },
                label = { Text("Зарлага бичсэн нярав") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    val qty = qtyString.toDoubleOrNull() ?: 0.0
                    viewModel.recordOutbound(
                        itemId = selectedItemId,
                        quantity = qty,
                        fromWarehouseId = sourceWhId,
                        recipientName = recipient,
                        remarks = remarks,
                        performedBy = nurseName
                    )
                    // Маягтыг цэвэрлэх
                    qtyString = ""
                    recipient = ""
                    remarks = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.VerticalAlignBottom, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Зарлага бүртгэх")
            }
        }
    }
}

// 4. Гүйлгээний түүхийн жагсаалт
@Composable
fun HistoryList(
    transactions: List<TransactionEntity>,
    txFilter: String,
    onFilterChange: (String) -> Unit,
    viewModel: InventoryViewModel
) {
    Column {
        // Шүүх хэсэг
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "Бүгд", "INCOMING" to "Орлого", "TRANSFER" to "Шилжилт", "OUTBOUND" to "Зарлага").forEach { (valType, label) ->
                val isSelected = txFilter == valType
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(valType) },
                    label = { Text(label, fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).testTag("tx_filter_$valType")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Ийм төрлийн гүйлгээний бүртгэл одоогоор байхгүй байна.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(transactions) { tx ->
                    TransactionStreamItem(tx = tx, viewModel = viewModel)
                }
            }
        }
    }
}

// ==================== SCREEN 4: AGUULAHUUD ====================
@Composable
fun WarehousesScreen(
    warehouses: List<WarehouseEntity>,
    itemsWithStock: List<ItemWithStock>,
    onAddWarehouseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Бүртгэлтэй агуулахууд (${warehouses.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Button(
                onClick = onAddWarehouseClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp).testTag("add_wh_btn")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Шинэ Агуулах", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (warehouses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Агуулах бүртгэлгүй байна. Шинээр нэмнэ үү.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(warehouses) { wh ->
                    // Агуулах тус бүр дэхь нийт барааны тоо хэмжээг тооцох
                    val activeStocksCount = itemsWithStock.filter { (it.stockByWarehouse[wh.id] ?: 0.0) != 0.0 }.size
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(0.5.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HomeWork,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = wh.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Код: ${wh.code}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Хариуцагч нярав:", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = wh.manager.ifBlank { "Тодорхойгүй" },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Үлдэгдэлтэй бараанууд:", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        text = "$activeStocksCount нэр төрөл",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== POP-UP DIAOLGS ====================

@Composable
fun AddItemDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (barcode: String, name: String, category: String, unit: String, desc: String, minQuantity: Double) -> Unit
) {
    var barcode by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("ш") } // Default unit
    var desc by remember { mutableStateOf("") }
    var minQtyString by remember { mutableStateOf("0.0") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Шинэ Бараа Материал Бүртгэх",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Divider()

                // Баркод
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Баркод / Хоёрдогч код") },
                    placeholder = { Text("Жишээ нь: 48600...") },
                    trailingIcon = {
                        IconButton(onClick = {
                            // Баркод сканердахын автомат загварчлал
                            barcode = "BC-" + (100000..999999).random()
                        }, modifier = Modifier.testTag("scan_simulate_btn")) {
                            Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_item_barcode")
                )

                // Нэр
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Барааны нэр (*)") },
                    modifier = Modifier.fillMaxWidth().testTag("add_item_name")
                )

                // Ангилал сонгох буюу бичих
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Ангилал / Бүлэг (*)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_item_category"),
                        trailingIcon = {
                            IconButton(onClick = { categoryDropdownExpanded = true }) {
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Хэмжих Нэгж
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Хэмжих нэгж (*)") },
                        placeholder = { Text("ш, кг, тонн, м") },
                        modifier = Modifier.weight(1f).testTag("add_item_unit")
                    )

                    // Доод үлдэгдэлтэй (Дохиолол заах)
                    OutlinedTextField(
                        value = minQtyString,
                        onValueChange = { minQtyString = it },
                        label = { Text("Доод үлдэгдэл") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.2f).testTag("add_item_min_qty")
                    )
                }

                // Тайлбар
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Нэмэлт тайлбар") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Болих")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val minQ = minQtyString.toDoubleOrNull() ?: 0.0
                            onConfirm(barcode, name, category, unit, desc, minQ)
                        },
                        modifier = Modifier.testTag("add_item_submit_btn")
                    ) {
                        Text("Бүртгэх")
                    }
                }
            }
        }
    }
}

@Composable
fun AddWarehouseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, manager: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var manager by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Шинэ Агуулах / Байршил Бүртгэх",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Divider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Агуулахын нэр (*)") },
                    placeholder = { Text("Жишээ: Зүүн салбар агуулах") },
                    modifier = Modifier.fillMaxWidth().testTag("add_wh_name")
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Агуулахын код (*)") },
                    placeholder = { Text("Жишээ: Wh04") },
                    modifier = Modifier.fillMaxWidth().testTag("add_wh_code")
                )

                OutlinedTextField(
                    value = manager,
                    onValueChange = { manager = it },
                    label = { Text("Хариуцагч ажилтан / Нярав") },
                    modifier = Modifier.fillMaxWidth().testTag("add_wh_manager")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Болих")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name, code, manager) },
                        modifier = Modifier.testTag("add_wh_submit_btn")
                    ) {
                        Text("Бүртгэх")
                    }
                }
            }
        }
    }
}

// Бид ScrollState-г хэрэглэх тул зөөвөрлөх туслах функцүүд:
@Composable
fun rememberScrollState(): androidx.compose.foundation.ScrollState {
    return androidx.compose.foundation.rememberScrollState()
}
