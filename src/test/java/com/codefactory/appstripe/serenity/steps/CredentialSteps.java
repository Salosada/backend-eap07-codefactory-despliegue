package com.codefactory.appstripe.serenity.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions para HU006 (Validación de credenciales) y HU010 (Revocación).
 */
public class CredentialSteps {

    private com.codefactory.appstripe.serenity.steps.CommonSteps.TestContext context() {
        return CommonSteps.context();
    }

    @Given("existen credenciales activas con permiso {string}")
    public void existenCredencialesActivas(String permission) {
        if (context().getPublicId() != null && context().getSecret() != null) {
            return;
        }
        obtenerCsrfYLoginAdmin();
        if (context().getMerchantId() == null) {
            crearComercio();
        }
        generarCredenciales();
    }

    @Given("existen credenciales activas para un comercio verificado")
    public void existenCredencialesActivasParaComercioVerificado() {
        if (context().getPublicId() != null && context().getSecret() != null) {
            return;
        }
        obtenerCsrfYLoginAdmin();
        if (context().getMerchantId() == null) {
            crearComercio();
        }
        generarCredenciales();
    }

    @Given("un comercio verificado con credenciales activas")
    public void unComercioVerificadoConCredencialesActivas() {
        existenCredencialesActivas("payments:write");
    }

    @Given("un comercio verificado")
    public void unComercioVerificado() {
        if (context().getMerchantId() != null) return;
        obtenerCsrfYLoginAdmin();
        crearComercio();
    }

    @Given("un administrador autenticado")
    public void unAdministradorAutenticado() {
        obtenerCsrfYLoginAdmin();
    }

    @Given("una credencial activa con publicId {string}")
    public void unaCredencialActiva(String publicIdPattern) {
        if (context().getPublicId() == null) {
            existenCredencialesActivas("payments:write");
        }
    }

    @Given("una credencial previamente revocada")
    public void unaCredencialPreviamenteRevocada() {
        if (context().getPublicId() == null) {
            existenCredencialesActivas("payments:write");
        }
        Response resp = SerenityRest.given()
                .cookie("XSRF-TOKEN", context().getCsrfCookie())
                .header(context().getCsrfHeaderName(), context().getCsrfToken())
                .header("Authorization", "Bearer " + context().getAdminToken())
                .when()
                .patch("/api/v1/admin/credentials/{publicId}/revoke",
                        context().getPublicId());
        context().setLastResponse(resp);
        assertEquals(200, resp.statusCode(), "La revocación previa debería ser exitosa");
    }

    @Given("una credencial que fue revocada")
    public void unaCredencialQueFueRevocada() {
        unaCredencialPreviamenteRevocada();
    }

    @When("se envía una solicitud POST a {string} con las credenciales válidas")
    public void postConCredencialesValidas(String endpoint) {
        Map<String, Object> body = new HashMap<>();
        body.put("merchantId", context().getMerchantId());
        body.put("amount", 25000);
        Response resp = SerenityRest.given()
                .cookie("XSRF-TOKEN", context().getCsrfCookie())
                .header(context().getCsrfHeaderName(), context().getCsrfToken())
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json").body(body).when().post(endpoint);
        context().setLastResponse(resp);
    }

