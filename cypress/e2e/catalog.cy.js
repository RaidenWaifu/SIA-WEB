describe('Catalog', () => {
  it('E2E-01: shows the catalog with correct peso prices', () => {
    cy.visit('/')
    cy.get('[data-cy=product-card]').should('have.length', 3)

    cy.contains('[data-cy=product-card]', 'SIA1 Class T-Shirt')
      .find('[data-cy=product-price]')
      .should('have.text', '\u20B1350.00')

    cy.contains('[data-cy=product-card]', 'SIA1 Tote Bag')
      .find('[data-cy=product-price]')
      .should('have.text', '\u20B1150.00')

    cy.contains('[data-cy=product-card]', 'SIA1 Mug')
      .find('[data-cy=product-price]')
      .should('have.text', '\u20B1120.00')
  })
})
