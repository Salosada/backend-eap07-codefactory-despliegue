package com.codefactory.appstripe.transactions.infrastructure.persistence.repository;

import com.codefactory.appstripe.transactions.domain.TransactionStatus;
import com.codefactory.appstripe.transactions.infrastructure.persistence.entity.TransactionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ITransactionSpringRepository extends JpaRepository<TransactionJpaEntity, String> {

    List<TransactionJpaEntity> findByMerchantId(String merchantId);

    @Query("""
            SELECT t.status AS status, COUNT(t.id) AS total
            FROM TransactionJpaEntity t
            WHERE t.merchantId = :merchantId
              AND t.status IN :statuses
              AND t.createdAt >= :fromInclusive
              AND t.createdAt < :toExclusive
            GROUP BY t.status
            """)
    List<StatusCountProjection> countByStatusForDashboard(
            @Param("merchantId") String merchantId,
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query(value = """
        SELECT
            DATE_FORMAT(t.created_at, '%Y-%m-%d') AS period,
            COUNT(*) AS transactionCount,
            COALESCE(SUM(t.amount - COALESCE(t.refunded_amount, 0)), 0) AS totalAmount,
            COALESCE(SUM(CASE WHEN t.status IN ('APPROVED', 'PARTIALLY_REFUNDED', 'REFUNDED') THEN 1 ELSE 0 END), 0) AS approvedCount,
            COALESCE(SUM(CASE WHEN t.status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejectedCount,
            COALESCE(SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedCount
        FROM transactions t
        WHERE t.merchant_id = :merchantId
          AND t.created_at >= :fromInclusive
          AND t.created_at < :toExclusive
        GROUP BY DATE_FORMAT(t.created_at, '%Y-%m-%d')
        ORDER BY DATE_FORMAT(t.created_at, '%Y-%m-%d')
        """, nativeQuery = true)
    List<TransactionVolumeProjection> summarizeTransactionVolumeByDay(
            @Param("merchantId") String merchantId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query(value = """
        SELECT
            DATE_FORMAT(t.created_at, '%Y-%m') AS period,
            COUNT(*) AS transactionCount,
            COALESCE(SUM(t.amount - COALESCE(t.refunded_amount, 0)), 0) AS totalAmount,
            COALESCE(SUM(CASE WHEN t.status IN ('APPROVED', 'PARTIALLY_REFUNDED', 'REFUNDED') THEN 1 ELSE 0 END), 0) AS approvedCount,
            COALESCE(SUM(CASE WHEN t.status = 'REJECTED' THEN 1 ELSE 0 END), 0) AS rejectedCount,
            COALESCE(SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failedCount
        FROM transactions t
        WHERE t.merchant_id = :merchantId
          AND t.created_at >= :fromInclusive
          AND t.created_at < :toExclusive
        GROUP BY DATE_FORMAT(t.created_at, '%Y-%m')
        ORDER BY DATE_FORMAT(t.created_at, '%Y-%m')
        """, nativeQuery = true)
    List<TransactionVolumeProjection> summarizeTransactionVolumeByMonth(
            @Param("merchantId") String merchantId,
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive
    );

    interface TransactionVolumeProjection {
        String getPeriod();
        Number getTransactionCount();
        BigDecimal getTotalAmount();
        Number getApprovedCount();
        Number getRejectedCount();
        Number getFailedCount();
    }

    interface StatusCountProjection {
        TransactionStatus getStatus();
        Long getTotal();
    }



}
