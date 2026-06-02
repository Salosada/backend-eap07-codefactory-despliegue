package com.codefactory.appstripe.identity.infrastructure.adapter;

import com.codefactory.appstripe.identity.application.port.ICommerceRepositoryPort;
import com.codefactory.appstripe.identity.domain.Merchant;
import com.codefactory.appstripe.identity.infrastructure.persistence.entity.MerchantJpaEntity;
import com.codefactory.appstripe.identity.infrastructure.persistence.repository.ICommerceSpringRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CommerceRepositoryAdapter implements ICommerceRepositoryPort {

    private final ICommerceSpringRepository springRepository;

    public CommerceRepositoryAdapter(ICommerceSpringRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Optional<Merchant> findById(String id) {
        return springRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Merchant save(Merchant merchant) {
        MerchantJpaEntity saved = springRepository.save(toEntity(merchant));
        return toDomain(saved);
    }

    @Override
    public boolean existsByBusinessId(String businessId) {
        return springRepository.existsByBusinessId(businessId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springRepository.existsByEmail(email);
    }

    @Override
    public List<Merchant> findAll() {
        return springRepository.findAll().stream().map(this::toDomain).toList();
    }

    private MerchantJpaEntity toEntity(Merchant merchant) {
        MerchantJpaEntity entity = new MerchantJpaEntity();
        entity.setId(merchant.getId());
        entity.setBusinessName(merchant.getBusinessName());
        entity.setBusinessId(merchant.getBusinessId());
        entity.setEmail(merchant.getEmail());
        entity.setBusinessType(merchant.getBusinessType());
        entity.setPermission(merchant.getPermission());
        entity.setStatus(merchant.getStatus());
        return entity;
    }

    private Merchant toDomain(MerchantJpaEntity entity) {
        return Merchant.builder()
                .id(entity.getId())
                .businessName(entity.getBusinessName())
                .businessId(entity.getBusinessId())
                .email(entity.getEmail())
                .businessType(entity.getBusinessType())
                .status(entity.getStatus())
                .permission(entity.getPermission())
                .build();
    }
}
