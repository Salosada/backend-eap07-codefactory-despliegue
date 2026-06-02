package com.codefactory.appstripe.transactions.application;

import com.codefactory.appstripe.transactions.application.port.IAuditPublisherPort;
import com.codefactory.appstripe.transactions.application.port.ITransactionRepositoryPort;
import com.codefactory.appstripe.transactions.application.query.PaymentStatusDistribution;
import com.codefactory.appstripe.transactions.application.query.TransactionStatusCount;
import com.codefactory.appstripe.transactions.domain.Transaction;
import com.codefactory.appstripe.transactions.domain.TransactionStatus;
import com.codefactory.appstripe.transactions.domain.exception.InvalidTransactionStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.codefactory.appstripe.transactions.application.port.IMerchantNotifierPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionApplicationServiceTest {

    @Mock
    private ITransactionRepositoryPort transactionRepositoryPort;

    @Mock
    private IAuditPublisherPort auditPublisherPort;

    @Mock
    private IMerchantNotifierPort merchantNotifierPort;

    @InjectMocks
    private TransactionApplicationService transactionApplicationService;

    // ESCENARIO 1: Transición válida
    @Test
    @DisplayName("Criterio 1: Transición válida de CREATED a PROCESSING")
    void shouldStartProcessing_SaveAuditAndNotify_WhenTransactionIsValid() {
        // Arrange
        String transactionId = "uuid-1234";

        // Mockeamos la entidad Transaction para controlar su comportamiento en esta prueba
        Transaction mockTransaction = mock(Transaction.class);
        when(mockTransaction.getId()).thenReturn(transactionId);

        // Simulamos que el estado original es CREATED y luego cambia a PROCESSING
        when(mockTransaction.getStatus())
                .thenReturn(TransactionStatus.CREATED)    // Primera llamada (oldStatus)
                .thenReturn(TransactionStatus.PROCESSING); // Segunda llamada (newStatus)

        when(transactionRepositoryPort.findById(transactionId)).thenReturn(Optional.of(mockTransaction));

        // Act
        transactionApplicationService.startProcessing(transactionId);

        // Assert
        // 1. Verificamos que el servicio delegó la responsabilidad al dominio
        verify(mockTransaction, times(1)).startProcessing();

        // 2. Verificamos que se guardó en BD
        verify(transactionRepositoryPort, times(1)).save(mockTransaction);

        // 3. Verificamos la auditoría indicando estado anterior y nuevo
        verify(auditPublisherPort, times(1)).publishStatusChange(transactionId, TransactionStatus.CREATED, TransactionStatus.PROCESSING);

        // 4. Verificamos la notificación al comercio
        verify(merchantNotifierPort, times(1)).notifyProcessingStart(mockTransaction);
    }

    // ESCENARIO 2: Transición no valida
    @Test
    @DisplayName("Criterio 2: Bloquear operación y lanzar excepción si la transacción ya está en estado final")
    void shouldBlockOperationAndThrowException_WhenTransactionIsInFinalState() {
        // Arrange
        String transactionId = "uuid-1234";
        Transaction mockTransaction = mock(Transaction.class);

        when(transactionRepositoryPort.findById(transactionId)).thenReturn(Optional.of(mockTransaction));

        // Simulamos la regla de negocio del dominio: si ya está en estado final,
        // el metodo startProcessing() de la entidad lanza la excepción
        doThrow(new InvalidTransactionStateException("No se puede cambiar el estado de una transacción finalizada"))
                .when(mockTransaction).startProcessing();

        // Act & Assert
        assertThrows(InvalidTransactionStateException.class, () -> {
            transactionApplicationService.startProcessing(transactionId);
        }, "Debe lanzar InvalidTransactionStateException al detectar una transición no válida");

        // Assert: Confirmamos que se bloqueó la operación (no se guardó, no se auditó, no se notificó)
        verify(transactionRepositoryPort, never()).save(any(Transaction.class));
        verify(auditPublisherPort, never()).publishStatusChange(anyString(), any(), any());
        verify(merchantNotifierPort, never()).notifyProcessingStart(any(Transaction.class));
    }

    @Test
    @DisplayName("HU020: debe calcular distribución de pagos por estado")
    void shouldCalculatePaymentStatusDistribution() {
        String merchantId = "mch_123";
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);

        when(transactionRepositoryPort.countByMerchantIdAndStatusInAndCreatedAtBetween(
                eq(merchantId),
                anyList(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(
                new TransactionStatusCount(TransactionStatus.APPROVED, 8),
                new TransactionStatusCount(TransactionStatus.REJECTED, 2),
                new TransactionStatusCount(TransactionStatus.FAILED, 0)
        ));

        PaymentStatusDistribution result =
                transactionApplicationService.getPaymentStatusDistribution(merchantId, from, to);

        assertEquals(10, result.totalFinalized());
        assertEquals("80.00", result.approvalRate().toPlainString());
        assertEquals(4, result.distribution().size());

        verify(transactionRepositoryPort).countByMerchantIdAndStatusInAndCreatedAtBetween(
                eq(merchantId),
                anyList(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }
}