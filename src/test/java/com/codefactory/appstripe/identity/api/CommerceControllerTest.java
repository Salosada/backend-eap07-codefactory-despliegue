package com.codefactory.appstripe.identity.api;

import com.codefactory.appstripe.identity.application.CommerceApplicationService;
import com.codefactory.appstripe.identity.application.MerchantRegistrationResult;
import com.codefactory.appstripe.identity.domain.ApiCredentialPermission;
import com.codefactory.appstripe.identity.domain.Merchant;
import com.codefactory.appstripe.identity.domain.MerchantStatus;
import com.codefactory.appstripe.security.infrastructure.filter.CredentialValidationFilter;
import com.codefactory.appstripe.security.infrastructure.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CommerceController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CredentialValidationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CommerceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommerceApplicationService commerceApplicationService;

    @Test
    @DisplayName("CP-S1-001: registra comercio y retorna CREATED")
    void shouldRegisterMerchant() throws Exception {
        Merchant merchant = Merchant.builder()
                .id("mch_123")
                .businessName("Mi Tienda SAS")
                .businessId("9012345678")
                .email("admin@mitienda.com")
                .businessType("RETAIL")
                .status(MerchantStatus.VERIFIED)
                .permission(ApiCredentialPermission.PAYMENTS)
                .build();

        when(commerceApplicationService.registerMerchant(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new MerchantRegistrationResult(merchant, "invite-token-123"));

        mockMvc.perform(post("/api/v1/admin/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "Mi Tienda SAS",
                                  "businessId": "9012345678",
                                  "email": "admin@mitienda.com",
                                  "businessType": "RETAIL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("mch_123"))
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.invitationToken").value("invite-token-123"));
    }

    @Test
    @DisplayName("CP-S1-003: rechaza registro con email duplicado")
    void shouldRejectDuplicatedEmail() throws Exception {
        when(commerceApplicationService.registerMerchant(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("Ya existe un comercio con el correo electronico indicado"));

        mockMvc.perform(post("/api/v1/admin/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "Mi Tienda SAS",
                                  "businessId": "9012345678",
                                  "email": "admin@mitienda.com",
                                  "businessType": "RETAIL"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    @DisplayName("CP-S1-002: retorna error estandarizado ante falla interna")
    void shouldReturnInternalErrorWhenUnexpectedFailureOccurs() throws Exception {
        when(commerceApplicationService.registerMerchant(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Falla interna simulada"));

        mockMvc.perform(post("/api/v1/admin/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "Mi Tienda SAS",
                                  "businessId": "9012345678",
                                  "email": "admin@mitienda.com",
                                  "businessType": "RETAIL"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("CP-S1-004: rechaza registro con campos obligatorios vacios")
    void shouldRejectBlankRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "businessName": "",
                                  "businessId": "",
                                  "email": "",
                                  "businessType": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
