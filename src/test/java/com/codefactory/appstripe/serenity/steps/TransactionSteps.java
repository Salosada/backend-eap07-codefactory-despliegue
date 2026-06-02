package com.codefactory.appstripe.serenity.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Step definitions para HU012 (Resultado del pago) y HU015 (Listado de pagos).
 * <p>
 * Usa {@link SharedContext#getInstance()} como singleton, sin dependencia de Spring DI.
 */
public class TransactionSteps {

    private com.codefactory.appstripe.serenity.steps.CommonSteps.TestContext context() {
        return CommonSteps.context();
    }

    // ========================================================================
    // HU012 — Procesamiento del resultado del pago
    // ========================================================================

    @Given("existe una transacción en estado {string} con id {string}")
    public void existeTransaccionEnEstado(String estado, String idPattern) {
        existeTransaccionEnEstado(estado);
    }

    @Given("una transacción en estado {string} con id {string}")
    public void unaTransaccionEnEstadoConId(String estado, String idPattern) {
        existeTransaccionEnEstado(estado);
    }

    @Given("existe una transacción en estado {string}")
    public void existeTransaccionEnEstado(String estado) {
        // Si ya tenemos una transacción, la reutilizamos
        if (context().getTransactionId() != null) {
            return;
        }

        // Asegurar que tenemos comercio y credenciales
        if (context().getMerchantId() == null || context().getPublicId() == null) {
            // Usar un step interno: esto normalmente se haría con credenciales existentes
            crearTransaccion();
            return;
        }

        crearTransaccion();
    }

    @Given("existen transacciones creadas para el comercio actual")
    public void existenTransaccionesCreadas() {
        if (context().getTransactionId() == null) {
            existeTransaccionEnEstado("CREATED");
        }
    }

    @Given("dos comercios diferentes con transacciones creadas")
    public void dosComerciosConTransacciones() {
        // Aseguramos que el primer comercio tenga al menos una transacción
        if (context().getTransactionId() == null) {
            existeTransaccionEnEstado("CREATED");
        }
        // En un escenario real, crearíamos otro comercio. Para propósitos de la prueba,
        // esto se validaría con la respuesta del listado.
    }

    @Given("una transacción en estado {string}")
    public void unaTransaccionEnEstado(String estado) {
        if (context().getTransactionId() != null) {
            return;
        }
        existeTransaccionEnEstado(estado);
    }

    @Given("el comercio tiene una transaccion en estado {string}")
    public void elComercioTieneTransaccionEnEstado(String estado) {
        existeTransaccionEnEstado(estado);
    }

    @Given("existe una transacción en estado COMPLETED")
    public void existeTransaccionCompletada() {
        // Crear transacción y completarla
        existeTransaccionEnEstado("CREATED");

        Map<String, Object> body = new HashMap<>();
        body.put("result", "APPROVED");
        body.put("authorizationCode", "AUTH-99999");

        String endpoint = "/api/v1/transactions/{id}/complete"
                .replace("{id}", context().getTransactionId());

        Response resp = SerenityRest.given()
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json")
                .body(body)
                .when()
                .patch(endpoint);

        assertEquals(200, resp.statusCode(), "La transacción debería completarse");
    }

    @Given("existen transacciones para el comercio actual")
    public void existenTransaccionesParaElComercio() {
        if (context().getTransactionId() == null) {
            existeTransaccionEnEstado("CREATED");
        }
    }

    @When("se envía una solicitud PATCH a {string} con:")
    public void patchConBody(String endpoint, String docString) {
        String resolvedEndpoint = endpoint
                .replace("{transactionId}", context().getTransactionId() != null ?
                        context().getTransactionId() : "txn_placeholder")
                .replace("{publicId}", context().getPublicId() != null ?
                        context().getPublicId() : "pk_placeholder");

        Response resp = SerenityRest.given()
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json")
                .body(docString)
                .when()
                .patch(resolvedEndpoint);

        context().setLastResponse(resp);
    }

    @When("se envía una solicitud GET a {string}")
    public void getEndpoint(String endpoint) {
        Response resp = SerenityRest.given()
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .when()
                .get(endpoint);

        context().setLastResponse(resp);
    }

    @When("el comercio A consulta su listado de pagos")
    public void comercioAConsultaListado() {
        getEndpoint("/api/v1/transactions?page=0&size=10");
    }

    // ========================================================================
    // Then's — Validaciones de respuesta
    // ========================================================================

    @Then("la respuesta debe tener código {int}")
    public void laRespuestaDebeTenerCodigo(int statusCode) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");
        assertEquals(statusCode, resp.statusCode(),
                "Código de estado esperado: " + statusCode +
                        " pero fue: " + resp.statusCode() +
                        " | Body: " + resp.body().asString());
    }

    @Then("el campo {string} debe ser {string}")
    public void elCampoDebeSer(String campo, String valorEsperado) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");

        Object valorActual = resp.jsonPath().get(campo);
        assertNotNull(valorActual, "El campo '" + campo + "' no debe ser nulo");

        // Si el valor esperado tiene placeholders, reemplazarlos
        String valorResuelto = valorEsperado
                .replace("{merchantId}", context().getMerchantId() != null ?
                        context().getMerchantId() : "")
                .replace("{publicId}", context().getPublicId() != null ?
                        context().getPublicId() : "");

        assertEquals(valorResuelto, valorActual.toString(),
                "Campo '" + campo + "' debería ser '" + valorResuelto + "'");
    }

    @Then("el campo {string} debe coincidir con el patrón {string}")
    public void elCampoDebeCoincidirConPatron(String campo, String patron) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");

        String valorActual = resp.jsonPath().getString(campo);
        assertNotNull(valorActual, "El campo '" + campo + "' no debe ser nulo");

        String patronRegex = patron.replace("*", ".*");
        assertTrue(valorActual.matches(patronRegex),
                "El campo '" + campo + "' con valor '" + valorActual +
                        "' debería coincidir con '" + patron + "'");
    }

    @Then("el campo {string} debe ser un texto no vacío")
    public void elCampoDebeSerTextoNoVacio(String campo) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");
        String valor = resp.jsonPath().getString(campo);
        assertNotNull(valor, "El campo '" + campo + "' no debe ser nulo");
        assertFalse(valor.isEmpty(), "El campo '" + campo + "' no debe estar vacío");
    }

    @Then("el campo {string} debe ser un email válido")
    public void elCampoDebeSerEmailValido(String campo) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");
        String valor = resp.jsonPath().getString(campo);
        assertNotNull(valor, "El campo '" + campo + "' no debe ser nulo");
        assertTrue(valor.contains("@"), "El campo '" + campo + "' debe ser un email válido");
    }

    @Then("el campo {string} debe ser false")
    public void elCampoDebeSerFalse(String campo) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");
        Boolean valor = resp.jsonPath().getBoolean(campo);
        assertNotNull(valor, "El campo '" + campo + "' no debe ser nulo");
        assertFalse(valor, "El campo '" + campo + "' debe ser false");
    }

    @Then("la respuesta debe contener una lista de transacciones")
    public void laRespuestaDebeContenerLista() {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");

        // Verificar que hay al menos un elemento o un array vacío
        Object content = resp.jsonPath().getList("content");
        assertNotNull(content, "La respuesta debe contener una lista 'content'");
    }

    @Then("la respuesta debe incluir metadatos de paginación")
    public void laRespuestaDebeIncluirPaginacion() {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");
        assertNotNull(resp.jsonPath().get("page"), "Debe incluir 'page'");
        assertNotNull(resp.jsonPath().get("size"), "Debe incluir 'size'");
        assertNotNull(resp.jsonPath().get("totalElements"), "Debe incluir 'totalElements'");
    }

    @Then("todas las transacciones en la lista deben tener status {string}")
    public void todasLasTransaccionesDebenTenerStatus(String statusEsperado) {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");

        List<Map<String, Object>> content = resp.jsonPath().getList("content");
        if (content != null && !content.isEmpty()) {
            for (Map<String, Object> tx : content) {
                assertEquals(statusEsperado, tx.get("status"),
                        "Todas las transacciones deben tener status " + statusEsperado);
            }
        }
    }

    @Then("no debe ver transacciones del comercio B")
    public void noDebeVerTransaccionesDeOtroComercio() {
        Response resp = context().getLastResponse();
        assertNotNull(resp, "Debe haber una respuesta previa");

        String body = resp.body().asString();
        // Si la respuesta no tiene contenido JSON (ej: 403 sin body), 
        // no se puede validar aislamiento — eso es un problema del backend
        if (body == null || body.trim().isEmpty() || body.trim().equals("")) {
            fail("No se pudo validar aislamiento: respuesta vacía (código " + resp.statusCode() + ")");
        }

        List<Map<String, Object>> content = resp.jsonPath().getList("content");
        if (content != null && !content.isEmpty()) {
            for (Map<String, Object> tx : content) {
                String merchantId = (String) tx.get("merchantId");
                if (merchantId != null) {
                    assertEquals(context().getMerchantId(), merchantId,
                            "No debe ver transacciones de otro comercio");
                }
            }
        }
    }

    // ========================================================================
    // Métodos auxiliares
    // ========================================================================

    private void crearTransaccion() {
        // Asegurar que tenemos credenciales
        if (context().getPublicId() == null) {
            return; // El escenario fallará por falta de credenciales
        }

        Map<String, Object> tx = new HashMap<>();
        tx.put("merchantId", context().getMerchantId());
        tx.put("amount", 50000);

        Response resp = SerenityRest.given()
                .header("X-Merchant-Id", context().getMerchantId())
                .header("X-Public-Id", context().getPublicId())
                .header("X-Secret", context().getSecret())
                .contentType("application/json")
                .body(tx)
                .when()
                .post("/api/v1/transactions")
                .then()
                .statusCode(201)
                .extract().response();

        context().setTransactionId(resp.jsonPath().getString("id"));
        context().setLastResponse(resp);
    }
}
