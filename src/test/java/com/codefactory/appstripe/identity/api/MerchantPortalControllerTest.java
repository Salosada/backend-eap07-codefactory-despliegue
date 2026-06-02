package com.codefactory.appstripe.identity.api;

import com.codefactory.appstripe.identity.api.dto.CredentialItemResponse;
import com.codefactory.appstripe.identity.api.dto.MerchantResponse;
import com.codefactory.appstripe.identity.api.dto.UpdateMerchantProfileRequest;
import com.codefactory.appstripe.identity.application.CommerceApplicationService;
import com.codefactory.appstripe.transactions.application.TransactionApplicationService;
import com.codefactory.appstripe.identity.application.port.IApiCredentialRepositoryPort;
import com.codefactory.appstripe.identity.domain.ApiCredential;
import com.codefactory.appstripe.identity.domain.ApiCredentialPermission;
import com.codefactory.appstripe.identity.domain.Merchant;
import com.codefactory.appstripe.identity.domain.MerchantStatus;
import com.codefactory.appstripe.transactions.api.dto.TransactionResponse;
import com.codefactory.appstripe.transactions.application.port.ITransactionRepositoryPort;
import com.codefactory.appstripe.transactions.domain.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MerchantPortalControllerTest {

    private final CommerceApplicationService commerceApplicationService = mock(CommerceApplicationService.class);
    private final IApiCredentialRepositoryPort credentialRepository = mock(IApiCredentialRepositoryPort.class);
    private final ITransactionRepositoryPort transactionRepository = mock(ITransactionRepositoryPort.class);
    private final TransactionApplicationService transactionApplicationService = mock(TransactionApplicationService.class);

    private final MerchantPortalController controller = new MerchantPortalController(
            commerceApplicationService,
            credentialRepository,
            transactionRepository,
            transactionApplicationService
    );

    @Test
    @DisplayName("CP-S2-004: consulta perfil del comercio autenticado")
    void shouldGetOwnMerchantProfile() {
        Merchant merchant = Merchant.builder()
                .id("mch_123")
                .businessName("Mi Tienda SAS")
                .businessId("9012345678")
                .email("admin@mitienda.com")
                .businessType("RETAIL")
                .status(MerchantStatus.VERIFIED)
                .permission(ApiCredentialPermission.PAYMENTS)
                .build();

        when(commerceApplicationService.getMerchantProfile("mch_123")).thenReturn(merchant);

        ResponseEntity<MerchantResponse> response = controller.getProfile(authForMerchant("mch_123"));

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("mch_123", response.getBody().getId());
        assertEquals("Mi Tienda SAS", response.getBody().getBusinessName());
    }

    @Test
    @DisplayName("HU008: actualiza perfil editable del comercio autenticado")
    void shouldUpdateOwnMerchantProfile() {
        UpdateMerchantProfileRequest request = new UpdateMerchantProfileRequest();
        request.setBusinessName("Mi Tienda Actualizada");
        request.setEmail("ops@mitienda.com");
        request.setBusinessType("ECOMMERCE");

        Merchant updated = Merchant.builder()
                .id("mch_123")
                .businessName("Mi Tienda Actualizada")
                .businessId("9012345678")
                .email("ops@mitienda.com")
                .businessType("ECOMMERCE")
                .status(MerchantStatus.VERIFIED)
                .permission(ApiCredentialPermission.PAYMENTS)
                .build();

        when(commerceApplicationService.updateMerchant("mch_123", "Mi Tienda Actualizada", "ops@mitienda.com", "ECOMMERCE"))
                .thenReturn(updated);

        ResponseEntity<MerchantResponse> response = controller.putProfile(authForMerchant("mch_123"), request);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Mi Tienda Actualizada", response.getBody().getBusinessName());
        assertEquals("9012345678", response.getBody().getBusinessId());
    }

    @Test
    @DisplayName("HU007: rechaza operaciones del portal si el token no contiene comercio")
    void shouldRejectPortalAccessWhenMerchantIdIsMissing() {
        assertThrows(IllegalStateException.class, () -> controller.getProfile(null));
    }

    @Test
    @DisplayName("HU015 parcial: lista solo transacciones del comercio autenticado segun repositorio")
    void shouldListTransactionsForAuthenticatedMerchant() {
        List<Transaction> transactions = List.of(new Transaction("trx_1", "mch_123", new BigDecimal("150.00")));
        when(transactionRepository.findByMerchantId("mch_123")).thenReturn(transactions);

        ResponseEntity<List<TransactionResponse>> response = controller.getTransactions(authForMerchant("mch_123"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("mch_123", response.getBody().get(0).getMerchantId());
        assertEquals("CREATED", response.getBody().get(0).getStatus());
    }

    @Test
    @DisplayName("HU010 parcial: lista credenciales activas del comercio autenticado")
    void shouldListActiveCredentialsForAuthenticatedMerchant() {
        List<ApiCredential> credentials = List.of(ApiCredential.builder()
                .publicId("pk_live_123")
                .merchantId("mch_123")
                .active(true)
                .build());
        when(credentialRepository.findByMerchantIdAndActiveTrue("mch_123")).thenReturn(credentials);

        ResponseEntity<List<CredentialItemResponse>> response = controller.getCredentials(authForMerchant("mch_123"));

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("pk_live_123", response.getBody().get(0).getPublicId());
    }

    private TestingAuthenticationToken authForMerchant(String merchantId) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("merchant", merchantId);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
