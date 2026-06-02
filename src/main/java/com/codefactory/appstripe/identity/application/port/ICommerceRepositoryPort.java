package com.codefactory.appstripe.identity.application.port;

import com.codefactory.appstripe.identity.domain.Merchant;

import java.util.List;
import java.util.Optional;

public interface ICommerceRepositoryPort {
    // Para la HU2 necesitamos encontrarlo por su ID interno
    Optional<Merchant> findById(String id);

    // Otros métodos que pide tu documentación para la HU1
    Merchant save(Merchant merchant);

    boolean existsByBusinessId(String businessId);

    boolean existsByEmail(String email);

    List<Merchant> findAll();
}