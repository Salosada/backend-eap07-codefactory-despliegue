package com.codefactory.appstripe.transactions.application.port;

import com.codefactory.appstripe.transactions.application.query.TransactionStatusCount;
import com.codefactory.appstripe.transactions.application.query.TransactionVolumeReportItem;
import com.codefactory.appstripe.transactions.domain.Transaction;
import com.codefactory.appstripe.transactions.domain.TransactionStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


// guardar y buscar una transaccion
public interface ITransactionRepositoryPort {

    // Busca una transacción por su ID.
    Optional<Transaction> findById(String id);

    // Guarda o actualiza la transacción en la base de datos
    Transaction save(Transaction transaction);

    // Busca transacciones por merchantId
    List<Transaction> findByMerchantId(String merchantId);

    List<TransactionStatusCount> countByMerchantIdAndStatusInAndCreatedAtBetween(
            String merchantId,
            List<TransactionStatus> statuses,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    );

    List<TransactionVolumeReportItem> summarizeTransactionVolumeByDay(
            String merchantId,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    );

    List<TransactionVolumeReportItem> summarizeTransactionVolumeByMonth(
            String merchantId,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    );

    List<Transaction> findAll();

}
