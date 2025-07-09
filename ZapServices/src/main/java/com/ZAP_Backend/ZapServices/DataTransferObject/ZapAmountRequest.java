package com.ZAP_Backend.ZapServices.DataTransferObject;

public class ZapAmountRequest {
    private Double amount;

    public ZapAmountRequest() {
    }

    public ZapAmountRequest(Double amount) {
        this.amount = amount;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
} 