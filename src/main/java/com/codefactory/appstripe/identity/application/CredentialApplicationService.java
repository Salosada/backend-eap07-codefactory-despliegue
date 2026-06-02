package com.codefactory.appstripe.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codefactory.appstripe.identity.application.port.IApiCredentialRepositoryPort;
import com.codefactory.appstripe.identity.application.port.IApiKeyGeneratorPort;
import com.codefactory.appstripe.identity.application.port.ICommerceRepositoryPort;
import com.codefactory.appstripe.identity.application.port.ICredentialAuditPort;
import com.codefactory.appstripe.identity.domain.ApiCredential;
import com.codefactory.appstripe.identity.domain.ApiCredentialPermission;
import com.codefactory.appstripe.identity.domain.Merchant;
import com.codefactory.appstripe.identity.domain.MerchantStatus;
import com.codefactory.appstripe.identity.domain.exception.CredentialAccessDeniedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CredentialApplicationService {

    private final IApiCredentialRepositoryPort credentialRepository;
    private final ICommerceRepositoryPort commerceRepository;
    private final IApiKeyGeneratorPort keyGenerator;
    private final ICredentialAuditPort credentialAudit;

    @Transactional
    public ApiCredential generateCredentials(String merchantId) {
        Merchant merchant = commerceRepository.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Comercio no encontrado"));

        if (merchant.getStatus() != MerchantStatus.VERIFIED) {
            throw new IllegalStateException(
                "No es posible generar credenciales hasta que el comercio esté activo y verificado");
        }

        long activeCount = credentialRepository.countByMerchantIdAndActiveTrue(merchantId);
        if (activeCount >= 3) {
            throw new IllegalStateException("Se ha alcanzado el límite permitido de credenciales activas");
        }

        String plainSecret = keyGenerator.generateSecretKey();
        String publicId    = keyGenerator.generatePublicId();

        ApiCredential newCredential = ApiCredential.builder()
                .merchantId(merchantId)
                .publicId(publicId)
                .secretHash(keyGenerator.hashSecret(plainSecret))
                .active(true)
                .permission(ApiCredentialPermission.PAYMENTS)
                .build();

        credentialRepository.save(newCredential);

        return ApiCredential.builder()
                .publicId(publicId)
                .plainSecret(plainSecret)
                .build();
    }

    /**
     * Revoca una credencial activa validando que el solicitante sea el propietario.
     * Escenario 1: revocación exitosa + auditoría.
     * Escenario 3: lanza excepción si ya estaba revocada (idempotencia).
     * Escenario 4: lanza excepción si la credencial es de otro comercio (aislamiento).
     */
    @Transactional
    public ApiCredential revokeCredential(String publicId, String requestingMerchantId) {
        ApiCredential credential = credentialRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NoSuchElementException("Credencial no encontrada: " + publicId));

        // Escenario 4: aislamiento
        if (!credential.getMerchantId().equals(requestingMerchantId)) {
            throw new CredentialAccessDeniedException(
                "No tienes permisos para revocar una credencial que pertenece a otro comercio");
        }

        // Escenario 3: idempotencia
        if (!credential.isActive()) {
            throw new IllegalStateException(
                "La credencial ya fue revocada con anterioridad y no requiere ninguna acción adicional");
        }

        // Escenario 1: revocar, persistir y auditar
        credential.revoke();
        ApiCredential saved = credentialRepository.save(credential);
        credentialAudit.publishCredentialRevoked(publicId, requestingMerchantId, Instant.now());

        return saved;
    }

    /**
     * Variante para administradores de plataforma (ROLE_ADMIN) que no tienen
     * merchantId en el token. El admin puede revocar cualquier credencial.
     */
    @Transactional
    public ApiCredential revokeCredentialAsAdmin(String publicId) {
        ApiCredential credential = credentialRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NoSuchElementException("Credencial no encontrada: " + publicId));

        if (!credential.isActive()) {
            throw new IllegalStateException(
                "La credencial ya fue revocada con anterioridad y no requiere ninguna acción adicional");
        }

        credential.revoke();
        ApiCredential saved = credentialRepository.save(credential);
        credentialAudit.publishCredentialRevoked(publicId, "ADMIN", Instant.now());

        return saved;
    }

    public List<ApiCredential> listAllCredentials() {
        return credentialRepository.findAll();
    }
}
