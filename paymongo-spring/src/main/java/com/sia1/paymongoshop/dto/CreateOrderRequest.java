package com.sia1.paymongoshop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * What the frontend (app.js) sends to POST /api/orders
 * Example JSON body:
 * {
 *   "items": [
 *     { "name": "SIA1 T-Shirt", "priceCentavos": 35000, "quantity": 1 }
 *   ]
 * }
 */
public class CreateOrderRequest {

    @NotEmpty(message = "Cart cannot be empty")
    @Valid
    private List<Item> items;

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        @NotNull
        private String name;

        @NotNull @Min(1)
        private Integer priceCentavos;

        @NotNull @Min(1)
        private Integer quantity;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getPriceCentavos() { return priceCentavos; }
        public void setPriceCentavos(Integer priceCentavos) { this.priceCentavos = priceCentavos; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
