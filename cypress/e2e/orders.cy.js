const API = 'http://localhost:8080/api'

describe('Orders', () => {
  it('E2E-02: creates a pending order with the right total', () => {
    cy.intercept('POST', '**/api/orders').as('createOrder')
    cy.visit('/')
    cy.contains('[data-cy=product-card]', 'SIA1 Class T-Shirt')
      .find('[data-cy=add-to-cart]')
      .click()
    cy.get('[data-cy=checkout-button]').click()
    cy.wait('@createOrder').its('response.body.orderCode').then((code) => {
      cy.request(`${API}/orders/${code}`).its('body').should((order) => {
        expect(order.status).to.eq('pending')
        expect(order.totalCentavos).to.eq(35000)
      })
    })
  })

  it('E2E-05: updates the order on the success page', () => {
    cy.createOrder().then((order) => {
      // Cypress cannot pay on PayMongo, so this test-only endpoint plays the part
      // of PayMongo confirming the payment.
      cy.request('POST', `${API}/test/orders/${order.orderCode}/force-paid`)
      cy.visit(`/success.html?order=${order.orderCode}`)
      cy.get('[data-cy=result]').should('contain', 'Payment received')
      cy.request(`${API}/orders/${order.orderCode}`).its('body.status').should('eq', 'paid')
    })
  })

  it('E2E-05: keeps the order pending when the success URL is opened without paying', () => {
    cy.createOrder().then((order) => {
      cy.visit(`/success.html?order=${order.orderCode}`)
      cy.get('[data-cy=result]').should('contain', 'not yet confirmed')
      cy.request(`${API}/orders/${order.orderCode}`).its('body.status').should('eq', 'pending')
    })
  })

  it('E2E-06: never marks a cancelled order as paid', () => {
    cy.intercept('POST', '**/api/orders/*/cancel').as('cancelOrder')
    cy.createOrder().then((order) => {
      cy.visit(`/cancel.html?order=${order.orderCode}`)
      cy.get('[data-cy=result]').should('contain', 'Checkout cancelled')
      cy.wait('@cancelOrder')
      cy.request(`${API}/orders/${order.orderCode}`).its('body.status').should('eq', 'cancelled')
    })
  })
})
