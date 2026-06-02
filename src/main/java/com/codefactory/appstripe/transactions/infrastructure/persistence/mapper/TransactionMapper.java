package com.codefactory.appstripe.transactions.infrastructure.persistence.mapper;


import com.codefactory.appstripe.transactions.domain.Transaction;
import com.codefactory.appstripe.transactions.infrastructure.persistence.entity.TransactionJpaEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ObjectFactory;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionJpaEntity toEntity(Transaction domain);

    Transaction toDomain(TransactionJpaEntity entity);

    @AfterMapping
    default void defaultRefundedAmount(@MappingTarget TransactionJpaEntity entity) {
        if (entity.getRefundedAmount() == null) {
            entity.setRefundedAmount(BigDecimal.ZERO);
        }
    }

    // ESTA ES LA SOLUCIÓN MÁGICA:
    // Le decimos a MapStruct exactamente cómo construir el objeto de dominio
    // usando el constructor completo, evitando cualquier ambigüedad.
    @ObjectFactory
    default Transaction createTransaction(TransactionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Transaction(
                entity.getId(),
                entity.getMerchantId(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getRefundedAmount() // Asegúrate de pasar el monto reembolsado
        );
    }
}