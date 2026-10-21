package com.sia1.paymongoshop.dto;

public class OrderResponse {
    private String orderCode;
    private String status;
    private Integer totalCentavos;
    private String checkoutUrl; // only present right after creation

    public OrderResponse(String orderCode, String status, Integer totalCentavos, String checkoutUrl) {
        this.orderCode = orderCode;
        this.status = status;
        this.totalCentavos = totalCentavos;
        this.checkoutUrl = checkoutUrl;
    }

    public String getOrderCode() { return orderCode; }
    public String getStatus() { return status; }
    public Integer getTotalCentavos() { return totalCentavos; }
    public String getCheckoutUrl() { return checkoutUrl; }
}
