const API = 'http://localhost:8080/api'

describe('Checkout', () => {
  function addFirstItemAndCheckout() {
    cy.visit('/')
    cy.get('[data-cy=add-to-cart]').first().click()
    cy.get('[data-cy=checkout-button]').click()
  }

  it('E2E-03: gets a checkout URL from PayMongo', () => {
    cy.intercept('POST', '**/api/orders').as('createOrder')
    addFirstItemAndCheckout()
    cy.wait('@createOrder').then(({ response }) => {
      expect(response.statusCode).to.be.oneOf([200, 201])
      expect(response.body.checkoutUrl.startsWith('https://checkout.paymongo.com/')).to.eq(true)
    })
  })

  it('E2E-04: hands the customer over to PayMongo', () => {
    addFirstItemAndCheckout()
    // PayMongo's page is another website. Only check where we landed. Touch nothing.
    cy.origin('https://checkout.paymongo.com', () => {
      Cypress.on('uncaught:exception', () => false)
      cy.location('hostname').should('eq', 'checkout.paymongo.com')
    })
  })

  it('E2E-07: shows a clear error when checkout fails', () => {
    cy.intercept('POST', '**/api/orders', {
      statusCode: 500,
      body: { error: 'Payment unavailable' },
    }).as('createOrder')
    addFirstItemAndCheckout()
    cy.wait('@createOrder')
    cy.get('[data-cy=checkout-error]').should('be.visible').and('contain', 'Checkout failed')
    cy.location('hostname').should('eq', '127.0.0.1')
    cy.get('[data-cy=cart-count]').should('have.text', '1')
    cy.request(`${API}/test/orders/count?status=paid`).its('body.count').should('eq', 0)
  })
})
