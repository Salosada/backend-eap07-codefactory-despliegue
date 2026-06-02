package com.codefactory.appstripe.identity.application.port;

import java.util.List;
import java.util.Optional;

import com.codefactory.appstripe.identity.domain.ApiCredential;


public interface IApiCredentialRepositoryPort {
    ApiCredential save(ApiCredential credential);
    List<ApiCredential> findByMerchantIdAndActiveTrue(String merchantId);
    long countByMerchantIdAndActiveTrue(String merchantId);
    Optional<ApiCredential> findByPublicId(String publicId);

    List<ApiCredential> findAll();
}