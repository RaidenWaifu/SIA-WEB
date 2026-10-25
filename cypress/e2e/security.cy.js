describe('Security', () => {
  it('E2E-08: keeps the secret key out of the browser', () => {
    // Watch for any call from the browser straight to PayMongo's API
    cy.intercept('https://api.paymongo.com/**', cy.spy().as('browserToPayMongo'))
    // Stub the order call so the page stays on our shop
    cy.intercept('POST', '**/api/orders', { statusCode: 500, body: { error: 'stub' } }).as('createOrder')

    cy.visit('/')
    cy.get('[data-cy=add-to-cart]').first().click()
    cy.get('[data-cy=checkout-button]').click()
    cy.wait('@createOrder')
    cy.get('@browserToPayMongo').should('not.have.been.called')

    // None of the files the browser receives may contain a secret key
    ;['/', '/index.html', '/app.js', '/success.html', '/cancel.html'].forEach((path) => {
      cy.request(path).its('body').should('not.match', /sk_(test|live)_/)
    })
  })
})
