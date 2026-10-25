const API = 'http://localhost:8080/api'

// Start every test with an empty orders table (needs app.test-mode=true on the backend)
beforeEach(() => {
  cy.request('POST', `${API}/test/reset`)
})

// Create an order straight through the API (no browser). Gives back the new order.
Cypress.Commands.add('createOrder', () =>
  cy.request('POST', `${API}/orders`, {
    items: [{ name: 'SIA1 Class T-Shirt', priceCentavos: 35000, quantity: 1 }],
  }).its('body')
)
