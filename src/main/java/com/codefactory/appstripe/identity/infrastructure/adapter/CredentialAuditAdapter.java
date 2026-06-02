package com.codefactory.appstripe.identity.infrastructure.adapter;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.codefactory.appstripe.identity.application.port.ICredentialAuditPort;

@Component
public class CredentialAuditAdapter implements ICredentialAuditPort {

    private static final Logger log = LoggerFactory.getLogger(CredentialAuditAdapter.class);

    @Override
    public void publishCredentialRevoked(String publicId, String actorMerchantId, Instant revokedAt) {
        String safePublicId = publicId != null ? publicId.replaceAll("[\n\r]", "_") : null;
        String safeActorMerchantId = actorMerchantId != null ? actorMerchantId.replaceAll("[\n\r]", "_") : null;
        log.info(
            "[AUDIT] CREDENTIAL_REVOKED | publicId={} | actorMerchantId={} | revokedAt={}",
            safePublicId, safeActorMerchantId, revokedAt
        );
    }
}