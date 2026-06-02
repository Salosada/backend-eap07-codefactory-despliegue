package com.codefactory.appstripe.security.infrastructure.adapter;

import com.codefactory.appstripe.security.application.port.IUserRepositoryPort;
import com.codefactory.appstripe.security.domain.User;
import com.codefactory.appstripe.security.infrastructure.persistence.UserJpaEntity;
import com.codefactory.appstripe.security.infrastructure.persistence.repository.IUserSpringRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements IUserRepositoryPort {

    private final IUserSpringRepository repository;

    public UserRepositoryAdapter(IUserSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findByInvitationToken(String token) {
        return repository.findByInvitationToken(token).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll().stream().map(this::toDomain).toList();
    }

    private UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setRole(user.getRole());
        entity.setMerchantId(user.getMerchantId());
        entity.setTwoFactorSecret(user.getTwoFactorSecret());
        entity.setTwoFactorEnabled(user.isTwoFactorEnabled());
        entity.setInvitationToken(user.getInvitationToken());
        entity.setAccountActivated(user.isAccountActivated());
        return entity;
    }

    private User toDomain(UserJpaEntity entity) {
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .role(entity.getRole())
                .merchantId(entity.getMerchantId())
                .twoFactorSecret(entity.getTwoFactorSecret())
                .twoFactorEnabled(entity.isTwoFactorEnabled())
                .invitationToken(entity.getInvitationToken())
                .accountActivated(entity.isAccountActivated())
                .build();
    }
}