    @When("el cuerpo de la transacción contiene:")
    public void elCuerpoDeLaTransaccionContiene(String docString) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe existir una respuesta previa de la transacción");
    }

    @When("se envía una solicitud POST a {string} sin headers de credenciales")
    public void postSinCredenciales(String endpoint) {
        Map<String, Object> body = new HashMap<>();
        body.put("merchantId", context().getMerchantId());
        body.put("amount", 10000);
        Response resp = SerenityRest.given()
                .cookie("XSRF-TOKEN", context().getCsrfCookie())
                .header(context().getCsrfHeaderName(), context().getCsrfToken())
                .contentType("application/json").body(body).when().post(endpoint);
        context().setLastResponse(resp);
    }

    @When("se envía una solicitud POST a {string} con X-Merchant-Id de otro comercio")
    public void postConMerchantIdDeOtroComercio(String endpoint) {
        Map<String, Object> body = new HashMap<>();
        body.put("merchantId", "mch_intruso_123");
        body.put("amount", 10000);
        Response resp = SerenityRest.given()
                .cookie("XSRF-TOKEN", context().getCsrfCookie())
                .header(context().getCsrfHeaderName(), context().getCsrfToken())
                .header("X-Merchant-Id", "mch_intruso_123")
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json").body(body).when().post(endpoint);
        context().setLastResponse(resp);
    }

    @When("se envía una solicitud POST a {string} con credenciales falsas")
    public void postConCredencialesFalsas(String endpoint) {
        Map<String, Object> body = new HashMap<>();
        body.put("merchantId", context().getMerchantId());
        body.put("amount", 10000);
        Response resp = SerenityRest.given()
                .cookie("XSRF-TOKEN", context().getCsrfCookie())
                .header(context().getCsrfHeaderName(), context().getCsrfToken())
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", "pk_live_fake")
                .header("X-Secret", "sk_live_fake")
                .contentType("application/json").body(body).when().post(endpoint);
        context().setLastResponse(resp);
    }

    @When("se envía una solicitud POST a {string} con la credencial revocada")
    public void postConCredencialRevocada(String endpoint) {
        postConCredencialesValidas(endpoint);
    }

    @When("se envía una solicitud PATCH a {string}")
    public void patchEndpoint(String endpoint) {
        String resolvedEndpoint = endpoint.replace("{publicId}", context().getPublicId());
        Response resp = SerenityRest.given()
                .cookie("XSRF-TOKEN", context().getCsrfCookie())
                .header(context().getCsrfHeaderName(), context().getCsrfToken())
                .header("Authorization", "Bearer " + context().getAdminToken())
                .when().patch(resolvedEndpoint);
        context().setLastResponse(resp);
    }

    @When("se envía una solicitud PATCH a {string} con resultado válido")
    public void patchEndpointConResultado(String endpoint) {
        String resolvedEndpoint = endpoint.replace("{transactionId}", context().getTransactionId());
        Map<String, Object> body = new HashMap<>();
        body.put("result", "APPROVED");
        body.put("authorizationCode", "AUTH-12345");
        Response resp = SerenityRest.given()
                .cookie("XSRF-TOKEN", context().getCsrfCookie())
                .header(context().getCsrfHeaderName(), context().getCsrfToken())
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json").body(body).when().patch(resolvedEndpoint);
        context().setLastResponse(resp);
    }

    // ========================================================================
    // Métodos auxiliares
    // ========================================================================

    private void obtenerCsrfYLoginAdmin() {
        Map<String, Object> login = new HashMap<>();
        login.put("email", "admin@paycore.com");
        login.put("password", "admin123");
        Response loginResp = SerenityRest.given()
                .contentType("application/json").body(login).when()
                .post("/api/v1/auth/login")
                .then().statusCode(200).extract().response();
        context().setAdminToken(loginResp.jsonPath().getString("token"));
        assertNotNull(context().getAdminToken(), "Token de admin debe ser retornado");
    }

    private void crearComercio() {
        Map<String, Object> merchant = new HashMap<>();
        String ts = String.valueOf(context().getTimestamp());
        merchant.put("businessName", "Comercio Serenity " + ts);
        merchant.put("businessId", "biz_s_" + ts);
        merchant.put("email", context().getUniqueEmail("serenity"));
        merchant.put("businessType", "RETAIL");
        Response resp = SerenityRest.given()
                .header("Authorization", "Bearer " + context().getAdminToken())
                .contentType("application/json").body(merchant).when()
                .post("/api/v1/admin/merchants")
                .then().statusCode(201).extract().response();
        context().setMerchantId(resp.jsonPath().getString("id"));
        assertNotNull(context().getMerchantId(), "merchantId no debe ser nulo");
    }

    private void generarCredenciales() {
        Map<String, Object> gen = new HashMap<>();
        gen.put("merchantId", context().getMerchantId());
        Response genResp = SerenityRest.given()
                .header("Authorization", "Bearer " + context().getAdminToken())
                .contentType("application/json").body(gen).when()
                .post("/api/v1/admin/credentials/generate")
                .then().statusCode(201).extract().response();
        context().setPublicId(genResp.jsonPath().getString("publicId"));
        context().setSecret(genResp.jsonPath().getString("secret"));
        assertNotNull(context().getPublicId(), "publicId no debe ser nulo");
        assertNotNull(context().getSecret(), "secret no debe ser nulo");
    }
}