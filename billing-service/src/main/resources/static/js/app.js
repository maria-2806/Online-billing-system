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

    // Clear search values when switching tabs
    const searchCustomers = document.getElementById("searchCustomers");
    if (searchCustomers) searchCustomers.value = "";
    const searchInvoices = document.getElementById("searchInvoices");
    if (searchInvoices) searchInvoices.value = "";
    const searchPayments = document.getElementById("searchPayments");
    if (searchPayments) searchPayments.value = "";

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
                <td>${formatDate(bill.createdAt)}</td>
                <td>${formatCurrency(bill.subtotal)}</td>
                <td>${formatCurrency(bill.tax)}</td>
                <td style="font-weight: 600; color: var(--text-primary);">${formatCurrency(bill.total)}</td>
                <td><span class="badge ${getBadgeClass(bill.status)}">${bill.status}</span></td>
                <td style="text-align: right; padding-right: 20px;">
                    <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="viewInvoice(${bill.id})">View Invoice</button>
                </td>
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
                    <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="openEditCustomerModal(${c.id}, '${escapeHtml(c.name)}', '${escapeHtml(c.email)}', '${escapeHtml(c.phone || '')}')">Edit</button>
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
                <td>${formatDate(bill.createdAt)}</td>
                <td>${formatCurrency(bill.subtotal)}</td>
                <td>${formatCurrency(bill.tax)}</td>
                <td style="font-weight: 600; color: var(--text-primary);">${formatCurrency(bill.total)}</td>
                <td><span class="badge ${getBadgeClass(bill.status)}">${bill.status}</span></td>
                <td style="text-align: right; padding-right: 20px;">
                    <button class="btn btn-secondary" style="padding: 6px 12px; font-size: 0.8rem;" onclick="viewInvoice(${bill.id})">View Invoice</button>
                </td>
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
        const [bills, payments] = await Promise.all([
            fetch("/api/billing/invoices").then(res => res.json()),
            fetch("/api/billing/payments").then(res => res.json())
        ]);

        const select = document.getElementById("payInvoiceSelect");
        select.innerHTML = '<option value="" disabled selected>Select an unpaid invoice</option>';

        // Filter bills that are not fully settled or cancelled
        const unpaidBills = bills.filter(b => b.status !== "PAID" && b.status !== "CANCELLED");

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

        // Build a mapping from billId to customerName
        const billIdToCustomerName = {};
        bills.forEach(b => {
            billIdToCustomerName[b.id] = b.customerName;
        });

        // Render payments history table
        const tbody = document.getElementById("paymentsHistoryTable");
        tbody.innerHTML = "";

        if (payments.length === 0) {
            tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-secondary);">No payments recorded.</td></tr>`;
            return;
        }

        // Render in reverse order (newest first)
        payments.slice().reverse().forEach(p => {
            const customerName = billIdToCustomerName[p.billId] || `Invoice #${p.billId}`;
            const tr = document.createElement("tr");
            tr.innerHTML = `
                <td>#${p.id}</td>
                <td>#${p.billId}</td>
                <td style="font-weight: 600;">${customerName}</td>
                <td>${formatDate(p.paymentDate)}</td>
                <td style="font-weight: 600; color: var(--success);">${formatCurrency(p.amountPaid)}</td>
                <td>${formatPaymentMethod(p.method)}</td>
                <td><span class="badge ${getBadgeClass(p.status)}">${p.status}</span></td>
            `;
            tbody.appendChild(tr);
        });

    } catch (err) {
        console.error("Error loading payments data", err);
        showToast("Failed to load payments and invoice data", "error");
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
function openEditCustomerModal(id, name, email, phone) {
    document.getElementById("editCustId").value = id;
    document.getElementById("editCustName").value = name;
    document.getElementById("editCustEmail").value = email;
    document.getElementById("editCustPhone").value = phone || "";
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
        const phone = document.getElementById("custPhone").value.trim();
 
        try {
            const res = await fetch("/api/billing/customers", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name, email, phone })
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
        const phone = document.getElementById("editCustPhone").value.trim();
 
        try {
            const res = await fetch(`/api/billing/customers/${id}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name, email, phone })
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
            
            // Reset form, hide preview panel, and reload history
            document.getElementById("paymentForm").reset();
            document.getElementById("payAmountGroup").style.display = "none";
            loadPaymentsData();
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
        case "SUCCESS":
            return "badge-success";
        case "PARTIALLY_PAID":
            return "badge-info";
        case "CANCELLED":
        case "FAILED":
            return "badge-danger";
        case "ISSUED":
        case "PENDING":
        default:
            return "badge-warning";
    }
}

function formatPaymentMethod(method) {
    if (!method) return "N/A";
    if (method === "UPI") return "UPI";
    return method.replace(/_/g, " ").replace(/\b\w/g, c => c.toUpperCase());
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

// Date formatter
function formatDate(dateString) {
    if (!dateString) return "N/A";
    const d = new Date(dateString);
    if (isNaN(d.getTime())) return "N/A";
    return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

// --- FILTER / SEARCH ACTIONS ---

function filterCustomersTable() {
    const query = document.getElementById("searchCustomers").value.toLowerCase().trim();
    const rows = document.querySelectorAll("#customersTable tr");
    rows.forEach(row => {
        if (row.cells.length < 3) return;
        const name = row.cells[1]?.textContent.toLowerCase() || "";
        const email = row.cells[2]?.textContent.toLowerCase() || "";
        if (name.includes(query) || email.includes(query)) {
            row.style.display = "";
        } else {
            row.style.display = "none";
        }
    });
}

function filterInvoicesTable() {
    const query = document.getElementById("searchInvoices").value.toLowerCase().trim();
    const rows = document.querySelectorAll("#invoicesTable tr");
    rows.forEach(row => {
        if (row.cells.length < 8) return;
        const billId = row.cells[0]?.textContent.toLowerCase() || "";
        const customerName = row.cells[2]?.textContent.toLowerCase() || "";
        const status = row.cells[7]?.textContent.toLowerCase() || "";
        if (billId.includes(query) || customerName.includes(query) || status.includes(query)) {
            row.style.display = "";
        } else {
            row.style.display = "none";
        }
    });
}

function filterPaymentsTable() {
    const query = document.getElementById("searchPayments").value.toLowerCase().trim();
    const rows = document.querySelectorAll("#paymentsHistoryTable tr");
    rows.forEach(row => {
        if (row.cells.length < 7) return;
        const paymentId = row.cells[0]?.textContent.toLowerCase() || "";
        const customerName = row.cells[2]?.textContent.toLowerCase() || "";
        const method = row.cells[5]?.textContent.toLowerCase() || "";
        if (paymentId.includes(query) || customerName.includes(query) || method.includes(query)) {
            row.style.display = "";
        } else {
            row.style.display = "none";
        }
    });
}

// View and Download Invoice PDF Details
async function viewInvoice(billId) {
    try {
        // Fetch bill, customer and payments data in parallel
        const bill = await fetch("/api/billing/invoices").then(r => r.json()).then(list => list.find(b => b.id == billId));
        if (!bill) {
            showToast("Invoice not found", "error");
            return;
        }

        const [customer, payments] = await Promise.all([
            fetch(`/api/billing/customers/${bill.customerId}`).then(res => res.json()),
            fetch(`/api/billing/payments?billId=${billId}`).then(res => res.json())
        ]);

        // Calculate totals
        const totalPaid = payments
            .filter(p => p.status === "SUCCESS")
            .reduce((sum, p) => sum + p.amountPaid, 0);
        const remainingBalance = Math.max(0, bill.total - totalPaid);

        // Update modal DOM elements
        document.getElementById("invoiceDetailId").textContent = `#${bill.id}`;
        document.getElementById("invoiceDetailDate").textContent = formatDate(bill.createdAt);
        
        const statusBadge = document.getElementById("invoiceDetailStatus");
        statusBadge.textContent = bill.status;
        statusBadge.className = `badge ${getBadgeClass(bill.status)}`;

        document.getElementById("invoiceDetailCustName").textContent = customer.name;
        document.getElementById("invoiceDetailCustId").textContent = customer.id;
        document.getElementById("invoiceDetailCustEmail").textContent = customer.email;
        document.getElementById("invoiceDetailCustPhone").textContent = customer.phone || 'N/A';

        document.getElementById("invoiceRowSubtotal").textContent = formatCurrency(bill.subtotal);
        document.getElementById("invoiceRowTax").textContent = formatCurrency(bill.tax);
        document.getElementById("invoiceRowTotal").textContent = formatCurrency(bill.total);

        document.getElementById("invoiceSummarySubtotal").textContent = formatCurrency(bill.subtotal);
        document.getElementById("invoiceSummaryTax").textContent = formatCurrency(bill.tax);
        document.getElementById("invoiceSummaryTotal").textContent = formatCurrency(bill.total);
        document.getElementById("invoiceSummaryPaid").textContent = formatCurrency(totalPaid);
        document.getElementById("invoiceSummaryBalance").textContent = formatCurrency(remainingBalance);

        // Render payments history in invoice
        const paymentsListContainer = document.getElementById("invoicePaymentsList");
        paymentsListContainer.innerHTML = "";
        const successfulPayments = payments.filter(p => p.status === "SUCCESS");

        if (successfulPayments.length === 0) {
            paymentsListContainer.innerHTML = '<p style="font-style: italic; color: var(--text-secondary);">No payments recorded yet.</p>';
        } else {
            successfulPayments.forEach(p => {
                const itemDiv = document.createElement("div");
                itemDiv.style.display = "flex";
                itemDiv.style.justifyContent = "space-between";
                itemDiv.style.marginBottom = "4px";
                itemDiv.innerHTML = `
                    <span>${formatDate(p.paymentDate)} - ${formatPaymentMethod(p.method)}</span>
                    <span style="font-weight:600; color:var(--success);">${formatCurrency(p.amountPaid)}</span>
                `;
                paymentsListContainer.appendChild(itemDiv);
            });
        }

        // Set action for download PDF
        document.getElementById("downloadPdfBtn").onclick = () => downloadInvoicePDF(bill);

        openModal("viewInvoiceModal");
    } catch (err) {
        console.error("Error loading invoice preview details", err);
        showToast("Failed to fetch invoice details", "error");
    }
}

function downloadInvoicePDF(bill) {
    const element = document.getElementById('invoicePrintArea');
    const opt = {
        margin:       10,
        filename:     `invoice_${bill.id}.pdf`,
        image:        { type: 'jpeg', quality: 0.98 },
        html2canvas:  { scale: 2, useCORS: true, letterRendering: true },
        jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' }
    };
    html2pdf().set(opt).from(element).save();
}

