package com.codefactory.appstripe.transactions.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.codefactory.appstripe.transactions.domain.exception.InvalidTransactionStateException;

public class Transaction {

    private String id; // id de la transaccion
    private String merchantId; // id del comercio o dueño de la transaccion
    private BigDecimal amount; // monto de la transaccion
    private TransactionStatus status; // estado de la transaccion, se asigna automaticamente a CREATED
    private BigDecimal refundedAmount; // rastrea el monto reembolsado
    private LocalDateTime createdAt;   // fecha de creación para reportes

    /*Constructor #1 para crear una transaccion COMPLETAMENTE NUEVA
    el estado inicial de las transacciones es automáticamente asignado a CREATED*/
    public Transaction(String id, String merchantId, BigDecimal amount) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = TransactionStatus.CREATED; // Se asigna automáticamente al nacer
        this.refundedAmount = BigDecimal.ZERO;
    }


    public Transaction(
            String id,
            String merchantId,
            BigDecimal amount,
            TransactionStatus status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.refundedAmount = BigDecimal.ZERO;
    }
    /*Constructor 2 Para reconstruir una transacción que ya existe en la base de datos
    lo usara mapper para el paquete de infraestructura
     */
    public Transaction(String id, String merchantId, BigDecimal amount, TransactionStatus status) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.refundedAmount = BigDecimal.ZERO;
    }
    
    // Constructor completo para reconstruir desde BD con refundedAmount
    public Transaction(String id, String merchantId, BigDecimal amount,
                       TransactionStatus status, BigDecimal refundedAmount) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.status = status;
        this.refundedAmount = (refundedAmount != null) ? refundedAmount : BigDecimal.ZERO;
    }

    //bloquea transición si está en estado final y cambia a PROCESSING.
    public void startProcessing() {
        // si ya está aprovado, rechazado o fallido (estados finales) no se puede cambiar su estado a processing
        if (this.status == TransactionStatus.APPROVED || this.status == TransactionStatus.REJECTED || this.status == TransactionStatus.FAILED) {
            throw new InvalidTransactionStateException(
                    "Operación bloqueada: No se puede cambiar a PROCESSING un pago que ya está en estado final (" + this.status + ")."
            );
        }
        // Si no está en estado final, se puede cambiar a PROCESSING
        this.status = TransactionStatus.PROCESSING;
    }

    // Marca la transacción como aprobada (estado final)
    public void approve() {
        if (this.status == TransactionStatus.APPROVED || this.status == TransactionStatus.REJECTED || this.status == TransactionStatus.FAILED) {
            throw new InvalidTransactionStateException(
                    "Operación bloqueada: No se puede aprobar una transacción que ya está en estado final (" + this.status + ")."
            );
        }
        this.status = TransactionStatus.APPROVED;
    }

    // Marca la transacción como rechazada (estado final)
    public void reject() {
        if (this.status == TransactionStatus.APPROVED || this.status == TransactionStatus.REJECTED || this.status == TransactionStatus.FAILED) {
            throw new InvalidTransactionStateException(
                    "Operación bloqueada: No se puede rechazar una transacción que ya está en estado final (" + this.status + ")."
            );
        }
        this.status = TransactionStatus.REJECTED;
    }

    /**
     * Reembolsa el pago en su totalidad.
     * Regla 1: Solo se puede reembolsar si el estado es APPROVED o PARTIALLY_REFUNDED.
     * Regla 2: Si ya fue reembolsado totalmente, lanza excepción.
     */
    public void refundFull() {
        if (this.status == TransactionStatus.REFUNDED) {
            throw new InvalidTransactionStateException(
                "Este pago ya fue reembolsado en su totalidad. " +
                "No es posible procesar otro reembolso.");
        }
        if (this.status != TransactionStatus.APPROVED
                && this.status != TransactionStatus.PARTIALLY_REFUNDED) {
            throw new InvalidTransactionStateException(
                "No se puede reembolsar un pago que no está aprobado. " +
                "Estado actual: " + this.status);
        }
        this.refundedAmount = this.amount;
        this.status = TransactionStatus.REFUNDED;
    }

        /**
     * Reembolsa un monto parcial del pago.
     * Regla 1: Solo se puede reembolsar si el estado es APPROVED o PARTIALLY_REFUNDED.
     * Regla 2: El monto solicitado no puede superar el disponible para reembolso.
     * Regla 3: Si el monto reembolsado acumulado iguala el total, pasa a REFUNDED.
     */
    public void refundPartial(BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionStateException(
                "El monto del reembolso debe ser mayor a cero.");
        }
        if (this.status == TransactionStatus.REFUNDED) {
            throw new InvalidTransactionStateException(
                "Este pago ya fue reembolsado en su totalidad. " +
                "No es posible procesar otro reembolso.");
        }
        if (this.status != TransactionStatus.APPROVED
                && this.status != TransactionStatus.PARTIALLY_REFUNDED) {
            throw new InvalidTransactionStateException(
                "No se puede reembolsar un pago que no está aprobado. " +
                "Estado actual: " + this.status);
        }

        BigDecimal available = this.amount.subtract(this.refundedAmount);
        if (refundAmount.compareTo(available) > 0) {
            throw new InvalidTransactionStateException(
                "El monto solicitado (" + refundAmount + ") supera el disponible " +
                "para reembolso (" + available + ").");
        }

        this.refundedAmount = this.refundedAmount.add(refundAmount);

        // Si ya se reembolsó todo, cambia a estado final REFUNDED
        if (this.refundedAmount.compareTo(this.amount) == 0) {
            this.status = TransactionStatus.REFUNDED;
        } else {
            this.status = TransactionStatus.PARTIALLY_REFUNDED;
        }
    }


    /** Calcula cuánto queda disponible para reembolsar. */
    public BigDecimal getAvailableForRefund() {
        return this.amount.subtract(this.refundedAmount);
    }


    // getters, pero se puede usan @Data de lombok
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getId() {
        return id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }
}
