package com.codefactory.appstripe.transactions.api.dto;

import java.math.BigDecimal;

import com.codefactory.appstripe.transactions.domain.Transaction;
import com.codefactory.appstripe.transactions.domain.TransactionStatus;


import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionResponse {
    String id;
    String merchantId;
    BigDecimal amount;
    String status;
    String result;
    BigDecimal refundedAmount;   
    BigDecimal availableForRefund; 

    public static TransactionResponse fromDomain(Transaction transaction) {
        // Determinar el status visible hacia afuera
        String statusLabel;
        if (transaction.getStatus() == TransactionStatus.APPROVED
                || transaction.getStatus() == TransactionStatus.REJECTED
                || transaction.getStatus() == TransactionStatus.FAILED) {
            statusLabel = "COMPLETED";
        } else {
            statusLabel = transaction.getStatus().name();
        }

        // Determinar el result visible
        String result = null;
        if (transaction.getStatus() == TransactionStatus.APPROVED) {
            result = "APPROVED";
        } else if (transaction.getStatus() == TransactionStatus.REJECTED) {
            result = "REJECTED";
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .merchantId(transaction.getMerchantId())
                .amount(transaction.getAmount())
                .status(statusLabel)
                .result(result)
                .refundedAmount(nullSafeAmount(transaction.getRefundedAmount()))
                .availableForRefund(transaction.getAvailableForRefund())
                .build();
    }

    private static BigDecimal nullSafeAmount(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
