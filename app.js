// =========================================
// SR CHICKEN — BILLING APP
// Core Application Logic
// =========================================

// ---- DEFAULT DATA ----
const DEFAULT_PRODUCTS = [
  { id: 'p1', name: 'Whole Chicken', unit: 'kg', active: true },
  { id: 'p2', name: 'Boneless', unit: 'kg', active: true },
  { id: 'p3', name: 'Grill (with skin)', unit: 'kg', active: true },
  { id: 'p4', name: 'Biryani (big piece)', unit: 'kg', active: true },
];

const DEFAULT_SETTINGS = {
  shopName: 'SR Chicken',
  shopPhone: '',
  shopAddress: '',
  shopGstin: '',
};

// ---- DATA LAYER (localStorage) ----
const Storage = {
  get(key, fallback = null) {
    try {
      const val = localStorage.getItem(`sr_${key}`);
      return val ? JSON.parse(val) : fallback;
    } catch { return fallback; }
  },
  set(key, value) {
    localStorage.setItem(`sr_${key}`, JSON.stringify(value));
  },
  remove(key) {
    localStorage.removeItem(`sr_${key}`);
  }
};

function getCustomers() { return Storage.get('customers', []); }
function saveCustomers(list) { Storage.set('customers', list); }

function getBills() { return Storage.get('bills', []); }
function saveBills(list) { Storage.set('bills', list); }

function getProducts() { return Storage.get('products', DEFAULT_PRODUCTS); }
function saveProducts(list) { Storage.set('products', list); }

function getSettings() { return Storage.get('settings', { ...DEFAULT_SETTINGS }); }
function saveSettingsData(s) { Storage.set('settings', s); }

function getLastPrices() { return Storage.get('lastPrices', {}); }
function saveLastPrices(p) { Storage.set('lastPrices', p); }

// ---- UTILITIES ----
function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
}

function generateBillNumber() {
  const now = new Date();
  const dateStr = now.getFullYear().toString() +
    String(now.getMonth() + 1).padStart(2, '0') +
    String(now.getDate()).padStart(2, '0');

  const bills = getBills();
  const todayBills = bills.filter(b => b.billNumber && b.billNumber.includes(dateStr));
  const seq = todayBills.length + 1;

  return `SR-${dateStr}-${String(seq).padStart(3, '0')}`;
}

function formatCurrency(amount) {
  return '₹' + Number(amount).toLocaleString('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
}

function formatDate(dateStr) {
  const d = new Date(dateStr);
  return d.toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric'
  });
}

function formatTime(dateStr) {
  const d = new Date(dateStr);
  return d.toLocaleTimeString('en-IN', {
    hour: '2-digit', minute: '2-digit', hour12: true
  });
}

function formatDateTime(dateStr) {
  return formatDate(dateStr) + ', ' + formatTime(dateStr);
}

function getInitials(name) {
  return name.split(' ').map(w => w[0]).slice(0, 2).join('').toUpperCase();
}

