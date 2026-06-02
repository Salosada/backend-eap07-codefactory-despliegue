package com.codefactory.appstripe.transactions.application;

import com.codefactory.appstripe.transactions.application.port.IAuditPublisherPort;
import com.codefactory.appstripe.transactions.application.port.IMerchantNotifierPort;
import com.codefactory.appstripe.transactions.application.port.ITransactionRepositoryPort;
import com.codefactory.appstripe.transactions.application.query.PaymentStatusDistribution;
import com.codefactory.appstripe.transactions.application.query.PaymentStatusDistributionItem;
import com.codefactory.appstripe.transactions.application.query.TransactionStatusCount;
import com.codefactory.appstripe.transactions.application.query.TransactionVolumeReport;
import com.codefactory.appstripe.transactions.domain.Transaction;
import com.codefactory.appstripe.transactions.domain.TransactionStatus;
import org.springframework.stereotype.Service;


import com.codefactory.appstripe.transactions.application.query.TransactionVolumeGroupBy;
import com.codefactory.appstripe.transactions.application.query.TransactionVolumeReportItem;

import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionApplicationService {
    private final ITransactionRepositoryPort transactionRepositoryPort;
    private final IAuditPublisherPort auditPublisherPort;
    private final IMerchantNotifierPort merchantNotifierPort;

    public TransactionApplicationService(ITransactionRepositoryPort transactionRepositoryPort,
                                         IAuditPublisherPort auditPublisherPort,
                                         IMerchantNotifierPort merchantNotifierPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.auditPublisherPort = auditPublisherPort;
        this.merchantNotifierPort = merchantNotifierPort;
    }

    public Transaction assignInitialStatusCreated(Transaction transaction) {
        // Guardar en base de datos
        Transaction savedTransaction = transactionRepositoryPort.save(transaction);

        // Guardar en auditoría
        auditPublisherPort.publishStatusChange(savedTransaction.getId(), null, savedTransaction.getStatus());

        // Retornar la transacción guardada
        return savedTransaction;
    }

    public void startProcessing(String transactionId) {
        Transaction transaction = transactionRepositoryPort.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada con ID: " + transactionId));

        // guardar el estado anterior u old
        TransactionStatus oldStatus = transaction.getStatus();

        // procesar el pago
        transaction.startProcessing();

        // guardar en base de datos
        transactionRepositoryPort.save(transaction);

        // guardar en auditoria
        auditPublisherPort.publishStatusChange(transaction.getId(), oldStatus, transaction.getStatus());

        // notificar procesamiento al comercio
        merchantNotifierPort.notifyProcessingStart(transaction);
    }

    public Transaction getById(String transactionId) {
        return transactionRepositoryPort.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada con ID: " + transactionId));
    }

    public java.util.List<Transaction> getByMerchantId(String merchantId) {
        return transactionRepositoryPort.findByMerchantId(merchantId);
    }

    public List<Transaction> getAllTransactions() {
        return transactionRepositoryPort.findAll();
    }

    public Transaction completeTransaction(String transactionId, String result, String authorizationCode, String rejectionReason) {
        Transaction transaction = transactionRepositoryPort.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada con ID: " + transactionId));

        TransactionStatus oldStatus = transaction.getStatus();

        if ("APPROVED".equalsIgnoreCase(result)) {
            transaction.approve();
        } else if ("REJECTED".equalsIgnoreCase(result)) {
            transaction.reject();
        } else {
            throw new RuntimeException("Resultado desconocido: " + result);
        }

        Transaction saved = transactionRepositoryPort.save(transaction);

        auditPublisherPort.publishStatusChange(saved.getId(), oldStatus, saved.getStatus());

        merchantNotifierPort.notifyProcessingCompletion(saved, result, authorizationCode, rejectionReason);

        return saved;
    }


    public PaymentStatusDistribution getPaymentStatusDistribution(String merchantId, LocalDate from, LocalDate to) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalStateException("El token no tiene un comercio asociado");
        }

        if (from == null || to == null) {
            throw new IllegalArgumentException("Las fechas from y to son obligatorias");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }

        LocalDateTime fromInclusive = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<TransactionStatus> queryStatuses = List.of(
                TransactionStatus.APPROVED,
                TransactionStatus.REJECTED,
                TransactionStatus.FAILED,
                TransactionStatus.PARTIALLY_REFUNDED,
                TransactionStatus.REFUNDED
        );

        List<TransactionStatusCount> counts =
                transactionRepositoryPort.countByMerchantIdAndStatusInAndCreatedAtBetween(
                        merchantId,
                        queryStatuses,
                        fromInclusive,
                        toExclusive
                );

        Map<TransactionStatus, Long> countByStatus = new EnumMap<>(TransactionStatus.class);

        for (TransactionStatus status : queryStatuses) {
            countByStatus.put(status, 0L);
        }

        for (TransactionStatusCount count : counts) {
            countByStatus.put(count.status(), count.count());
        }

        // Compatibilidad: registros legacy en PARTIALLY_REFUNDED cuentan como aprobadas activas
        long legacyPartial = countByStatus.getOrDefault(TransactionStatus.PARTIALLY_REFUNDED, 0L);
        if (legacyPartial > 0) {
            countByStatus.merge(TransactionStatus.APPROVED, legacyPartial, Long::sum);
            countByStatus.put(TransactionStatus.PARTIALLY_REFUNDED, 0L);
        }

        List<TransactionStatus> reportStatuses = List.of(
                TransactionStatus.APPROVED,
                TransactionStatus.REJECTED,
                TransactionStatus.FAILED,
                TransactionStatus.REFUNDED
        );

        long totalFinalized = reportStatuses.stream()
                .mapToLong(status -> countByStatus.getOrDefault(status, 0L))
                .sum();

        List<PaymentStatusDistributionItem> distribution = reportStatuses.stream()
                .map(status -> new PaymentStatusDistributionItem(
                        status,
                        countByStatus.getOrDefault(status, 0L),
                        percentage(countByStatus.getOrDefault(status, 0L), totalFinalized)
                ))
                .toList();

        long successfulPayments = countByStatus.getOrDefault(TransactionStatus.APPROVED, 0L)
                + countByStatus.getOrDefault(TransactionStatus.REFUNDED, 0L);

        BigDecimal approvalRate = percentage(successfulPayments, totalFinalized);

        return new PaymentStatusDistribution(
                from,
                to,
                totalFinalized,
                approvalRate,
                distribution
        );
    }

    private BigDecimal percentage(long count, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
    public Transaction refundFull(String transactionId, String reason) {
        Transaction transaction = transactionRepositoryPort.findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transacción no encontrada con ID: " + transactionId));

        TransactionStatus oldStatus = transaction.getStatus();

        // La regla de negocio vive en el dominio
        transaction.refundFull();

        Transaction saved = transactionRepositoryPort.save(transaction);

        auditPublisherPort.publishStatusChange(
                saved.getId(), oldStatus, saved.getStatus());

        merchantNotifierPort.notifyRefund(saved, saved.getAmount(), reason);

        return saved;
    }

    public Transaction refundPartial(String transactionId,
                                     java.math.BigDecimal refundAmount,
                                     String reason) {
        Transaction transaction = transactionRepositoryPort.findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transacción no encontrada con ID: " + transactionId));

        TransactionStatus oldStatus = transaction.getStatus();

        // La regla de negocio vive en el dominio
        transaction.refundPartial(refundAmount);

        Transaction saved = transactionRepositoryPort.save(transaction);

        auditPublisherPort.publishStatusChange(
                saved.getId(), oldStatus, saved.getStatus());

        merchantNotifierPort.notifyRefund(saved, refundAmount, reason);

        return saved;
    }

    public TransactionVolumeReport getTransactionVolumeReport(
            String merchantId,
            LocalDate from,
            LocalDate to,
            String groupByValue
    ) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalStateException("El token no tiene un comercio asociado");
        }

        if (from == null || to == null) {
            throw new IllegalArgumentException("Las fechas from y to son obligatorias");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }

        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days > 366) {
            throw new IllegalArgumentException("El rango de fechas no puede superar 366 días");
        }

        TransactionVolumeGroupBy groupBy = TransactionVolumeGroupBy.from(groupByValue);

        LocalDateTime fromInclusive = from.atStartOfDay();
        LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();

        List<TransactionVolumeReportItem> items = switch (groupBy) {
            case DAY -> transactionRepositoryPort.summarizeTransactionVolumeByDay(
                    merchantId,
                    fromInclusive,
                    toExclusive
            );
            case MONTH -> transactionRepositoryPort.summarizeTransactionVolumeByMonth(
                    merchantId,
                    fromInclusive,
                    toExclusive
            );
        };

        return new TransactionVolumeReport(from, to, groupBy, items);
    }
}
