package com.codefactory.appstripe.identity.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.codefactory.appstripe.identity.application.port.ICommerceRepositoryPort;
import com.codefactory.appstripe.identity.domain.ApiCredentialPermission;
import com.codefactory.appstripe.identity.domain.Merchant;
import com.codefactory.appstripe.identity.domain.MerchantStatus;
import com.codefactory.appstripe.security.application.AuthenticationService;
import com.codefactory.appstripe.security.domain.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommerceApplicationService {

    private final ICommerceRepositoryPort commerceRepository;
    private final AuthenticationService authenticationService;

    public MerchantRegistrationResult registerMerchant(String businessName, String businessId, String email, String businessType) {
        if (commerceRepository.existsByBusinessId(businessId)) {
            throw new IllegalStateException("Ya existe un comercio con el número de identificación fiscal indicado");
        }
        if (commerceRepository.existsByEmail(email)) {
            throw new IllegalStateException("Ya existe un comercio con el correo electrónico indicado");
        }

        Merchant merchant = Merchant.builder()
                .id("mch_" + UUID.randomUUID().toString().replace("-", ""))
                .businessName(businessName)
                .businessId(businessId)
                .email(email)
                .businessType(businessType)
                .status(MerchantStatus.VERIFIED)
                .permission(ApiCredentialPermission.PAYMENTS)
                .build();

        Merchant savedMerchant = commerceRepository.save(merchant);

        User user = authenticationService.createMerchantUser(email, savedMerchant.getId());

        return new MerchantRegistrationResult(savedMerchant, user.getInvitationToken());
    }

    public List<Merchant> listMerchants() {
        return commerceRepository.findAll();
    }

    // Método que permite optener un Merchant a partir de su ID, se utiliza para obtener el perfil del comercio a partir del token JWT
    public Merchant getMerchantProfile(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new IllegalStateException("El token no tiene un comercio asociado");
        }

        return commerceRepository.findById(merchantId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Comercio no encontrado"));
    }

    // Método que permite actualizar el perfil de un comercio, se utiliza para actualizar el perfil del comercio a partir del token JWT
    public Merchant updateMerchant(String merchantId, String newName, String newEmail, String newType) {

        Merchant oldMerchant = getMerchantProfile(merchantId);

        String businessName = newName != null ? newName : oldMerchant.getBusinessName();
        String email = newEmail != null ? newEmail : oldMerchant.getEmail();
        String businessType = newType != null ? newType : oldMerchant.getBusinessType();

        Merchant updated = Merchant.builder()
                .id(oldMerchant.getId())
                .businessId(oldMerchant.getBusinessId())
                .status(oldMerchant.getStatus())
                .permission(oldMerchant.getPermission())
            .businessName(businessName)
            .email(email)
            .businessType(businessType)
                .build();

        return commerceRepository.save(updated);
    }
}
