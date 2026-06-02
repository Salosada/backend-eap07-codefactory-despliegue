package com.codefactory.appstripe.security.application;

import com.codefactory.appstripe.security.api.dto.AccountStatusResponse;
import com.codefactory.appstripe.security.api.dto.JwtResponse;
import com.codefactory.appstripe.security.application.port.IJwtProviderPort;
import com.codefactory.appstripe.security.application.port.IPasswordEncoderPort;
import com.codefactory.appstripe.security.application.port.IUserRepositoryPort;
import com.codefactory.appstripe.security.application.port.TwoFactorPort;
import com.codefactory.appstripe.security.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuthenticationService {

    private final IUserRepositoryPort userRepository;
    private final IPasswordEncoderPort passwordEncoder;
    private final IJwtProviderPort jwtProvider;
    private final TwoFactorPort twoFactorPort;

    public AuthenticationService(IUserRepositoryPort userRepository,
                                 IPasswordEncoderPort passwordEncoder,
                                 IJwtProviderPort jwtProvider,
                                 TwoFactorPort twoFactorPort) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.twoFactorPort = twoFactorPort;
    }

    public JwtResponse login(String email, String password, Integer totpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!user.isAccountActivated()) {
            throw new IllegalStateException("La cuenta no ha sido activada");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        if (user.isTwoFactorEnabled()) {
            if (totpCode == null || !twoFactorPort.verify(user.getTwoFactorSecret(), totpCode)) {
                throw new IllegalStateException("Código 2FA inválido o ausente");
            }
        }

        String token = jwtProvider.generateToken(user);
        return JwtResponse.builder()
                .token(token)
                .role(user.getRole())
                .merchantId(user.getMerchantId())
                .build();
    }

    public JwtResponse activateAccount(String invitationToken, String newPassword) {
        User user = userRepository.findByInvitationToken(invitationToken)
                .orElseThrow(() -> new IllegalStateException("Token de invitación inválido o expirado"));

        if (user.isAccountActivated()) {
            throw new IllegalStateException("La cuenta ya ha sido activada");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setAccountActivated(true);
        user.setInvitationToken(null);

        User savedUser = userRepository.save(user);

        String token = jwtProvider.generateToken(savedUser);
        return JwtResponse.builder()
                .token(token)
                .role(savedUser.getRole())
                .merchantId(savedUser.getMerchantId())
                .build();
    }

    public User createMerchantUser(String email, String merchantId) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("El email ya está en uso");
        }

        User user = User.builder()
                .email(email)
                .role("MERCHANT")
                .merchantId(merchantId)
                .invitationToken(UUID.randomUUID().toString())
                .accountActivated(false)
                .twoFactorEnabled(false)
                .build();

        return userRepository.save(user);
    }

    public List<AccountStatusResponse> listAccounts() {
        return userRepository.findAll().stream()
                .map(AccountStatusResponse::fromDomain)
                .toList();
    }
}
