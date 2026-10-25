// ==== Configuration ====
const API_BASE = "http://localhost:8080/api";

const PRODUCTS = [
    { id: 1, name: "SIA1 Class T-Shirt", description: "Official SIA1 cotton t-shirt.", priceCentavos: 35000 },
    { id: 2, name: "SIA1 Tote Bag", description: "Canvas tote bag with class logo.", priceCentavos: 15000 },
    { id: 3, name: "SIA1 Mug", description: "Ceramic mug, dishwasher safe.", priceCentavos: 12000 },
];

// ==== Cart (kept in localStorage so it survives a page refresh) ====
function getCart() {
    return JSON.parse(localStorage.getItem("cart") || "{}");
}
function saveCart(cart) {
    localStorage.setItem("cart", JSON.stringify(cart));
    renderCartCount();
}
function addToCart(productId) {
    const cart = getCart();
    cart[productId] = (cart[productId] || 0) + 1;
    saveCart(cart);
    showBanner("Added to cart!");
}
function renderCartCount() {
    const cart = getCart();
    const count = Object.values(cart).reduce((a, b) => a + b, 0);
    document.getElementById("cart-count").textContent = count;
}

function showBanner(msg) {
    const el = document.getElementById("banner");
    el.textContent = msg;
    el.style.display = "block";
    setTimeout(() => (el.style.display = "none"), 2000);
}

// Error message that stays on screen (the banner disappears after 2 seconds)
function showError(msg) {
    const el = document.getElementById("checkout-error");
    el.textContent = msg;
    el.style.display = "block";
}
function clearError() {
    const el = document.getElementById("checkout-error");
    el.textContent = "";
    el.style.display = "none";
}

function formatPesos(centavos) {
    return "\u20B1" + (centavos / 100).toFixed(2);
}

// ==== Render product grid ====
function renderProducts() {
    const grid = document.getElementById("product-grid");
    grid.innerHTML = "";
    PRODUCTS.forEach((p) => {
        const card = document.createElement("div");
        card.className = "card";
        card.setAttribute("data-cy", "product-card");
        card.innerHTML = `
            <h3 data-cy="product-name">${p.name}</h3>
            <p class="desc">${p.description}</p>
            <p class="price" data-cy="product-price">${formatPesos(p.priceCentavos)}</p>
            <button data-id="${p.id}" class="add-btn" data-cy="add-to-cart">Add to Cart</button>
        `;
        grid.appendChild(card);
    });

    grid.querySelectorAll(".add-btn").forEach((btn) => {
        btn.addEventListener("click", () => addToCart(Number(btn.dataset.id)));
    });
}

// ==== Checkout: POST cart to Spring Boot, then redirect to PayMongo ====
async function checkout() {
    clearError();
    const cart = getCart();
    const items = Object.entries(cart).map(([id, qty]) => {
        const product = PRODUCTS.find((p) => p.id === Number(id));
        return { name: product.name, priceCentavos: product.priceCentavos, quantity: qty };
    });

    if (items.length === 0) {
        showError("Your cart is empty.");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/orders`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ items }),
        });

        if (!res.ok) {
            let err = {};
            try { err = await res.json(); } catch (e) {}
            showError("Checkout failed: " + (err.detail || err.error || res.statusText));
            return;
        }

        const order = await res.json();
        localStorage.removeItem("cart");
        window.location.href = order.checkoutUrl;
    } catch (e) {
        showError("Could not reach backend at " + API_BASE + ". Is Spring Boot running?");
        console.error(e);
    }
}

// ==== Checkout button (added to the topbar) ====
function addCheckoutButton() {
    const btn = document.createElement("button");
    btn.textContent = "Checkout";
    btn.className = "checkout-btn-small";
    btn.setAttribute("data-cy", "checkout-button");
    btn.addEventListener("click", checkout);
    document.querySelector(".topbar").appendChild(btn);
}

// ==== Ping backend on load, just to show connection status ====
async function checkApiStatus() {
    const statusEl = document.getElementById("api-status");
    try {
        const res = await fetch(`${API_BASE}/orders/PING-CHECK`);
        statusEl.textContent = res.status === 404
            ? "connected to " + API_BASE
            : "responded with status " + res.status;
    } catch {
        statusEl.textContent = "NOT reachable at " + API_BASE + " - start the Spring Boot app";
        statusEl.style.color = "crimson";
    }
}

document.addEventListener("DOMContentLoaded", () => {
    renderProducts();
    renderCartCount();
    addCheckoutButton();
    checkApiStatus();
});
