package com.codefactory.appstripe.transactions.infrastructure.adapter;

import com.codefactory.appstripe.transactions.application.port.ITransactionRepositoryPort;
import com.codefactory.appstripe.transactions.application.query.TransactionStatusCount;
import com.codefactory.appstripe.transactions.application.query.TransactionVolumeReportItem;
import com.codefactory.appstripe.transactions.domain.Transaction;
import com.codefactory.appstripe.transactions.domain.TransactionStatus;
import com.codefactory.appstripe.transactions.infrastructure.persistence.entity.TransactionJpaEntity;
import com.codefactory.appstripe.transactions.infrastructure.persistence.mapper.TransactionMapper;
import com.codefactory.appstripe.transactions.infrastructure.persistence.repository.ITransactionSpringRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Component
public class TransactionRepositoryAdapter implements ITransactionRepositoryPort {

    private final ITransactionSpringRepository springRepository;
    private final TransactionMapper mapper;

    public TransactionRepositoryAdapter(ITransactionSpringRepository springRepository, TransactionMapper mapper) {
        this.springRepository = springRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Transaction> findById(String id) {
        // Busca en la bd y si lo encuentra, lo traduce a la clase pura de Dominio
        return springRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Transaction save(Transaction transaction) {
        //
        TransactionJpaEntity entity = mapper.toEntity(transaction);

        //Guarda en base de datos
        TransactionJpaEntity savedEntity = springRepository.save(entity);

        // devuelve el Dominio puro
        return mapper.toDomain(savedEntity);
    }

    @Override
    public List<Transaction> findByMerchantId(String merchantId) {
        return springRepository.findByMerchantId(merchantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TransactionStatusCount> countByMerchantIdAndStatusInAndCreatedAtBetween(
            String merchantId,
            List<TransactionStatus> statuses,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    ) {
        return springRepository.countByStatusForDashboard(
                        merchantId,
                        statuses,
                        fromInclusive,
                        toExclusive
                )
                .stream()
                .map(row -> new TransactionStatusCount(row.getStatus(), row.getTotal()))
                .toList();
    }

    @Override
    public List<TransactionVolumeReportItem> summarizeTransactionVolumeByDay(
            String merchantId,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    ) {
        return springRepository.summarizeTransactionVolumeByDay(merchantId, fromInclusive, toExclusive)
                .stream()
                .map(this::toTransactionVolumeReportItem)
                .toList();
    }

    @Override
    public List<TransactionVolumeReportItem> summarizeTransactionVolumeByMonth(
            String merchantId,
            LocalDateTime fromInclusive,
            LocalDateTime toExclusive
    ) {
        return springRepository.summarizeTransactionVolumeByMonth(merchantId, fromInclusive, toExclusive)
                .stream()
                .map(this::toTransactionVolumeReportItem)
                .toList();
    }

    private TransactionVolumeReportItem toTransactionVolumeReportItem(
            ITransactionSpringRepository.TransactionVolumeProjection row
    ) {
        return new TransactionVolumeReportItem(
                row.getPeriod(),
                row.getTransactionCount().longValue(),
                row.getTotalAmount(),
                row.getApprovedCount().longValue(),
                row.getRejectedCount().longValue(),
                row.getFailedCount().longValue()
        );
    }

    @Override
    public List<Transaction> findAll() {
        return springRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}