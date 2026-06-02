package com.codefactory.appstripe.identity.application;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codefactory.appstripe.identity.application.port.ICommerceRepositoryPort;
import com.codefactory.appstripe.identity.domain.ApiCredentialPermission;
import com.codefactory.appstripe.identity.domain.Merchant;
import com.codefactory.appstripe.identity.domain.MerchantStatus;
import com.codefactory.appstripe.security.application.AuthenticationService;
import com.codefactory.appstripe.security.domain.User;

@ExtendWith(MockitoExtension.class)
class CommerceApplicationServiceTest {

    @Mock
    private ICommerceRepositoryPort commerceRepository;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private CommerceApplicationService commerceApplicationService;
    

    @Test
    @DisplayName("Debe registrar comercio VERIFIED cuando datos son válidos")
    void shouldRegisterMerchantSuccessfully() {
        when(commerceRepository.existsByBusinessId("900123456")).thenReturn(false);
        when(commerceRepository.existsByEmail("ops@merchant.com")).thenReturn(false);
        when(commerceRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authenticationService.createMerchantUser(anyString(), anyString()))
                .thenReturn(User.builder().invitationToken("invite-token-123").build());

        MerchantRegistrationResult result = commerceApplicationService.registerMerchant(
                "Tienda Demo",
                "900123456",
                "ops@merchant.com",
                "Retail");

        assertEquals("Tienda Demo", result.getMerchant().getBusinessName());
        assertEquals(MerchantStatus.VERIFIED, result.getMerchant().getStatus());
        assertEquals("invite-token-123", result.getInvitationToken());
    }

    @Test
    @DisplayName("Debe rechazar registro si email ya existe")
    void shouldFailWhenEmailExists() {
        when(commerceRepository.existsByBusinessId("900123456")).thenReturn(false);
        when(commerceRepository.existsByEmail("ops@merchant.com")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> commerceApplicationService.registerMerchant(
                "Tienda Demo",
                "900123456",
                "ops@merchant.com",
                "Retail"));
    }
    @Test
    @DisplayName("Debe consultar perfil de comercio existente")
    void shouldGetMerchantProfile() {
        Merchant merchant = Merchant.builder()
                .id("mch_123")
                .businessName("Tienda Demo")
                .businessId("900123456")
                .email("ops@merchant.com")
                .businessType("Retail")
                .status(MerchantStatus.VERIFIED)
                .build();

        when(commerceRepository.findById("mch_123")).thenReturn(java.util.Optional.of(merchant));

        Merchant result = commerceApplicationService.getMerchantProfile("mch_123");

        assertEquals("mch_123", result.getId());
        assertEquals("Tienda Demo", result.getBusinessName());
        assertEquals("ops@merchant.com", result.getEmail());
    }

    @Test
    @DisplayName("Debe fallar si el comercio no existe")
    void shouldFailWhenMerchantDoesNotExist() {
        when(commerceRepository.findById("mch_missing")).thenReturn(java.util.Optional.empty());

        assertThrows(java.util.NoSuchElementException.class,
                () -> commerceApplicationService.getMerchantProfile("mch_missing"));
    }

    @Test
    @DisplayName("Debe fallar si el merchantId viene vacío")
    void shouldFailWhenMerchantIdIsBlank() {
        assertThrows(IllegalStateException.class,
                () -> commerceApplicationService.getMerchantProfile(" "));
    }

    @Test
    @DisplayName("Debe actualizar perfil de comercio exitosamente")
    void shouldUpdateMerchantProfileSuccessfully() {
        // Arrange
        Merchant merchant = Merchant.builder()
                .id("mch_123")
                .businessName("Tienda Demo")
                .businessId("900123456")
                .email("ops@merchant.com")
                .businessType("Retail")
                .status(MerchantStatus.VERIFIED)
                .permission(ApiCredentialPermission.PAYMENTS)
                .build();

        when(commerceRepository.findById("mch_123")).thenReturn(Optional.of(merchant));
        when(commerceRepository.save(any(Merchant.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Merchant result = commerceApplicationService.updateMerchant(
                "mch_123",
                "Tienda Demo Actualizada",
                "newops@merchant.com",
                "Trade"
        );

        // Assert
        assertEquals("mch_123", result.getId());
        assertEquals("Tienda Demo Actualizada", result.getBusinessName());
        assertEquals("newops@merchant.com", result.getEmail());
        assertEquals("Trade", result.getBusinessType());
        // Campos que NO deben cambiar
        assertEquals("900123456", result.getBusinessId());
        assertEquals(MerchantStatus.VERIFIED, result.getStatus());
    }

    @Test
    @DisplayName("Debe fallar si el comercio no existe")
    void shouldFailWhenMerchantDoesNotExistOnUpdate() {
        // Arrange
        when(commerceRepository.findById("mch_missing"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(java.util.NoSuchElementException.class,
                () -> commerceApplicationService.updateMerchant(
                        "mch_missing",
                        "Tienda Demo",
                        "ops@merchant.com",
                        "Retail"
                ));
    }

    @Test
    @DisplayName("Debe fallar si el merchantId viene vacío")
    void shouldFailWhenMerchantIdIsBlankOnUpdate() {
        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> commerceApplicationService.updateMerchant(
                        " ",
                        "Tienda Demo",
                        "ops@merchant.com",
                        "Retail"
                ));
    }
}