// ---- TOAST NOTIFICATIONS ----
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  const icons = { success: '✓', error: '✕', info: 'ℹ' };
  toast.innerHTML = `<span>${icons[type] || 'ℹ'}</span><span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.animation = 'toastSlideOut 0.3s ease forwards';
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}

// ---- ROUTER ----
let currentView = 'dashboard';
let currentBillForPreview = null;

function navigateTo(view) {
  currentView = view;
  window.location.hash = view;

  // Update nav active states
  document.querySelectorAll('.nav-link, .mobile-nav-link').forEach(link => {
    link.classList.toggle('active', link.dataset.view === view);
  });

  // Show/hide views
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  const viewEl = document.getElementById(`view-${view}`);
  if (viewEl) viewEl.classList.add('active');

  // Render view content
  switch (view) {
    case 'dashboard': renderDashboard(); break;
    case 'new-bill': renderNewBill(); break;
    case 'customers': renderCustomers(); break;
    case 'history': renderHistory(); break;
    case 'settings': renderSettings(); break;
  }
}

function handleHashChange() {
  const hash = window.location.hash.replace('#', '') || 'dashboard';
  navigateTo(hash);
}

// ---- DASHBOARD ----
function renderDashboard() {
  // Date
  const dateEl = document.getElementById('dashboard-date');
  const now = new Date();
  dateEl.textContent = now.toLocaleDateString('en-IN', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  });

  // Stats
  const bills = getBills();
  const customers = getCustomers();
  const today = new Date().toDateString();
  const todayBills = bills.filter(b => new Date(b.createdAt).toDateString() === today);
  const todayRevenue = todayBills.reduce((s, b) => s + b.total, 0);
  const totalRevenue = bills.reduce((s, b) => s + b.total, 0);

  const statsHtml = `
    <div class="stat-card" style="--stat-accent: #f59e0b;">
      <div class="stat-label">Today's Bills</div>
      <div class="stat-value">${todayBills.length}</div>
      <div class="stat-icon">📄</div>
    </div>
    <div class="stat-card" style="--stat-accent: #10b981;">
      <div class="stat-label">Today's Revenue</div>
      <div class="stat-value">${formatCurrency(todayRevenue)}</div>
      <div class="stat-icon">💰</div>
    </div>
    <div class="stat-card" style="--stat-accent: #3b82f6;">
      <div class="stat-label">Total Customers</div>
      <div class="stat-value">${customers.length}</div>
      <div class="stat-icon">👥</div>
    </div>
    <div class="stat-card" style="--stat-accent: #8b5cf6;">
      <div class="stat-label">Total Revenue</div>
      <div class="stat-value">${formatCurrency(totalRevenue)}</div>
      <div class="stat-icon">📊</div>
    </div>
  `;
  document.getElementById('dashboard-stats').innerHTML = statsHtml;

  // Recent Bills
  const recentBills = [...bills].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5);
  const recentContainer = document.getElementById('recent-bills-container');

  if (recentBills.length === 0) {
    recentContainer.innerHTML = `
      <div class="empty-state">
        <span class="empty-icon">📄</span>
        <p>No bills yet. Create your first bill!</p>
      </div>
    `;
  } else {
    recentContainer.innerHTML = `
      <table class="data-table">
        <thead>
          <tr>
            <th>Bill No.</th>
            <th>Customer</th>
            <th>Date</th>
            <th>Amount</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${recentBills.map(b => `
            <tr>
              <td><span class="badge badge-amber">${b.billNumber}</span></td>
              <td>${b.customerName}</td>
              <td>${formatDateTime(b.createdAt)}</td>
              <td style="font-weight: 700; color: var(--color-primary);">${formatCurrency(b.total)}</td>
              <td>
                <button class="btn btn-ghost btn-sm" onclick="viewBill('${b.id}')">View</button>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  }
}

// ---- NEW BILL ----
let billItems = [];

function renderNewBill() {
  // Populate customer dropdown
  const select = document.getElementById('bill-customer-select');
  const customers = getCustomers();
  const currentVal = select.value;

  select.innerHTML = '<option value="">— Choose a customer —</option>' +
    customers.map(c => `<option value="${c.id}" ${c.id === currentVal ? 'selected' : ''}>${c.name}${c.phone ? ' (' + c.phone + ')' : ''}</option>`).join('');

  // Handle customer selection change
  select.onchange = () => {
    const infoEl = document.getElementById('selected-customer-info');
    const cust = customers.find(c => c.id === select.value);
    if (cust) {
      infoEl.classList.remove('hidden');
      infoEl.innerHTML = `
        <div class="customer-info-name">${cust.name}</div>
        ${cust.phone ? `<div>📞 ${cust.phone}</div>` : ''}
      `;
    } else {
      infoEl.classList.add('hidden');
    }
  };

  // Init with one empty item if none
  if (billItems.length === 0) {
    addBillItem();
  } else {
    renderBillItems();
  }
}

function addBillItem() {
  const products = getProducts().filter(p => p.active);
  const lastPrices = getLastPrices();

  billItems.push({
    id: generateId(),
    productId: '',
    productName: '',
    qty: '',
    rate: '',
    amount: 0,
  });

  renderBillItems();
}

function renderBillItems() {
  const tbody = document.getElementById('bill-items-body');
  const products = getProducts().filter(p => p.active);
  const lastPrices = getLastPrices();

  tbody.innerHTML = billItems.map((item, idx) => `
    <tr data-item-id="${item.id}">
      <td>
        <select class="form-input" onchange="updateItemProduct(${idx}, this.value)">
          <option value="">Select...</option>
          ${products.map(p => `<option value="${p.id}" ${item.productId === p.id ? 'selected' : ''}>${p.name}</option>`).join('')}
        </select>
      </td>
      <td>
        <input type="number" class="form-input" placeholder="0" min="0" step="0.5"
               value="${item.qty}" oninput="updateItemQty(${idx}, this.value)">
      </td>
      <td>
        <input type="number" class="form-input" placeholder="₹0" min="0" step="1"
               value="${item.rate}" oninput="updateItemRate(${idx}, this.value)">
      </td>
      <td>
        <div class="item-amount">${item.amount > 0 ? formatCurrency(item.amount) : '—'}</div>
      </td>
      <td>
        <button class="btn-remove-item" onclick="removeBillItem(${idx})" ${billItems.length <= 1 ? 'disabled' : ''}>
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </td>
    </tr>
  `).join('');

  updateBillTotals();
}

function updateItemProduct(idx, productId) {
  const products = getProducts();
  const product = products.find(p => p.id === productId);
  const lastPrices = getLastPrices();

  billItems[idx].productId = productId;
  billItems[idx].productName = product ? product.name : '';

  // Auto-fill last used price
  if (product && lastPrices[productId]) {
    billItems[idx].rate = lastPrices[productId];
  }

  billItems[idx].amount = (billItems[idx].qty || 0) * (billItems[idx].rate || 0);
  renderBillItems();
}

function updateItemQty(idx, value) {
  billItems[idx].qty = parseFloat(value) || 0;
  billItems[idx].amount = billItems[idx].qty * (billItems[idx].rate || 0);
  // Update amount display without full re-render
  const row = document.querySelector(`[data-item-id="${billItems[idx].id}"]`);
  if (row) {
    row.querySelector('.item-amount').textContent = billItems[idx].amount > 0 ? formatCurrency(billItems[idx].amount) : '—';
  }
  updateBillTotals();
}

function updateItemRate(idx, value) {
  billItems[idx].rate = parseFloat(value) || 0;
  billItems[idx].amount = (billItems[idx].qty || 0) * billItems[idx].rate;
  const row = document.querySelector(`[data-item-id="${billItems[idx].id}"]`);
  if (row) {
    row.querySelector('.item-amount').textContent = billItems[idx].amount > 0 ? formatCurrency(billItems[idx].amount) : '—';
  }
  updateBillTotals();
}

function removeBillItem(idx) {
  if (billItems.length <= 1) return;
  billItems.splice(idx, 1);
  renderBillItems();
}

function updateBillTotals() {
  const subtotal = billItems.reduce((s, item) => s + (item.amount || 0), 0);
  const discountValue = parseFloat(document.getElementById('bill-discount-value')?.value) || 0;
  const discountType = document.getElementById('bill-discount-type')?.value || 'flat';

  let discountAmount = 0;
  if (discountType === 'percent') {
    discountAmount = (subtotal * discountValue) / 100;
  } else {
    discountAmount = discountValue;
  }
  discountAmount = Math.min(discountAmount, subtotal); // can't discount more than subtotal

  const grandTotal = subtotal - discountAmount;

  document.getElementById('bill-subtotal').textContent = formatCurrency(subtotal);
  document.getElementById('bill-discount-amount').textContent = `−${formatCurrency(discountAmount)}`;
  document.getElementById('bill-grand-total').textContent = formatCurrency(grandTotal);
}

function generateBill() {
  const customerId = document.getElementById('bill-customer-select').value;
  const customers = getCustomers();
  const customer = customers.find(c => c.id === customerId);

  if (!customerId || !customer) {
    showToast('Please select a customer', 'error');
    return;
  }

  const validItems = billItems.filter(item => item.productId && item.qty > 0 && item.rate > 0);
  if (validItems.length === 0) {
    showToast('Please add at least one item with quantity and rate', 'error');
    return;
  }

  const subtotal = validItems.reduce((s, item) => s + item.amount, 0);
  const discountValue = parseFloat(document.getElementById('bill-discount-value').value) || 0;
  const discountType = document.getElementById('bill-discount-type').value;
  let discountAmount = discountType === 'percent' ? (subtotal * discountValue) / 100 : discountValue;
  discountAmount = Math.min(discountAmount, subtotal);
  const total = subtotal - discountAmount;

  const bill = {
    id: generateId(),
    billNumber: generateBillNumber(),
    customerId: customer.id,
    customerName: customer.name,
    customerPhone: customer.phone || '',
    items: validItems.map(item => ({
      productId: item.productId,
      productName: item.productName,
      qty: item.qty,
      rate: item.rate,
      amount: item.amount,
    })),
    subtotal,
    discountValue,
    discountType,
    discountAmount,
    total,
    createdAt: new Date().toISOString(),
  };

  // Save bill
  const bills = getBills();
  bills.push(bill);
  saveBills(bills);

  // Save last prices
  const lastPrices = getLastPrices();
  validItems.forEach(item => {
    lastPrices[item.productId] = item.rate;
  });
  saveLastPrices(lastPrices);

  // Reset form
  billItems = [];
  document.getElementById('bill-customer-select').value = '';
  document.getElementById('selected-customer-info').classList.add('hidden');
  document.getElementById('bill-discount-value').value = '0';

  showToast(`Bill ${bill.billNumber} created successfully!`, 'success');

  // Show preview
  viewBill(bill.id);
}

// ---- CUSTOMERS ----
function renderCustomers() {
  const customers = getCustomers();
  const search = (document.getElementById('customer-search')?.value || '').toLowerCase();
  const filtered = customers.filter(c =>
    c.name.toLowerCase().includes(search) ||
    (c.phone && c.phone.includes(search))
  );

  document.getElementById('customers-count').textContent = `${customers.length} customer${customers.length !== 1 ? 's' : ''}`;

  const grid = document.getElementById('customers-grid');
  if (filtered.length === 0) {
    grid.innerHTML = `
      <div class="empty-state" style="grid-column: 1 / -1;">
        <span class="empty-icon">👥</span>
        <p>${search ? 'No customers match your search.' : 'No customers yet. Add your first customer!'}</p>
      </div>
    `;
  } else {
    grid.innerHTML = filtered.map(c => `
      <div class="customer-card">
        <div class="customer-info">
          <div class="customer-avatar">${getInitials(c.name)}</div>
          <div>
            <div class="customer-name">${c.name}</div>
            <div class="customer-phone">${c.phone || 'No phone'}</div>
          </div>
        </div>
        <div class="customer-actions">
          <button class="btn btn-ghost btn-sm" onclick="editCustomer('${c.id}')">Edit</button>
          <button class="btn btn-ghost btn-sm" onclick="deleteCustomer('${c.id}')" style="color: var(--color-danger);">Delete</button>
        </div>
      </div>
    `).join('');
  }
}

function openCustomerModal(id = null) {
  const overlay = document.getElementById('customer-modal-overlay');
  const titleEl = document.getElementById('customer-modal-title');
  const nameInput = document.getElementById('customer-name');
  const phoneInput = document.getElementById('customer-phone');
  const editIdInput = document.getElementById('customer-edit-id');

  if (id) {
    const customer = getCustomers().find(c => c.id === id);
    if (!customer) return;
    titleEl.textContent = 'Edit Customer';
    nameInput.value = customer.name;
    phoneInput.value = customer.phone || '';
    editIdInput.value = customer.id;
  } else {
    titleEl.textContent = 'Add Customer';
    nameInput.value = '';
    phoneInput.value = '';
    editIdInput.value = '';
  }

  overlay.classList.add('active');
  nameInput.focus();
}

function closeCustomerModal() {
  document.getElementById('customer-modal-overlay').classList.remove('active');
}

function editCustomer(id) {
  openCustomerModal(id);
}

function saveCustomer() {
  const name = document.getElementById('customer-name').value.trim();
  const phone = document.getElementById('customer-phone').value.trim();
  const editId = document.getElementById('customer-edit-id').value;

  if (!name) {
    showToast('Customer name is required', 'error');
    return;
  }

  const customers = getCustomers();

  if (editId) {
    // Edit existing
    const idx = customers.findIndex(c => c.id === editId);
    if (idx >= 0) {
      customers[idx].name = name;
      customers[idx].phone = phone;
    }
    showToast('Customer updated', 'success');
  } else {
    // Add new
    customers.push({
      id: generateId(),
      name,
      phone,
      createdAt: new Date().toISOString(),
    });
    showToast('Customer added!', 'success');
  }

  saveCustomers(customers);
  closeCustomerModal();

  // Re-render current view
  if (currentView === 'customers') renderCustomers();
  if (currentView === 'new-bill') renderNewBill();
}

function deleteCustomer(id) {
  if (!confirm('Are you sure you want to delete this customer?')) return;
  const customers = getCustomers().filter(c => c.id !== id);
  saveCustomers(customers);
  showToast('Customer deleted', 'info');
  renderCustomers();
}

// ---- BILL HISTORY ----
function renderHistory() {
  const bills = getBills();
  const search = (document.getElementById('history-search')?.value || '').toLowerCase();
  const fromDate = document.getElementById('history-from')?.value;
  const toDate = document.getElementById('history-to')?.value;

  let filtered = [...bills].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

  if (search) {
    filtered = filtered.filter(b =>
      b.billNumber.toLowerCase().includes(search) ||
      b.customerName.toLowerCase().includes(search)
    );
  }

  if (fromDate) {
    const from = new Date(fromDate);
    from.setHours(0, 0, 0, 0);
    filtered = filtered.filter(b => new Date(b.createdAt) >= from);
  }

  if (toDate) {
    const to = new Date(toDate);
    to.setHours(23, 59, 59, 999);
    filtered = filtered.filter(b => new Date(b.createdAt) <= to);
  }

  document.getElementById('history-count').textContent = `${bills.length} total bill${bills.length !== 1 ? 's' : ''}`;

  const container = document.getElementById('history-table-container');

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <span class="empty-icon">📋</span>
        <p>${search || fromDate || toDate ? 'No bills match your filters.' : 'No bills yet.'}</p>
      </div>
    `;
  } else {
    const totalFiltered = filtered.reduce((s, b) => s + b.total, 0);
    container.innerHTML = `
      <div style="padding: 12px 16px; border-bottom: 1px solid var(--border-subtle); font-size: 13px; color: var(--text-secondary);">
        Showing <strong style="color: var(--text-primary);">${filtered.length}</strong> bill${filtered.length !== 1 ? 's' : ''}
        · Total: <strong style="color: var(--color-primary);">${formatCurrency(totalFiltered)}</strong>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>Bill No.</th>
            <th>Customer</th>
            <th>Items</th>
            <th>Date</th>
            <th>Amount</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          ${filtered.map(b => `
            <tr>
              <td><span class="badge badge-amber">${b.billNumber}</span></td>
              <td>${b.customerName}</td>
              <td>${b.items.length} item${b.items.length !== 1 ? 's' : ''}</td>
              <td>${formatDateTime(b.createdAt)}</td>
              <td style="font-weight: 700; color: var(--color-primary);">${formatCurrency(b.total)}</td>
              <td>
                <div style="display: flex; gap: 4px;">
                  <button class="btn btn-ghost btn-sm" onclick="viewBill('${b.id}')">View</button>
                  <button class="btn btn-ghost btn-sm" onclick="deleteBill('${b.id}')" style="color: var(--color-danger);">Delete</button>
                </div>
              </td>
            </tr>
          `).join('')}
        </tbody>
      </table>
    `;
  }
}

