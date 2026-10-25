# SIA1 PayMongo Shop — Spring Boot + HTML/CSS/JS

Two separate folders, matching your phase plan:

```
paymongo-spring/     <- PHASE 2: Java Spring Boot backend (REST API + MySQL)
paymongo-frontend/   <- PHASE 3: HTML/CSS/JS, opened with VS Code Live Server
```

## How the pieces connect

```
Browser (frontend, port 5500 via Live Server)
    │  fetch() → JSON
    ▼
Spring Boot REST API (port 8080)
    │  JPA/Hibernate
    ▼
MySQL "orders" + "order_items" tables (via XAMPP)

Spring Boot ──HTTPS──> PayMongo API (checkout session, payment verification)
```

The frontend never talks to PayMongo or MySQL directly — it only calls your
Spring Boot API. Spring Boot is the only thing holding the PayMongo secret key
and the only thing with a JDBC connection to MySQL.

## PHASE 1 — Database (you already did this)

Make sure `order_items` also exists — run `paymongo-spring/order_items.sql`
in phpMyAdmin against your existing `paymongo_shop` database (safe to run;
it only creates the table if missing, doesn't touch your `orders` data).

## PHASE 2 — Run the Spring Boot backend in VS Code

1. Install the **Extension Pack for Java** and **Spring Boot Extension Pack** in VS Code.
2. Open the `paymongo-spring` folder in VS Code (`File → Open Folder`).
3. Edit `src/main/resources/application.properties`:
   - Set your MySQL username/password if not the XAMPP default (`root` / blank).
   - Paste your PayMongo **test secret key** into `paymongo.secret-key`.
4. Start MySQL from the XAMPP Control Panel (Apache isn't needed for this phase — Spring Boot is its own server).
5. Open `PaymongoShopApplication.java` and click **Run** (or press `F5`).
6. Confirm it's alive: visit `http://localhost:8080/api/orders/PING-CHECK` in a browser —
   a `{"timestamp":...,"status":404,...}` JSON response means the backend is up and reachable
   (404 just means that specific order code doesn't exist, which is expected).

## PHASE 3 — Run the frontend

1. Open the `paymongo-frontend` folder in VS Code (can be a second VS Code window).
2. Install the **Live Server** extension if you don't have it.
3. Right-click `index.html` → **Open with Live Server**. It should open at `http://127.0.0.1:5500`.
4. The footer of the page shows the backend connection status — confirm it says "connected" before testing checkout.

If Live Server uses a different port than 5500, update:
- `paymongo-frontend/app.js` → not needed (frontend doesn't need its own URL)
- `paymongo-spring/src/main/resources/application.properties` → `app.cors.allowed-origins` and `app.success-url` / `app.cancel-url`

## PHASE 4 — Test a payment

1. On the storefront, click **Add to Cart** on a product, then **Checkout** (top right).
2. The frontend sends your cart to `POST /api/orders`. Spring Boot saves the order, calls PayMongo's Checkout API, and returns a `checkoutUrl`.
3. Your browser is redirected to PayMongo's own hosted payment page.
4. Use a [PayMongo test card](https://developers.paymongo.com/docs/testing) (or simulate GCash/Maya) to "pay."
5. PayMongo redirects you back to `success.html`, which calls `GET /api/orders/{code}/verify` — Spring Boot re-checks the payment with PayMongo server-side before marking the order `paid` in MySQL.

## PHASE 5 — Webhooks (not built yet)

Right now, payment status only updates when the customer is redirected back to
`success.html`. If they close the tab mid-payment, the order stays `pending`
even if they actually paid. A PayMongo **webhook** (a POST PayMongo sends to
your server the moment payment succeeds) fixes that — happy to build a
`/api/webhooks/paymongo` endpoint next if you want this phase done too.

## PHASE 6–8 (not built yet)

Success/cancel pages, an admin dashboard (list/filter orders in MySQL), and
security hardening (input validation, rate limiting, hiding stack traces) are
still open — let me know which one you want to tackle next.

## Running the E2E tests

1. Start MySQL and create the `paymongo_shop` database (see `paymongo-spring/order_items.sql`).
2. Copy `paymongo-spring/src/main/resources/application.properties.example` to `application.properties`. Fill in your database user and password and your own PayMongo test secret key (`sk_test_...`). Keep `app.test-mode=true`.
3. Start the backend: run `PaymongoShopApplication.java` (port 8080).
4. Install the tools once: `npm install`
5. Start the shop page: `npm run serve` (http://127.0.0.1:5500)
6. In another terminal, run the tests: `npm run test:e2e`

The tests reset the orders table before each test, so use a database you do not need.
