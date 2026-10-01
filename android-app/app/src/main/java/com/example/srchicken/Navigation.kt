package com.example.srchicken

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.srchicken.data.AppRepository
import com.example.srchicken.data.BillEntity
import com.example.srchicken.data.BillItemEntity
import com.example.srchicken.data.CustomerEntity
import com.example.srchicken.data.ProductEntity
import com.example.srchicken.data.SettingsEntity
import com.example.srchicken.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val factory = remember { AppViewModelFactory.create(context) }
    val backStack = remember { mutableStateListOf<Any>(DashboardRoute) }
    BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }

    val currentRoute = backStack.lastOrNull() ?: DashboardRoute

    Scaffold(
        containerColor = Surface1,
        bottomBar = {
            val topLevelRoutes = setOf(
                DashboardRoute::class, NewBillRoute::class,
                CustomersRoute::class, HistoryRoute::class
            )
            val isTopLevel = topLevelRoutes.any { cls -> cls.isInstance(currentRoute) }

            if (isTopLevel) {
                NavigationBar(
                    containerColor = Surface2,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = item.route::class.isInstance(currentRoute)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    backStack.clear()
                                    backStack.add(item.route)
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Indigo500,
                                selectedTextColor = Indigo500,
                                unselectedIconColor = OnSurface40,
                                unselectedTextColor = OnSurface40,
                                indicatorColor = Indigo500.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AnimatedContent(
                targetState = currentRoute,
                label = "ScreenTransition"
            ) { route ->
                when (route) {
                    is DashboardRoute -> {
                        val vm: DashboardViewModel = viewModel(factory = factory)
                        val state by vm.uiState.collectAsStateWithLifecycle()
                        DashboardScreen(
                            state = state,
                            onNewBill = { backStack.clear(); backStack.add(NewBillRoute) },
                            onViewBill = { backStack.add(BillDetailRoute(it)) },
                            onViewHistory = { backStack.clear(); backStack.add(HistoryRoute) },
                            onAddCustomer = { backStack.clear(); backStack.add(CustomersRoute) },
                            onSettings = { backStack.add(SettingsRoute) }
                        )
                    }
                    is NewBillRoute -> {
                        val vm: NewBillViewModel = viewModel(factory = factory)
                        val state by vm.state.collectAsStateWithLifecycle()
                        NewBillScreen(
                            state = state,
                            onSelectCustomer = vm::selectCustomer,
                            onAddItem = vm::addItem,
                            onRemoveItem = vm::removeItem,
                            onUpdateProduct = vm::updateItemProduct,
                            onUpdateQty = vm::updateItemQty,
                            onUpdateRate = vm::updateItemRate,
                            onUpdateDiscount = vm::updateDiscount,
                            onUpdateDiscountType = vm::updateDiscountType,
                            onUpdateBillDate = vm::updateBillDate,
                            onGenerate = { vm.generateBill { billId -> backStack.add(BillDetailRoute(billId)) } },
                            onAddCustomer = { backStack.add(CustomersRoute) },
                            onClearError = vm::clearError
                        )
                    }
                    is CustomersRoute -> {
                        val vm: CustomersViewModel = viewModel(factory = factory)
                        val state by vm.state.collectAsStateWithLifecycle()
                        val filtered by vm.filteredCustomers.collectAsStateWithLifecycle()
                        CustomersScreen(
                            state = state,
                            filteredCustomers = filtered,
                            onSearch = vm::setSearchQuery,
                            onOpenAdd = vm::openAddDialog,
                            onOpenEdit = vm::openEditDialog,
                            onDelete = vm::deleteCustomer,
                            onClose = vm::closeDialog,
                            onUpdateName = vm::updateName,
                            onUpdatePhone = vm::updatePhone,
                            onUpdateCustomerCode = vm::updateCustomerCode,
                            onUpdateOpeningBalance = vm::updateOpeningBalance,
                            onSave = vm::saveCustomer
                        )
                    }
                    is HistoryRoute -> {
                        val vm: HistoryViewModel = viewModel(factory = factory)
                        val state by vm.state.collectAsStateWithLifecycle()
                        val filtered by vm.filteredBills.collectAsStateWithLifecycle()
                        HistoryScreen(
                            state = state,
                            filteredBills = filtered,
                            onSearch = vm::setSearchQuery,
                            onFromDate = vm::setFromDate,
                            onToDate = vm::setToDate,
                            onClearFilters = vm::clearFilters,
                            onViewBill = { backStack.add(BillDetailRoute(it)) },
                            onConfirmDelete = vm::confirmDelete,
                            onCancelDelete = vm::cancelDelete,
                            onDelete = vm::deleteBill,
                            onGetItems = vm::getBillItems,
                            onGetBill = vm::getBillById
                        )
                    }
                    is SettingsRoute -> {
                        val vm: SettingsViewModel = viewModel(factory = factory)
                        val state by vm.state.collectAsStateWithLifecycle()
                        SettingsScreen(
                            state = state,
                            onBack = { backStack.removeLastOrNull() },
                            onUpdateName = vm::updateShopName,
                            onUpdatePhone = vm::updateShopPhone,
                            onUpdateAddress = vm::updateShopAddress,
                            onUpdateGstin = vm::updateShopGstin,
                            onUpdateTaxPercentage = vm::updateTaxPercentage,
                            onSave = vm::saveSettings,
                            onClearSaved = vm::clearSavedFlag,
                            onNewProduct = vm::updateNewProductName,
                            onNewProductUnit = vm::updateNewProductUnit,
                            onAddProduct = vm::addProduct,
                            onToggleProduct = vm::toggleProduct,
                            onDeleteProduct = vm::deleteProduct,
                            onShowClear = vm::showClearConfirm,
                            onHideClear = vm::hideClearConfirm,
                            onClearAll = vm::clearAllData
                        )
                    }
                    is BillDetailRoute -> {
                        val vm: HistoryViewModel = viewModel(factory = factory)
                        BillDetailScreen(
                            billId = route.billId,
                            onBack = { backStack.removeLastOrNull() },
                            getItems = vm::getBillItems,
                            getBill = vm::getBillById,
                            onUpdateDate = vm::updateBillDate
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

// ============================================================
//  DASHBOARD SCREEN
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onNewBill: () -> Unit,
    onViewBill: (Long) -> Unit,
    onViewHistory: () -> Unit,
    onAddCustomer: () -> Unit,
    onSettings: () -> Unit
) {
    val today = remember { java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale.getDefault()).format(java.util.Date()) }

    Scaffold(
        containerColor = Surface1,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SR Billing", style = MaterialTheme.typography.titleLarge, color = Indigo500, fontWeight = FontWeight.ExtraBold)
                        Text(today, style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = OnSurface60)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewBill,
                containerColor = Indigo500,
                contentColor = Neutral900
            ) {
                Icon(Icons.Filled.Add, "New Bill")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats grid
            item {
                Text("Overview", style = MaterialTheme.typography.titleMedium, color = OnSurface, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "Today's Bills", state.stats.todayBillCount.toString(), Icons.Filled.Receipt, Indigo500)
                    StatCard(Modifier.weight(1f), "Today's Revenue", Formatters.currency(state.stats.todayRevenue), Icons.Filled.CurrencyRupee, Success)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "Total Customers", state.stats.totalCustomers.toString(), Icons.Filled.Group, Info)
                    StatCard(Modifier.weight(1f), "Total Revenue", Formatters.currency(state.stats.totalRevenue), Icons.Filled.BarChart, Indigo600)
                }
            }

            // Quick Actions
            item {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, color = OnSurface, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionButton(Modifier.weight(1f), "New Bill", Icons.Filled.AddCircle, onClick = onNewBill)
                    QuickActionButton(Modifier.weight(1f), "Add Customer", Icons.Filled.PersonAdd, onClick = onAddCustomer)
                    QuickActionButton(Modifier.weight(1f), "History", Icons.Filled.History, onClick = onViewHistory)
                }
            }

            // Recent Bills
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Bills", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    TextButton(onClick = onViewHistory) {
                        Text("See All", color = Indigo500, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (state.recentBills.isEmpty()) {
                item { EmptyState("No bills yet", "Tap + to create your first bill", Icons.Outlined.Receipt) }
            } else {
                items(state.recentBills, key = { it.id }) { bill ->
                    BillListCard(bill = bill, onClick = { onViewBill(bill.id) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accentColor: Color) {
    Surface(modifier = modifier, color = Surface2, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DividerColor)) {
        Box(Modifier.padding(16.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(16.dp))
                    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                }
                Spacer(Modifier.height(8.dp))
                Text(value, style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun QuickActionButton(modifier: Modifier, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = Surface2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, null, tint = Indigo500, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = OnSurface60, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BillListCard(bill: BillEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Surface2,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Indigo500.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Receipt, null, tint = Indigo500, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(bill.billNumber, style = MaterialTheme.typography.labelLarge, color = Indigo500, fontWeight = FontWeight.SemiBold)
                    Text(bill.customerName, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    Text(Formatters.dateTime(bill.createdAt), style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                }
            }
            Text(Formatters.currency(bill.total), style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = OnSurface40, modifier = Modifier.size(48.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface60)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurface40, textAlign = TextAlign.Center)
    }
}

// ============================================================
//  NEW BILL SCREEN
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBillScreen(
    state: NewBillUiState,
    onSelectCustomer: (Long) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onUpdateProduct: (String, Long) -> Unit,
    onUpdateQty: (String, String) -> Unit,
    onUpdateRate: (String, String) -> Unit,
    onUpdateDiscount: (String) -> Unit,
    onUpdateDiscountType: (String) -> Unit,
    onUpdateBillDate: (Long) -> Unit,
    onGenerate: () -> Unit,
    onAddCustomer: () -> Unit,
    onClearError: () -> Unit
) {
    val context = LocalContext.current
    if (state.error != null) {
        LaunchedEffect(state.error) {
            Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
            onClearError()
        }
    }

    var customerExpanded by remember { mutableStateOf(false) }
    var discountTypeExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Surface1,
        topBar = {
            TopAppBar(
                title = { Text("New Bill", style = MaterialTheme.typography.titleLarge, color = OnSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
            )
        },
        bottomBar = {
            Surface(color = Surface2, tonalElevation = 0.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Amount due", style = MaterialTheme.typography.labelMedium, color = OnSurface60)
                        Text(Formatters.currency(state.grandTotal), style = MaterialTheme.typography.titleLarge, color = Indigo500, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = onGenerate,
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Neutral900),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Neutral900, strokeWidth = 2.dp)
                        else Text("Generate Bill", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer section
            item {
                SectionCard(title = "Customer") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = customerExpanded,
                            onExpandedChange = { customerExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = state.selectedCustomer?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = { Text("Select customer", color = OnSurface40) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(customerExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                colors = srTextFieldColors(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = customerExpanded,
                                onDismissRequest = { customerExpanded = false },
                                containerColor = Surface3
                            ) {
                                state.customers.forEach { customer ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(customer.name, color = OnSurface, style = MaterialTheme.typography.bodyMedium)
                                                if (customer.phone.isNotBlank())
                                                    Text(customer.phone, color = OnSurface60, style = MaterialTheme.typography.labelSmall)
                                            }
                                        },
                                        onClick = { onSelectCustomer(customer.id); customerExpanded = false }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onAddCustomer,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface3)
                        ) {
                            Icon(Icons.Filled.PersonAdd, "Add Customer", tint = Indigo500)
                        }
                    }
                    if (state.selectedCustomer != null) {
                        Spacer(Modifier.height(8.dp))
                        Surface(color = Indigo500.copy(alpha = 0.08f), shape = RoundedCornerShape(8.dp)) {
                            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, null, tint = Indigo500, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(state.selectedCustomer.name, style = MaterialTheme.typography.labelLarge, color = OnSurface)
                                    if (state.selectedCustomer.customerCode.isNotBlank())
                                        Text("Customer ID: ${state.selectedCustomer.customerCode}", style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                                    if (state.selectedCustomer.phone.isNotBlank())
                                        Text(state.selectedCustomer.phone, style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Bill date") {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = state.billDate }
                            DatePickerDialog(context, { _, year, month, day ->
                                cal.set(year, month, day)
                                onUpdateBillDate(cal.timeInMillis)
                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface),
                        border = BorderStroke(1.dp, DividerColor),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(Formatters.date(state.billDate))
                    }
                }
            }

            // Items section
            item {
                SectionCard(title = "Items") {
                    state.items.forEachIndexed { index, item ->
                        BillItemRow(
                            item = item,
                            index = index,
                            products = state.products,
                            canRemove = state.items.size > 1,
                            onProductChange = { onUpdateProduct(item.id, it) },
                            onQtyChange = { onUpdateQty(item.id, it) },
                            onRateChange = { onUpdateRate(item.id, it) },
                            onRemove = { onRemoveItem(item.id) }
                        )
                        if (index < state.items.size - 1) {
                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onAddItem,
                        colors = ButtonDefaults.textButtonColors(contentColor = Indigo500)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Item", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Summary section
            item {
                SectionCard(title = "Summary") {
                    SummaryRow("Subtotal", Formatters.currency(state.subtotal))
                    Spacer(Modifier.height(8.dp))
                    // Discount row
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Discount", style = MaterialTheme.typography.bodyMedium, color = OnSurface60, modifier = Modifier.width(80.dp))
                        OutlinedTextField(
                            value = state.discountValue,
                            onValueChange = onUpdateDiscount,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = srTextFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = discountTypeExpanded,
                            onExpandedChange = { discountTypeExpanded = it },
                            modifier = Modifier.width(80.dp)
                        ) {
                            OutlinedTextField(
                                value = if (state.discountType == "flat") "₹" else "%",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(discountTypeExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                colors = srTextFieldColors(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = discountTypeExpanded,
                                onDismissRequest = { discountTypeExpanded = false },
                                containerColor = Surface3
                            ) {
                                DropdownMenuItem(text = { Text("₹ Flat", color = OnSurface) }, onClick = { onUpdateDiscountType("flat"); discountTypeExpanded = false })
                                DropdownMenuItem(text = { Text("% Percent", color = OnSurface) }, onClick = { onUpdateDiscountType("percent"); discountTypeExpanded = false })
                            }
                        }
                        Text("-${Formatters.currency(state.discountAmount)}", color = Danger, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (state.taxAmount > 0) {
                        SummaryRow("Tax (${state.taxPercentage}%)", Formatters.currency(state.taxAmount))
                        Spacer(Modifier.height(8.dp))
                    }
                    if (state.openingBalance != 0.0) {
                        SummaryRow("Previous balance", Formatters.currency(state.openingBalance))
                        Spacer(Modifier.height(8.dp))
                    }
                    HorizontalDivider(color = DividerColor)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount due", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                        Text(Formatters.currency(state.grandTotal), style = MaterialTheme.typography.titleMedium, color = Indigo500, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillItemRow(
    item: BillItem,
    index: Int,
    products: List<ProductEntity>,
    canRemove: Boolean,
    onProductChange: (Long) -> Unit,
    onQtyChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var productExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Item ${index + 1}", style = MaterialTheme.typography.labelMedium, color = OnSurface60)
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, "Remove", tint = Danger, modifier = Modifier.size(16.dp))
                }
            }
        }
        ExposedDropdownMenuBox(expanded = productExpanded, onExpandedChange = { productExpanded = it }) {
            OutlinedTextField(
                value = item.productName.ifBlank { "" },
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select product", color = OnSurface40) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(productExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                colors = srTextFieldColors(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                label = { Text("Product") }
            )
            ExposedDropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }, containerColor = Surface3) {
                products.forEach { product ->
                    DropdownMenuItem(
                        text = { Text(product.name, color = OnSurface) },
                        onClick = { onProductChange(product.id); productExpanded = false }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = item.qty,
                onValueChange = onQtyChange,
                label = { Text("Qty") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = srTextFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )
            OutlinedTextField(
                value = item.rate,
                onValueChange = onRateChange,
                label = { Text("Rate (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = srTextFieldColors(),
                shape = RoundedCornerShape(8.dp)
            )
        }
        if (item.amount > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("= ${Formatters.currency(item.amount)}", style = MaterialTheme.typography.labelLarge, color = Indigo500, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurface60)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Surface2, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DividerColor)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun srTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Indigo500,
    unfocusedBorderColor = Surface4,
    focusedLabelColor = Indigo500,
    unfocusedLabelColor = OnSurface60,
    cursorColor = Indigo500,
    focusedTextColor = OnSurface,
    unfocusedTextColor = OnSurface,
    focusedContainerColor = Surface3,
    unfocusedContainerColor = Surface3,
    focusedPlaceholderColor = OnSurface40,
    unfocusedPlaceholderColor = OnSurface40
)

// ============================================================
//  CUSTOMERS SCREEN
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    state: CustomersUiState,
    filteredCustomers: List<CustomerEntity>,
    onSearch: (String) -> Unit,
    onOpenAdd: () -> Unit,
    onOpenEdit: (CustomerEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onClose: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdatePhone: (String) -> Unit,
    onUpdateCustomerCode: (String) -> Unit,
    onUpdateOpeningBalance: (String) -> Unit,
    onSave: () -> Unit
) {
    if (state.showAddDialog) {
        CustomerDialog(
            title = if (state.editingCustomer != null) "Edit Customer" else "Add Customer",
            name = state.nameInput,
            phone = state.phoneInput,
            customerCode = state.customerCodeInput,
            openingBalance = state.openingBalanceInput,
            error = state.inputError,
            onName = onUpdateName,
            onPhone = onUpdatePhone,
            onCustomerCode = onUpdateCustomerCode,
            onOpeningBalance = onUpdateOpeningBalance,
            onSave = onSave,
            onDismiss = onClose
        )
    }

    Scaffold(
        containerColor = Surface1,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Customers", style = MaterialTheme.typography.titleLarge, color = OnSurface)
                        Text("${state.customers.size} total", style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenAdd, containerColor = Indigo500, contentColor = Neutral900) {
                Icon(Icons.Filled.PersonAdd, "Add Customer")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                placeholder = { Text("Search name, phone or customer ID…", color = OnSurface40) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = OnSurface40) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearch("") }) { Icon(Icons.Filled.Close, null, tint = OnSurface40) }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = srTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            if (filteredCustomers.isEmpty()) {
                EmptyState(
                    if (state.searchQuery.isBlank()) "No customers yet" else "No results",
                    if (state.searchQuery.isBlank()) "Tap + to add your first customer" else "Try a different search",
                    Icons.Outlined.Group
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerListCard(customer = customer, onEdit = { onOpenEdit(customer) }, onDelete = { onDelete(customer.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun CustomerListCard(customer: CustomerEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Surface2, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, DividerColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Indigo600),
                    contentAlignment = Alignment.Center
                ) {
                    Text(Formatters.initials(customer.name), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column {
                    Text(customer.name, style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.SemiBold)
                    val detail = listOf(customer.customerCode.ifBlank { "" }, customer.phone.ifBlank { "No phone" }).filter { it.isNotBlank() }.joinToString("  •  ")
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = OnSurface60)
                    if (customer.openingBalance != 0.0) Text("Opening balance: ${Formatters.currency(customer.openingBalance)}", style = MaterialTheme.typography.labelSmall, color = Danger)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Edit, "Edit", tint = OnSurface60, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = Danger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CustomerDialog(
    title: String,
    name: String,
    phone: String,
    customerCode: String,
    openingBalance: String,
    error: String?,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onCustomerCode: (String) -> Unit,
    onOpeningBalance: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        title = { Text(title, style = MaterialTheme.typography.titleMedium, color = OnSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onName,
                    label = { Text("Name *") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error, color = Danger) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = srTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = customerCode,
                    onValueChange = onCustomerCode,
                    label = { Text("Customer ID / restaurant code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = srTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = openingBalance,
                    onValueChange = onOpeningBalance,
                    label = { Text("Opening balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = srTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhone,
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = srTextFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Neutral900), shape = RoundedCornerShape(10.dp)) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = OnSurface60)) {
                Text("Cancel")
            }
        }
    )
}

// ============================================================
//  HISTORY SCREEN
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    filteredBills: List<BillEntity>,
    onSearch: (String) -> Unit,
    onFromDate: (Long?) -> Unit,
    onToDate: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onViewBill: (Long) -> Unit,
    onConfirmDelete: (Long) -> Unit,
    onCancelDelete: () -> Unit,
    onDelete: (Long) -> Unit,
    onGetItems: suspend (Long) -> List<BillItemEntity>,
    onGetBill: suspend (Long) -> BillEntity?
) {
    val context = LocalContext.current
    val filteredTotal = filteredBills.sumOf { it.total }

    if (state.showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = onCancelDelete,
            containerColor = Surface2,
            title = { Text("Delete Bill?", color = OnSurface) },
            text = { Text("This action cannot be undone.", color = OnSurface60) },
            confirmButton = {
                Button(onClick = { onDelete(state.showDeleteConfirm) },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = onCancelDelete, colors = ButtonDefaults.textButtonColors(contentColor = OnSurface60)) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = Surface1,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Bill History", style = MaterialTheme.typography.titleLarge, color = OnSurface)
                        Text("${state.allBills.size} total bills", style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                    }
                },
                actions = {
                    if (state.searchQuery.isNotBlank() || state.fromDate != null || state.toDate != null) {
                        TextButton(onClick = onClearFilters) { Text("Clear", color = Indigo500) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                placeholder = { Text("Search bill no. or customer…", color = OnSurface40) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = OnSurface40) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearch("") }) { Icon(Icons.Filled.Close, null, tint = OnSurface40) }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = srTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Date filters
            Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateFilterChip(
                    label = if (state.fromDate != null) "From: ${Formatters.shortDate(state.fromDate)}" else "From date",
                    selected = state.fromDate != null,
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            cal.set(y, m, d); onFromDate(cal.timeInMillis)
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    onClear = { onFromDate(null) },
                    modifier = Modifier.weight(1f)
                )
                DateFilterChip(
                    label = if (state.toDate != null) "To: ${Formatters.shortDate(state.toDate)}" else "To date",
                    selected = state.toDate != null,
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            cal.set(y, m, d); onToDate(cal.timeInMillis)
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    onClear = { onToDate(null) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Summary bar
            if (filteredBills.isNotEmpty()) {
                Surface(color = Surface2, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${filteredBills.size} bills", style = MaterialTheme.typography.labelMedium, color = OnSurface60)
                        Text("Total: ${Formatters.currency(filteredTotal)}", style = MaterialTheme.typography.labelMedium, color = Indigo500, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (filteredBills.isEmpty()) {
                EmptyState("No bills found", "Try adjusting your filters", Icons.Outlined.SearchOff)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredBills, key = { it.id }) { bill ->
                        HistoryBillCard(bill = bill, onClick = { onViewBill(bill.id) }, onDelete = { onConfirmDelete(bill.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun DateFilterChip(label: String, selected: Boolean, onClick: () -> Unit, onClear: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = if (selected) {
            { IconButton(onClick = onClear, modifier = Modifier.size(16.dp)) { Icon(Icons.Filled.Close, null, modifier = Modifier.size(12.dp)) } }
        } else null,
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Indigo500.copy(alpha = 0.15f),
            selectedLabelColor = Indigo500,
            selectedLeadingIconColor = Indigo500,
            containerColor = Surface3,
            labelColor = OnSurface60
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = DividerColor, selectedBorderColor = Indigo500.copy(alpha = 0.3f)
        )
    )
}

@Composable
fun HistoryBillCard(bill: BillEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(color = Surface2, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, DividerColor), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.clickable(onClick = onClick).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Indigo500.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Receipt, null, tint = Indigo500, modifier = Modifier.size(20.dp)) }
                Column(modifier = Modifier.weight(1f)) {
                    Text(bill.billNumber, style = MaterialTheme.typography.labelLarge, color = Indigo500, fontWeight = FontWeight.SemiBold)
                    Text(bill.customerName, style = MaterialTheme.typography.bodyMedium, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(Formatters.dateTime(bill.createdAt), style = MaterialTheme.typography.labelSmall, color = OnSurface60)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Formatters.currency(bill.total), style = MaterialTheme.typography.titleSmall, color = OnSurface, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Delete, "Delete", tint = Danger, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ============================================================
//  SETTINGS SCREEN
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdatePhone: (String) -> Unit,
    onUpdateAddress: (String) -> Unit,
    onUpdateGstin: (String) -> Unit,
    onUpdateTaxPercentage: (String) -> Unit,
    onSave: () -> Unit,
    onClearSaved: () -> Unit,
    onNewProduct: (String) -> Unit,
    onNewProductUnit: (String) -> Unit,
    onAddProduct: () -> Unit,
    onToggleProduct: (ProductEntity) -> Unit,
    onDeleteProduct: (Long) -> Unit,
    onShowClear: () -> Unit,
    onHideClear: () -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
            onClearSaved()
        }
    }

    if (state.showClearConfirm) {
        AlertDialog(
            onDismissRequest = onHideClear,
            containerColor = Surface2,
            title = { Text("Clear All Data?", color = Danger) },
            text = { Text("This will permanently delete all bills and customers. This cannot be undone.", color = OnSurface60) },
            confirmButton = {
                Button(onClick = onClearAll, colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White), shape = RoundedCornerShape(10.dp)) { Text("Delete All") }
            },
            dismissButton = {
                TextButton(onClick = onHideClear, colors = ButtonDefaults.textButtonColors(contentColor = OnSurface60)) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = Surface1,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, color = OnSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface60) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Shop details
            item {
                SectionCard(title = "Shop Details") {
                    Text("These details appear on generated bills.", style = MaterialTheme.typography.bodySmall, color = OnSurface60)
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SrTextField("Shop Name", state.shopName, onUpdateName)
                        SrTextField("Phone", state.shopPhone, onUpdatePhone, KeyboardType.Phone)
                        SrTextField("Address", state.shopAddress, onUpdateAddress, singleLine = false)
                        SrTextField("GSTIN (optional)", state.shopGstin, onUpdateGstin)
                        SrTextField("Default Tax (%)", state.taxPercentage, onUpdateTaxPercentage, KeyboardType.Decimal)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo500, contentColor = Neutral900),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Save Details", fontWeight = FontWeight.Bold) }
                }
            }

            // Products
            item {
                SectionCard(title = "Products") {
                    Text("Manage available products for billing.", style = MaterialTheme.typography.bodySmall, color = OnSurface60)
                    Spacer(Modifier.height(12.dp))
                    if (state.products.isEmpty()) {
                        Text("No products. Add one below.", style = MaterialTheme.typography.bodySmall, color = OnSurface40, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        state.products.forEach { product ->
                            ProductListItem(product = product, onToggle = { onToggleProduct(product) }, onDelete = { onDeleteProduct(product.id) })
                            if (product != state.products.last()) HorizontalDivider(color = DividerColor)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    var unitExpanded by remember { mutableStateOf(false) }
                    val unitOptions = listOf("pcs", "kg", "g", "ltr", "ml", "m", "ft", "box", "set")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.newProductName,
                            onValueChange = onNewProduct,
                            placeholder = { Text("Product name", color = OnSurface40) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = srTextFieldColors(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = it },
                            modifier = Modifier.width(90.dp)
                        ) {
                            OutlinedTextField(
                                value = state.newProductUnit,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Unit") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                colors = srTextFieldColors(),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false },
                                containerColor = Surface3
                            ) {
                                unitOptions.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u, color = OnSurface) },
                                        onClick = { onNewProductUnit(u); unitExpanded = false }
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onAddProduct,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Indigo500)
                        ) { Icon(Icons.Filled.Add, "Add Product", tint = Neutral900) }
                    }
                }
            }

            // Data management
            item {
                SectionCard(title = "Data Management") {
                    Text("Export or clear your billing data.", style = MaterialTheme.typography.bodySmall, color = OnSurface60)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onShowClear,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Danger.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
                    ) {
                        Icon(Icons.Outlined.DeleteForever, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Clear All Data", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Version info
            item {
                Text("SR Billing  v1.0", style = MaterialTheme.typography.labelSmall, color = OnSurface40, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun SrTextField(label: String, value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        colors = srTextFieldColors(),
        shape = RoundedCornerShape(10.dp),
        minLines = if (!singleLine) 3 else 1,
        maxLines = if (!singleLine) 5 else 1
    )
}

@Composable
fun ProductListItem(product: ProductEntity, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
            Text("${if (product.active) "Active" else "Disabled"} · ${product.unit}", style = MaterialTheme.typography.labelSmall, color = if (product.active) Success else OnSurface40)
        }
        Row {
            TextButton(onClick = onToggle, colors = ButtonDefaults.textButtonColors(contentColor = if (product.active) OnSurface60 else Indigo500)) {
                Text(if (product.active) "Disable" else "Enable", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Delete, "Remove", tint = Danger, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ============================================================
//  BILL DETAIL SCREEN
// ============================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    billId: Long,
    onBack: () -> Unit,
    getItems: suspend (Long) -> List<BillItemEntity>,
    getBill: suspend (Long) -> BillEntity?,
    onUpdateDate: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bill by remember { mutableStateOf<BillEntity?>(null) }
    var items by remember { mutableStateOf<List<BillItemEntity>>(emptyList()) }
    var settings by remember { mutableStateOf(SettingsEntity()) }
    var editingDate by remember { mutableStateOf(false) }

    LaunchedEffect(billId) {
        bill = getBill(billId)
        items = getItems(billId)
        settings = AppRepository.getInstance(context).getSettingsOnce()
    }

    Scaffold(
        containerColor = Surface1,
        topBar = {
            TopAppBar(
                title = { Text(bill?.billNumber ?: "Bill", style = MaterialTheme.typography.titleLarge, color = OnSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = OnSurface60) }
                },
                actions = {
                    bill?.let { b ->
                        IconButton(onClick = {
                            scope.launch { PdfGenerator.generateAndShare(context, b, items, settings) }
                        }) {
                            Icon(Icons.Filled.Share, "Share Bill", tint = Indigo500)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
            )
        }
    ) { padding ->
        if (bill == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Indigo500)
            }
        } else {
            val b = bill!!
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Shop header
                item {
                    Surface(color = Surface2, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DividerColor), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(settings.shopName.ifBlank { "SR Billing" }, style = MaterialTheme.typography.titleLarge, color = Indigo500, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                            val details = listOfNotNull(settings.shopPhone.ifBlank { null }, settings.shopAddress.ifBlank { null }).joinToString(" · ")
                            if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = OnSurface60, textAlign = TextAlign.Center)
                            if (settings.shopGstin.isNotBlank()) Text("GSTIN: ${settings.shopGstin}", style = MaterialTheme.typography.labelSmall, color = OnSurface40, textAlign = TextAlign.Center)
                        }
                    }
                }

                // Bill info grid
                item {
                    Surface(color = Surface2, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DividerColor)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BillInfoItem("Bill Number", b.billNumber)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BillInfoItem("Date", Formatters.date(b.createdAt), align = TextAlign.End)
                                    IconButton(onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = b.createdAt }
                                        DatePickerDialog(context, { _, year, month, day ->
                                            cal.set(year, month, day)
                                            onUpdateDate(b.id, cal.timeInMillis)
                                            bill = b.copy(createdAt = cal.timeInMillis)
                                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Outlined.EditCalendar, "Change bill date", tint = Indigo500, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = DividerColor)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BillInfoItem("Customer", b.customerName)
                                BillInfoItem("Phone", b.customerPhone.ifBlank { "—" }, align = TextAlign.End)
                            }
                            if (b.customerCode.isNotBlank()) {
                                HorizontalDivider(color = DividerColor)
                                BillInfoItem("Customer ID", b.customerCode)
                            }
                        }
                    }
                }

                // Items
                item {
                    SectionCard(title = "Items") {
                        // Table header
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Item", style = MaterialTheme.typography.labelSmall, color = OnSurface40, modifier = Modifier.weight(2f))
                            Text("Qty", style = MaterialTheme.typography.labelSmall, color = OnSurface40, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Rate", style = MaterialTheme.typography.labelSmall, color = OnSurface40, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("Amount", style = MaterialTheme.typography.labelSmall, color = OnSurface40, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        }
                        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
                        items.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(item.productName, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(2f))
                                Text("${item.qty}", style = MaterialTheme.typography.bodySmall, color = OnSurface60, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(Formatters.currency(item.rate), style = MaterialTheme.typography.bodySmall, color = OnSurface60, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(Formatters.currency(item.amount), style = MaterialTheme.typography.labelLarge, color = OnSurface, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Totals
                item {
                    SectionCard(title = "Totals") {
                        SummaryRow("Subtotal", Formatters.currency(b.subtotal))
                        if (b.discountAmount > 0) {
                            Spacer(Modifier.height(4.dp))
                            val discLabel = if (b.discountType == "percent") "Discount (${b.discountValue.toInt()}%)" else "Discount"
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(discLabel, style = MaterialTheme.typography.bodyMedium, color = OnSurface60)
                                Text("-${Formatters.currency(b.discountAmount)}", color = Danger, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (b.taxAmount > 0) {
                            SummaryRow("Tax (${b.taxPercentage}%)", Formatters.currency(b.taxAmount))
                            Spacer(Modifier.height(8.dp))
                        }
                        if (b.openingBalance != 0.0) {
                            SummaryRow("Previous balance", Formatters.currency(b.openingBalance))
                            Spacer(Modifier.height(8.dp))
                        }
                        HorizontalDivider(color = DividerColor)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount due", style = MaterialTheme.typography.titleMedium, color = OnSurface, fontWeight = FontWeight.Bold)
                            Text(Formatters.currency(b.totalDue), style = MaterialTheme.typography.titleMedium, color = Indigo500, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                // Share button
                item {
                    Button(
                        onClick = { scope.launch { PdfGenerator.generateAndShareWhatsApp(context, b, items, settings) } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = Neutral900),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share on WhatsApp", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { scope.launch { PdfGenerator.generateAndShare(context, b, items, settings) } },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OnSurface),
                        border = BorderStroke(1.dp, DividerColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share or download PDF", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun BillInfoItem(label: String, value: String, align: TextAlign = TextAlign.Start) {
    Column(horizontalAlignment = if (align == TextAlign.End) Alignment.End else Alignment.Start) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurface40)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = OnSurface, fontWeight = FontWeight.SemiBold, textAlign = align)
    }
}
