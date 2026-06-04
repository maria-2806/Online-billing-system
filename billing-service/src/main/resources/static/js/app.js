// app.js
document.addEventListener("DOMContentLoaded", () => {
    // 1. Session check
    if (localStorage.getItem("isLoggedIn") !== "true") {
        window.location.href = "index.html";
        return;
    }

    // Load active tab data on start
    loadTabContent("overview");

    // Initialize calculations listener for Create Invoice modal
    const invoiceAmountInput = document.getElementById("invoiceAmount");
    if (invoiceAmountInput) {
        invoiceAmountInput.addEventListener("input", updateInvoicePreview);
    }

    // Initialize change listener for Payment select
    const payInvoiceSelect = document.getElementById("payInvoiceSelect");
    if (payInvoiceSelect) {
        payInvoiceSelect.addEventListener("change", handleInvoiceSelectForPayment);
    }

    // Set up form submission handlers
    setupFormHandlers();
});

// Toast notifications helper
function showToast(message, type = "success") {
    const container = document.getElementById("toastContainer");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span>${message}</span>
        <button onclick="this.parentElement.remove()" style="background:none;border:none;color:var(--text-secondary);cursor:pointer;font-size:1.1rem;margin-left:12px;">&times;</button>
    `;
    container.appendChild(toast);

    // Auto-remove after 4 seconds
    setTimeout(() => {
        toast.remove();
    }, 4000);
}

// Tab Switching
function switchTab(tabId, element) {
    // Hide all tabs
    document.querySelectorAll(".tab-content").forEach(el => el.style.display = "none");
    // Remove active class from menu items
    document.querySelectorAll(".nav-item").forEach(el => el.classList.remove("active"));

    // Show selected tab
    const selectedTab = document.getElementById(`tab-${tabId}`);
    if (selectedTab) selectedTab.style.display = "block";

    // Set menu item active
    if (element) {
        element.classList.add("active");
    } else {
        // Fallback: search for item matching the action
        document.querySelectorAll(".nav-item").forEach(el => {
            if (el.textContent.trim().toLowerCase().includes(tabId)) {
                el.classList.add("active");
            }
        });
    }

    // Update Titles
    const pageTitle = document.getElementById("pageTitle");
    const pageSubtitle = document.getElementById("pageSubtitle");
    if (pageTitle) {
        pageTitle.textContent = tabId.charAt(0).toUpperCase() + tabId.slice(1) + (tabId === "overview" ? " Overview" : " Management");
    }

    // Load active tab data
    loadTabContent(tabId);
}

// Load Tab Data Orchestration
function loadTabContent(tabId) {
    switch (tabId) {
        case "overview":
            loadOverviewData();
            break;
        case "customers":
            loadCustomersData();
            break;
        case "invoices":
            loadInvoicesData();
            break;
        case "payments":
            loadPaymentsData();
            break;
    }
}

// Modal Toggle Helpers
function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.add("active");
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) modal.classList.remove("active");
}

// --- API ACTIONS & RENDERING ---

// 1. OVERVIEW DATA
async function loadOverviewData() {
    try {
        const [customers, bills] = await Promise.all([
            fetch("/api/billing/customers").then(res => res.json()),
            fetch("/api/billing/invoices").then(res => res.json())
        ]);

        // Render customer count
        document.getElementById("statTotalCustomers").textContent = customers.length;

        let totalBilled = 0;
        let totalCollected = 0;

        // Process bills and sum collections
        const billPromises = bills.map(async (bill) => {
            totalBilled += bill.total;
            
            // Fetch payments for this bill to calculate collected amount
            const payments = await fetch(`/api/billing/payments?billId=${bill.id}`).then(res => res.json());
            const collected = payments
                .filter(p => p.status === "SUCCESS")
                .reduce((sum, p) => sum + p.amountPaid, 0);
            
            totalCollected += collected;
        });

        await Promise.all(billPromises);

        const outstanding = totalBilled - totalCollected;

        // Update stats card UI
        document.getElementById("statTotalBilled").textContent = formatCurrency(totalBilled);
        document.getElementById("statTotalCollected").textContent = formatCurrency(totalCollected);
        document.getElementById("statOutstanding").textContent = formatCurrency(outstanding);

        // Render recent invoices table (last 5)
        const recentBills = bills.slice(-5).reverse();
        const tbody = document.getElementById("recentInvoicesTable");
        tbody.innerHTML = "";

        if (recentBills.length === 0) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-secondary);">No invoices generated yet.</td></tr>`;
            return;
        }

        recentBills.forEach(bill => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>#${bill.id}</td>
                <td>${bill.customerName}</td>
                <td>${formatCurrency(bill.subtotal)}</td>
                <td>${formatCurrency(bill.tax)}</td>
                <td style="font-weight: 600; color: var(--text-primary);">${formatCurrency(bill.total)}</td>
                <td><span class="badge ${getBadgeClass(bill.status)}">${bill.status}</span></td>
            `;
            tbody.appendChild(tr);
        });

    } catch (err) {
        console.error("Error loading overview stats", err);
        showToast("Failed to fetch dashboard statistics", "error");
    }
}

// 2. CUSTOMERS MANAGEMENT
async function loadCustomersData() {
    try {
        const customers = await fetch("/api/billing/customers").then(res => res.json());
        const tbody = document.getElementById("customersTable");
        tbody.innerHTML = "";

        if (customers.length === 0) {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-secondary);">No customers registered.</td></tr>`;
            return;
        }

        customers.forEach(c => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>${c.id}</td>
                <td style="font-weight: 600;">${c.name}</td>
                <td>${c.email}</td>
                <td style="text-align: right; padding-right:20px;">
                    <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="openEditCustomerModal(${c.id}, '${escapeHtml(c.name)}', '${escapeHtml(c.email)}')">Edit</button>
                    <button class="btn btn-primary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="viewCustomerSummary(${c.id})">Summary</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("Error loading customers", err);
        showToast("Failed to fetch customer list", "error");
    }
}

