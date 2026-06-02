package com.codefactory.appstripe.security.application.port;

import com.codefactory.appstripe.security.domain.User;

import java.util.List;
import java.util.Optional;

public interface IUserRepositoryPort {
    Optional<User> findByEmail(String email);
    Optional<User> findByInvitationToken(String token);
    boolean existsByEmail(String email);
    User save(User user);

    List<User> findAll();
}
