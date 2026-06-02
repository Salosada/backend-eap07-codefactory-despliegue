package com.codefactory.appstripe.serenity.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;

import java.util.HashMap;
import java.util.Map;

/**
 * Step definitions para HU-07 (Consulta de perfil) y HU-08 (Actualización de perfil).
 */
public class MerchantSteps {

    private com.codefactory.appstripe.serenity.steps.CommonSteps.TestContext context() {
        return CommonSteps.context();
    }

    @Given("un comercio autenticado con credenciales activas")
    public void unComercioAutenticadoConCredenciales() {
        // Reutiliza el setup de credenciales activas
        if (context().getPublicId() == null) {
            // Obtener CSRF + login admin + crear comercio + generar credenciales
            obtenerCsrfYLoginAdmin();
            if (context().getMerchantId() == null) {
                crearComercio();
            }
            generarCredenciales();
        }
    }

    @Given("un comercio no autenticado")
    public void unComercioNoAutenticado() {
        // No se configura nada, se envía la petición sin auth
    }

    @When("se envía una solicitud GET a {string} sin autenticación")
    public void getEndpointSinAuth(String endpoint) {
        Response resp = SerenityRest.given()
                .when()
                .get(endpoint);

        context().setLastResponse(resp);
    }

    /**
     * Método interno para GET autenticado sin anotación @When.
     * La definición canónica del step "se envía una solicitud GET a"
     * está en TransactionSteps.getEndpoint(), que incluye resolución de placeholders.
     */
    private void getEndpointAuth(String endpoint) {
        Response resp = SerenityRest.given()
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .when()
                .get(endpoint);

        context().setLastResponse(resp);
    }

    /**
     * Método interno para PATCH autenticado sin anotación @When.
     * La definición canónica del step "se envía una solicitud PATCH a {string} con:"
     * está en TransactionSteps.patchConBody(), que incluye resolución de placeholders.
     */
    private void patchEndpointAuth(String endpoint, String docString) {
        Response resp = SerenityRest.given()
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json")
                .body(docString)
                .when()
                .patch(endpoint);

        context().setLastResponse(resp);
    }

    @When("se envía una solicitud PATCH a {string} con email diferente")
    public void patchConEmailDiferente(String endpoint) {
        String body = """
                {
                    "email": "otro-email@cambiado.com"
                }
                """;

        Response resp = SerenityRest.given()
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json")
                .body(body)
                .when()
                .patch(endpoint);

        context().setLastResponse(resp);
    }

    // ========================================================================
    // Métodos auxiliares (igual que en CredentialSteps para mantener independencia)
    // ========================================================================

    private void obtenerCsrfYLoginAdmin() {
        if (context().getAdminToken() != null) return;

        Map<String, Object> login = new HashMap<>();
        login.put("email", "admin@paycore.com");
        login.put("password", "admin123");

        Response loginResp = SerenityRest.given()
                .contentType("application/json")
                .body(login)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        context().setAdminToken(loginResp.jsonPath().getString("token"));
    }

    private void crearComercio() {
        Map<String, Object> merchant = new HashMap<>();
        String ts = String.valueOf(context().getTimestamp());
        merchant.put("businessName", "Merchant Perfil " + ts);
        merchant.put("businessId", "biz_mp_" + ts);
        merchant.put("email", context().getUniqueEmail("perfil"));
        merchant.put("businessType", "RETAIL");

        Response resp = SerenityRest.given()
                .header("Authorization", "Bearer " + context().getAdminToken())
                .contentType("application/json")
                .body(merchant)
                .when()
                .post("/api/v1/admin/merchants")
                .then()
                .statusCode(201)
                .extract().response();

        context().setMerchantId(resp.jsonPath().getString("id"));
    }

    private void generarCredenciales() {
        Map<String, Object> gen = new HashMap<>();
        gen.put("merchantId", context().getMerchantId());

        Response genResp = SerenityRest.given()
                .header("Authorization", "Bearer " + context().getAdminToken())
                .contentType("application/json")
                .body(gen)
                .when()
                .post("/api/v1/admin/credentials/generate")
                .then()
                .statusCode(201)
                .extract().response();

        context().setPublicId(genResp.jsonPath().getString("publicId"));
        context().setSecret(genResp.jsonPath().getString("secret"));
    }
}