// 3. INVOICES & BILLS
async function loadInvoicesData() {
    try {
        const bills = await fetch("/api/billing/invoices").then(res => res.json());
        const tbody = document.getElementById("invoicesTable");
        tbody.innerHTML = "";

        if (bills.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary);">No invoices created.</td></tr>`;
            return;
        }

        bills.slice().reverse().forEach(bill => {
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>#${bill.id}</td>
                <td>${bill.customerId}</td>
                <td>${bill.customerName}</td>
                <td>${formatCurrency(bill.subtotal)}</td>
                <td>${formatCurrency(bill.tax)}</td>
                <td style="font-weight: 600; color: var(--text-primary);">${formatCurrency(bill.total)}</td>
                <td><span class="badge ${getBadgeClass(bill.status)}">${bill.status}</span></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error("Error loading bills", err);
        showToast("Failed to fetch invoices", "error");
    }
}

// 4. PAYMENTS RECORD TAB
async function loadPaymentsData() {
    try {
        const bills = await fetch("/api/billing/invoices").then(res => res.json());
        const select = document.getElementById("payInvoiceSelect");
        select.innerHTML = '<option value="" disabled selected>Select an unpaid invoice</option>';

        // Filter bills that are not fully settled or cancelled
        const unpaidBills = bills.filter(b => b.status !== "PAID" && b.status !== "CANCELLED");

        if (unpaidBills.length === 0) {
            showToast("No outstanding invoices available for payment");
            document.getElementById("payAmountGroup").style.display = "none";
            return;
        }

        unpaidBills.forEach(b => {
            const option = document.createElement("option");
            option.value = b.id;
            option.textContent = `Invoice #${b.id} - ${b.customerName} (${formatCurrency(b.total)})`;
            select.appendChild(option);
        });

        // Hide invoice balance info until selection
        document.getElementById("payAmountGroup").style.display = "none";
        document.getElementById("payAmount").value = "";
        document.getElementById("payAmountHint").textContent = "";

    } catch (err) {
        console.error("Error loading bills for payment", err);
        showToast("Failed to load invoice list for payment options", "error");
    }
}

// Get invoice balances dynamically when selected for payment
async function handleInvoiceSelectForPayment(event) {
    const billId = event.target.value;
    if (!billId) return;

    try {
        const [bill, payments] = await Promise.all([
            fetch(`/api/billing/invoices/${billId}/status`).then(res => {
                // Fetch the full bill object from listing
                return fetch("/api/billing/invoices").then(r => r.json()).then(list => list.find(b => b.id == billId));
            }),
            fetch(`/api/billing/payments?billId=${billId}`).then(res => res.json())
        ]);

        const totalInvoice = bill.total;
        const amountPaid = payments
            .filter(p => p.status === "SUCCESS")
            .reduce((sum, p) => sum + p.amountPaid, 0);
        
        const outstanding = totalInvoice - amountPaid;

        // Render preview card details
        document.getElementById("payTotalInvoiceVal").textContent = formatCurrency(totalInvoice);
        document.getElementById("payAmountPaidVal").textContent = formatCurrency(amountPaid);
        document.getElementById("payOutstandingVal").textContent = formatCurrency(outstanding);
        document.getElementById("payAmountGroup").style.display = "block";

        // Auto-fill and bound inputs
        const payAmountInput = document.getElementById("payAmount");
        payAmountInput.value = outstanding.toFixed(2);
        payAmountInput.max = outstanding.toFixed(2);

        document.getElementById("payAmountHint").textContent = `Max payable amount is ${formatCurrency(outstanding)}.`;

    } catch (err) {
        console.error("Error calculating invoice balance details", err);
        showToast("Failed to retrieve balance metrics for invoice", "error");
    }
}

// Automatic calculations for Create Invoice modal
function updateInvoicePreview() {
    const rawVal = parseFloat(document.getElementById("invoiceAmount").value) || 0;
    
    const subtotal = rawVal;
    const tax = rawVal * 0.18;
    const total = subtotal + tax;

    document.getElementById("calcSubtotal").textContent = formatCurrency(subtotal);
    document.getElementById("calcTax").textContent = formatCurrency(tax);
    document.getElementById("calcTotal").textContent = formatCurrency(total);
}

// Generate Invoice Modal Setup
async function openCreateInvoiceModal() {
    try {
        const customers = await fetch("/api/billing/customers").then(res => res.json());
        const select = document.getElementById("invoiceCustSelect");
        select.innerHTML = '<option value="" disabled selected>Select customer</option>';

        customers.forEach(c => {
            const option = document.createElement("option");
            option.value = c.id;
            option.textContent = `${c.name} (${c.email})`;
            select.appendChild(option);
        });

        // Reset values
        document.getElementById("invoiceAmount").value = "";
        updateInvoicePreview();

        openModal("createInvoiceModal");
    } catch (err) {
        console.error("Error setting up invoice generator", err);
        showToast("Failed to fetch customers list", "error");
    }
}

// Edit Customer Modal Setup
function openEditCustomerModal(id, name, email) {
    document.getElementById("editCustId").value = id;
    document.getElementById("editCustName").value = name;
    document.getElementById("editCustEmail").value = email;
    openModal("editCustomerModal");
}

// View Customer Summary Dashboard Modal
async function viewCustomerSummary(id) {
    try {
        const summary = await fetch(`/api/billing/customers/${id}/summary`).then(res => res.json());

        document.getElementById("summaryCustName").textContent = summary.customerName;
        document.getElementById("summaryCustId").textContent = `Customer ID: ${summary.customerId}`;
        document.getElementById("summaryTotalBills").textContent = summary.totalBills;
        document.getElementById("summaryPaidBills").textContent = summary.paidBills;
        document.getElementById("summaryPendingBills").textContent = summary.pendingBills;
        document.getElementById("summaryTotalBilled").textContent = formatCurrency(summary.totalBilled);

        // Customize letter avatar based on first char of name
        document.getElementById("summaryAvatar").textContent = summary.customerName.charAt(0).toUpperCase();

        openModal("customerSummaryModal");
    } catch (err) {
        console.error("Error loading customer metrics summary", err);
        showToast("Failed to load customer summary stats", "error");
    }
}

// --- SUBMIT EVENT HANDLERS ---

function setupFormHandlers() {
    // 1. Add Customer Form
    document.getElementById("addCustomerForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        const name = document.getElementById("custName").value.trim();
        const email = document.getElementById("custEmail").value.trim();

        try {
            const res = await fetch("/api/billing/customers", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name, email })
            });

            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg || "Failed to create customer");
            }

            closeModal("addCustomerModal");
            document.getElementById("addCustomerForm").reset();
            showToast("Customer registered successfully");
            loadCustomersData();
        } catch (err) {
            console.error(err);
            showToast(err.message || "Email address is already in use", "error");
        }
    });

    // 2. Edit Customer Form
    document.getElementById("editCustomerForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        const id = document.getElementById("editCustId").value;
        const name = document.getElementById("editCustName").value.trim();
        const email = document.getElementById("editCustEmail").value.trim();

        try {
            const res = await fetch(`/api/billing/customers/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name, email })
            });

            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg || "Failed to update customer");
            }

            closeModal("editCustomerModal");
            showToast("Customer updated successfully");
            loadCustomersData();
        } catch (err) {
            console.error(err);
            showToast(err.message || "Failed to edit customer", "error");
        }
    });

    // 3. Create Invoice Form
    document.getElementById("createInvoiceForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        const customerId = parseInt(document.getElementById("invoiceCustSelect").value);
        const amount = parseFloat(document.getElementById("invoiceAmount").value);

        try {
            const res = await fetch("/api/billing/invoices", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ customerId, amount })
            });

            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg || "Failed to generate invoice");
            }

            closeModal("createInvoiceModal");
            document.getElementById("createInvoiceForm").reset();
            showToast("Billing invoice generated successfully");
            loadInvoicesData();
        } catch (err) {
            console.error(err);
            showToast(err.message || "Failed to generate invoice", "error");
        }
    });

    // 4. Record Payment Form
    document.getElementById("paymentForm").addEventListener("submit", async (e) => {
        e.preventDefault();
        const billId = parseInt(document.getElementById("payInvoiceSelect").value);
        const amount = parseFloat(document.getElementById("payAmount").value);
        const method = document.getElementById("payMethod").value;

        try {
            const res = await fetch("/api/billing/payments", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ billId, amount, method })
            });

            if (!res.ok) {
                const errMsg = await res.text();
                throw new Error(errMsg || "Failed to record payment");
            }

            const paymentResponse = await res.json();
            showToast(`Transaction successful! Remaining balance: ${formatCurrency(paymentResponse.remainingBalance)}`);
            
            // Shift to invoices tab to view updated statuses
            switchTab("invoices");
        } catch (err) {
            console.error(err);
            showToast(err.message || "Overpayment or invalid payment details", "error");
        }
    });
}

// --- PRESENTATION HELPERS ---

function formatCurrency(value) {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value);
}

function getBadgeClass(status) {
    switch (status) {
        case "PAID":
            return "badge-success";
        case "PARTIALLY_PAID":
            return "badge-info";
        case "CANCELLED":
            return "badge-danger";
        case "ISSUED":
        case "PENDING":
        default:
            return "badge-warning";
    }
}

function escapeHtml(str) {
    return str
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

// Logout session clear
function logout() {
    localStorage.clear();
    window.location.href = "index.html";
}
