// src/test/java/com/codefactory/appstripe/transactions/domain/TransactionRefundTest.java
package com.codefactory.appstripe.transactions.domain;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.codefactory.appstripe.transactions.domain.exception.InvalidTransactionStateException;

class TransactionRefundTest {

    // ── HU016: Reembolso total ─────────────────────────────────────────────

    @Test
    @DisplayName("HU016 - Escenario 1: Reembolso total exitoso de un pago aprobado")
    void shouldRefundFull_WhenTransactionIsApproved() {
        // Arrange
        Transaction tx = approvedTransaction("100.00");

        // Act
        tx.refundFull();

        // Assert
        assertEquals(TransactionStatus.REFUNDED, tx.getStatus());
        assertEquals(new BigDecimal("100.00"), tx.getRefundedAmount());
        assertEquals(0, tx.getAvailableForRefund().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("HU016 - Escenario 2: No se puede reembolsar un pago rechazado")
    void shouldThrowException_WhenRefundingRejectedTransaction() {
        // Arrange
        Transaction tx = new Transaction("tx-1", "mch-1",
                new BigDecimal("100.00"), TransactionStatus.REJECTED);

        // Act & Assert
        InvalidTransactionStateException ex = assertThrows(
                InvalidTransactionStateException.class, tx::refundFull);
        assertTrue(ex.getMessage().contains("no está aprobado"));
    }

    @Test
    @DisplayName("HU016 - Escenario 3: No se puede reembolsar dos veces el mismo pago")
    void shouldThrowException_WhenRefundingAlreadyRefundedTransaction() {
        // Arrange
        Transaction tx = approvedTransaction("100.00");
        tx.refundFull(); // primer reembolso

        // Act & Assert
        InvalidTransactionStateException ex = assertThrows(
                InvalidTransactionStateException.class, tx::refundFull);
        assertTrue(ex.getMessage().contains("ya fue reembolsado en su totalidad"));
    }

    @Test
    @DisplayName("HU016 - Reembolso total desde estado PARTIALLY_REFUNDED")
    void shouldRefundFull_WhenTransactionIsPartiallyRefunded() {
        // Arrange: pago de 100, ya devolvimos 30
        Transaction tx = new Transaction("tx-1", "mch-1",
                new BigDecimal("100.00"),
                TransactionStatus.PARTIALLY_REFUNDED,
                new BigDecimal("30.00"));

        // Act
        tx.refundFull();

        // Assert
        assertEquals(TransactionStatus.REFUNDED, tx.getStatus());
        assertEquals(new BigDecimal("100.00"), tx.getRefundedAmount());
    }

    // ── HU017: Reembolso parcial ───────────────────────────────────────────

    @Test
    @DisplayName("HU017 - Escenario 1: Reembolso parcial exitoso dentro del monto disponible")
    void shouldRefundPartial_WhenAmountIsWithinAvailable() {
        // Arrange
        Transaction tx = approvedTransaction("100.00");

        // Act
        tx.refundPartial(new BigDecimal("30.00"));

        // Assert
        assertEquals(TransactionStatus.PARTIALLY_REFUNDED, tx.getStatus());
        assertEquals(new BigDecimal("30.00"), tx.getRefundedAmount());
        assertEquals(new BigDecimal("70.00"), tx.getAvailableForRefund());
    }

    @Test
    @DisplayName("HU017 - Escenario 2: No se puede reembolsar más del monto disponible")
    void shouldThrowException_WhenRefundExceedsAvailable() {
        // Arrange: pago de 100, ya devolvimos 80, quedan 20
        Transaction tx = new Transaction("tx-1", "mch-1",
                new BigDecimal("100.00"),
                TransactionStatus.PARTIALLY_REFUNDED,
                new BigDecimal("80.00"));

        // Act & Assert: intentamos devolver 50 cuando solo hay 20
        InvalidTransactionStateException ex = assertThrows(
                InvalidTransactionStateException.class,
                () -> tx.refundPartial(new BigDecimal("50.00")));
        assertTrue(ex.getMessage().contains("supera el disponible"));
    }

    @Test
    @DisplayName("HU017 - Acumulación de reembolsos parciales hasta llegar a REFUNDED")
    void shouldTransitionToRefunded_WhenCumulativeRefundsEqualTotal() {
        // Arrange
        Transaction tx = approvedTransaction("100.00");

        // Act: dos reembolsos que suman el total
        tx.refundPartial(new BigDecimal("60.00"));
        tx.refundPartial(new BigDecimal("40.00"));

        // Assert
        assertEquals(TransactionStatus.REFUNDED, tx.getStatus());
        assertEquals(new BigDecimal("100.00"), tx.getRefundedAmount());
    }

    @Test
    @DisplayName("HU017 - No se puede hacer reembolso parcial sobre un pago no aprobado")
    void shouldThrowException_WhenPartialRefundOnNonApprovedTransaction() {
        // Arrange
        Transaction tx = new Transaction("tx-1", "mch-1",
                new BigDecimal("100.00"), TransactionStatus.CREATED);

        // Act & Assert
        assertThrows(InvalidTransactionStateException.class,
                () -> tx.refundPartial(new BigDecimal("20.00")));
    }

    @Test
    @DisplayName("HU017 - Monto de reembolso cero lanza excepción")
    void shouldThrowException_WhenRefundAmountIsZero() {
        Transaction tx = approvedTransaction("100.00");
        assertThrows(InvalidTransactionStateException.class,
                () -> tx.refundPartial(BigDecimal.ZERO));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Transaction approvedTransaction(String amount) {
        return new Transaction("tx-1", "mch-1",
                new BigDecimal(amount), TransactionStatus.APPROVED,
                BigDecimal.ZERO);
    }
}