function deleteBill(id) {
  if (!confirm('Delete this bill? This cannot be undone.')) return;
  const bills = getBills().filter(b => b.id !== id);
  saveBills(bills);
  showToast('Bill deleted', 'info');
  renderHistory();
}

// ---- BILL PREVIEW & PDF ----
function viewBill(id) {
  const bill = getBills().find(b => b.id === id);
  if (!bill) {
    showToast('Bill not found', 'error');
    return;
  }

  currentBillForPreview = bill;
  const settings = getSettings();

  const shopDetails = [settings.shopPhone, settings.shopAddress, settings.shopGstin ? `GSTIN: ${settings.shopGstin}` : '']
    .filter(Boolean).join(' · ');

  const content = document.getElementById('bill-preview-content');
  content.innerHTML = `
    <div class="bill-preview">
      <div class="bill-preview-header">
        <div class="bill-preview-shop-name">${settings.shopName || 'SR Chicken'}</div>
        ${shopDetails ? `<div class="bill-preview-shop-details">${shopDetails}</div>` : ''}
      </div>

      <div class="bill-preview-info">
        <div>
          <div class="bill-preview-info-label">Bill Number</div>
          <div class="bill-preview-info-value">${bill.billNumber}</div>
        </div>
        <div>
          <div class="bill-preview-info-label">Date</div>
          <div class="bill-preview-info-value">${formatDateTime(bill.createdAt)}</div>
        </div>
        <div>
          <div class="bill-preview-info-label">Customer</div>
          <div class="bill-preview-info-value">${bill.customerName}</div>
        </div>
        <div>
          <div class="bill-preview-info-label">Phone</div>
          <div class="bill-preview-info-value">${bill.customerPhone || '—'}</div>
        </div>
      </div>

      <table class="bill-preview-table">
        <thead>
          <tr>
            <th>#</th>
            <th>Item</th>
            <th>Qty (kg)</th>
            <th>Rate (₹/kg)</th>
            <th>Amount (₹)</th>
          </tr>
        </thead>
        <tbody>
          ${bill.items.map((item, i) => `
            <tr>
              <td>${i + 1}</td>
              <td>${item.productName}</td>
              <td>${item.qty}</td>
              <td>${formatCurrency(item.rate)}</td>
              <td>${formatCurrency(item.amount)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>

      <div class="bill-preview-totals">
        <div class="total-row">
          <span>Subtotal</span>
          <span>${formatCurrency(bill.subtotal)}</span>
        </div>
        ${bill.discountAmount > 0 ? `
          <div class="total-row">
            <span>Discount${bill.discountType === 'percent' ? ` (${bill.discountValue}%)` : ''}</span>
            <span style="color: #ef4444;">−${formatCurrency(bill.discountAmount)}</span>
          </div>
        ` : ''}
        <div class="total-row grand-total">
          <span>Total</span>
          <span>${formatCurrency(bill.total)}</span>
        </div>
      </div>

      <div class="bill-preview-footer">
        Thank you for your business! 🐔
      </div>
    </div>
  `;

  document.getElementById('bill-modal-overlay').classList.add('active');
}

function closeBillModal() {
  document.getElementById('bill-modal-overlay').classList.remove('active');
  currentBillForPreview = null;
}

function downloadPDF() {
  if (!currentBillForPreview) return;
  const bill = currentBillForPreview;
  const settings = getSettings();

  const { jsPDF } = window.jspdf;
  const doc = new jsPDF();

  const pageWidth = doc.internal.pageSize.getWidth();
  const margin = 20;

  // ---- Header ----
  doc.setFontSize(22);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(180, 83, 9); // amber-700
  doc.text(settings.shopName || 'SR Chicken', pageWidth / 2, 25, { align: 'center' });

  let yPos = 32;

  // Shop details
  const shopDetailParts = [settings.shopPhone, settings.shopAddress, settings.shopGstin ? `GSTIN: ${settings.shopGstin}` : ''].filter(Boolean);
  if (shopDetailParts.length > 0) {
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(107, 114, 128);
    doc.text(shopDetailParts.join('  |  '), pageWidth / 2, yPos, { align: 'center' });
    yPos += 6;
  }

  // Divider
  yPos += 4;
  doc.setDrawColor(229, 231, 235);
  doc.setLineWidth(0.5);
  doc.line(margin, yPos, pageWidth - margin, yPos);
  yPos += 10;

  // ---- Bill Info ----
  doc.setFontSize(10);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(107, 114, 128);
  doc.text('BILL NUMBER', margin, yPos);
  doc.text('DATE', pageWidth / 2, yPos);
  yPos += 5;

  doc.setFont('helvetica', 'normal');
  doc.setTextColor(26, 26, 26);
  doc.text(bill.billNumber, margin, yPos);
  doc.text(formatDateTime(bill.createdAt), pageWidth / 2, yPos);
  yPos += 8;

  doc.setFont('helvetica', 'bold');
  doc.setTextColor(107, 114, 128);
  doc.text('CUSTOMER', margin, yPos);
  doc.text('PHONE', pageWidth / 2, yPos);
  yPos += 5;

  doc.setFont('helvetica', 'normal');
  doc.setTextColor(26, 26, 26);
  doc.text(bill.customerName, margin, yPos);
  doc.text(bill.customerPhone || '—', pageWidth / 2, yPos);
  yPos += 10;

  // ---- Items Table ----
  const tableData = bill.items.map((item, i) => [
    i + 1,
    item.productName,
    item.qty + ' kg',
    formatCurrency(item.rate),
    formatCurrency(item.amount),
  ]);

  doc.autoTable({
    startY: yPos,
    head: [['#', 'Item', 'Qty', 'Rate', 'Amount']],
    body: tableData,
    theme: 'grid',
    headStyles: {
      fillColor: [245, 158, 11],
      textColor: [255, 255, 255],
      fontStyle: 'bold',
      fontSize: 10,
    },
    bodyStyles: {
      fontSize: 10,
      textColor: [55, 65, 81],
    },
    columnStyles: {
      0: { halign: 'center', cellWidth: 12 },
      2: { halign: 'right' },
      3: { halign: 'right' },
      4: { halign: 'right' },
    },
    margin: { left: margin, right: margin },
    styles: {
      lineColor: [229, 231, 235],
      lineWidth: 0.3,
    },
  });

  yPos = doc.lastAutoTable.finalY + 10;

  // ---- Totals ----
  const totalsX = pageWidth - margin - 70;
  const valuesX = pageWidth - margin;

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(107, 114, 128);
  doc.text('Subtotal', totalsX, yPos, { align: 'right' });
  doc.setTextColor(26, 26, 26);
  doc.text(formatCurrency(bill.subtotal), valuesX, yPos, { align: 'right' });
  yPos += 6;

  if (bill.discountAmount > 0) {
    doc.setTextColor(107, 114, 128);
    const discLabel = bill.discountType === 'percent' ? `Discount (${bill.discountValue}%)` : 'Discount';
    doc.text(discLabel, totalsX, yPos, { align: 'right' });
    doc.setTextColor(239, 68, 68);
    doc.text(`-${formatCurrency(bill.discountAmount)}`, valuesX, yPos, { align: 'right' });
    yPos += 6;
  }

  // Total line
  doc.setDrawColor(229, 231, 235);
  doc.setLineWidth(0.5);
  doc.line(totalsX - 10, yPos, valuesX, yPos);
  yPos += 7;

  doc.setFontSize(14);
  doc.setFont('helvetica', 'bold');
  doc.setTextColor(26, 26, 26);
  doc.text('Total', totalsX, yPos, { align: 'right' });
  doc.setTextColor(180, 83, 9);
  doc.text(formatCurrency(bill.total), valuesX, yPos, { align: 'right' });

  // ---- Footer ----
  yPos += 20;
  doc.setDrawColor(200, 200, 200);
  doc.setLineDashPattern([2, 2], 0);
  doc.line(margin, yPos, pageWidth - margin, yPos);
  yPos += 8;

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.setTextColor(156, 163, 175);
  doc.text('Thank you for your business!', pageWidth / 2, yPos, { align: 'center' });

  // Save
  doc.save(`${bill.billNumber}.pdf`);
  showToast('PDF downloaded!', 'success');
}

// ---- SETTINGS ----
function renderSettings() {
  const settings = getSettings();
  document.getElementById('settings-shop-name').value = settings.shopName || '';
  document.getElementById('settings-shop-phone').value = settings.shopPhone || '';
  document.getElementById('settings-shop-address').value = settings.shopAddress || '';
  document.getElementById('settings-shop-gstin').value = settings.shopGstin || '';

  renderProductsList();
}

function saveSettings() {
  const settings = {
    shopName: document.getElementById('settings-shop-name').value.trim(),
    shopPhone: document.getElementById('settings-shop-phone').value.trim(),
    shopAddress: document.getElementById('settings-shop-address').value.trim(),
    shopGstin: document.getElementById('settings-shop-gstin').value.trim(),
  };
  saveSettingsData(settings);
  showToast('Settings saved!', 'success');
}

function renderProductsList() {
  const products = getProducts();
  const list = document.getElementById('settings-products-list');

  list.innerHTML = products.map(p => `
    <div class="product-item">
      <span class="product-item-name">${p.name}</span>
      <div style="display: flex; gap: 6px; align-items: center;">
        <span class="badge ${p.active ? 'badge-green' : ''}" style="${!p.active ? 'opacity: 0.5;' : ''}">${p.active ? 'Active' : 'Inactive'}</span>
        <button class="btn btn-ghost btn-sm" onclick="toggleProduct('${p.id}')">${p.active ? 'Disable' : 'Enable'}</button>
        <button class="btn btn-ghost btn-sm" onclick="removeProduct('${p.id}')" style="color: var(--color-danger);">Remove</button>
      </div>
    </div>
  `).join('');
}

function addProduct() {
  const nameInput = document.getElementById('new-product-name');
  const name = nameInput.value.trim();
  if (!name) {
    showToast('Enter a product name', 'error');
    return;
  }

  const products = getProducts();
  products.push({
    id: generateId(),
    name,
    unit: 'kg',
    active: true,
  });
  saveProducts(products);
  nameInput.value = '';
  renderProductsList();
  showToast('Product added!', 'success');
}

function toggleProduct(id) {
  const products = getProducts();
  const product = products.find(p => p.id === id);
  if (product) {
    product.active = !product.active;
    saveProducts(products);
    renderProductsList();
  }
}

function removeProduct(id) {
  if (!confirm('Remove this product?')) return;
  const products = getProducts().filter(p => p.id !== id);
  saveProducts(products);
  renderProductsList();
  showToast('Product removed', 'info');
}

// ---- DATA MANAGEMENT ----
function exportData() {
  const data = {
    customers: getCustomers(),
    bills: getBills(),
    products: getProducts(),
    settings: getSettings(),
    lastPrices: getLastPrices(),
    exportedAt: new Date().toISOString(),
  };

  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `sr-chicken-backup-${new Date().toISOString().slice(0, 10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  showToast('Data exported!', 'success');
}

function clearAllData() {
  if (!confirm('⚠️ This will delete ALL bills, customers, and settings. Are you sure?')) return;
  if (!confirm('This is your last chance! Type OK to confirm.')) return;

  Storage.remove('customers');
  Storage.remove('bills');
  Storage.remove('products');
  Storage.remove('settings');
  Storage.remove('lastPrices');

  showToast('All data cleared', 'info');
  navigateTo('dashboard');
}

// ---- KEYBOARD SHORTCUTS ----
document.addEventListener('keydown', (e) => {
  // Escape to close modals
  if (e.key === 'Escape') {
    closeCustomerModal();
    closeBillModal();
  }
});

// Close modals on overlay click
document.getElementById('customer-modal-overlay')?.addEventListener('click', (e) => {
  if (e.target === e.currentTarget) closeCustomerModal();
});

document.getElementById('bill-modal-overlay')?.addEventListener('click', (e) => {
  if (e.target === e.currentTarget) closeBillModal();
});

// ---- INIT ----
function init() {
  // Initialize products if first run
  if (!Storage.get('products')) {
    saveProducts(DEFAULT_PRODUCTS);
  }

  // Handle routing
  window.addEventListener('hashchange', handleHashChange);
  handleHashChange();
}

// Start the app
document.addEventListener('DOMContentLoaded', init);
