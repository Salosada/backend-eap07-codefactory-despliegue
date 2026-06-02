package com.codefactory.appstripe.integration;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("local")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationApiTest {

    @LocalServerPort
    int port;

    private String adminToken;
    private String merchantId;
    private String publicId;
    private String secret;
    private String paymentId;
    private static long ts;

    @BeforeAll
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        ts = System.currentTimeMillis();

        Map<String, Object> login = new HashMap<>();
        login.put("email", "admin@paycore.com");
        login.put("password", "admin123");

        adminToken = given()
                .contentType("application/json")
                .body(login)
            .when().post("/api/v1/auth/login")
            .then().statusCode(200)
                .body("token", notNullValue())
                .body("role", equalTo("ADMIN"))
                .extract().path("token");

        Assertions.assertNotNull(adminToken, "Token de admin debe ser retornado");
    }

    @Test
    @Order(1)
    @DisplayName("CP-S1-001: Registro exitoso de comercio con datos válidos -> HTTP 201 + merchantId")
    void cp_s1_001_registerMerchantSuccess() {
        Map<String, Object> merchant = new HashMap<>();
        merchant.put("businessName", "Mi Tienda SAS " + ts);
        merchant.put("businessId", "biz_" + ts);
        merchant.put("email", "qa." + ts + "@mitienda.local");
        merchant.put("businessType", "RETAIL");

        Response resp = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(merchant)
            .when().post("/api/v1/admin/merchants")
            .then().statusCode(201)
                .body("id", startsWith("mch_"))
                .body("businessName", equalTo("Mi Tienda SAS " + ts))
                .body("status", equalTo("VERIFIED"))
                .extract().response();

        merchantId = resp.path("id");
        Assertions.assertNotNull(merchantId, "CP-S1-001: merchantId no debe ser nulo");
    }

    @Test
    @Order(2)
    @DisplayName("CP-S1-003: Registro con email duplicado -> HTTP 409 Conflict")
    void cp_s1_003_registerDuplicateEmail() {
        Map<String, Object> merchant = new HashMap<>();
        merchant.put("businessName", "Otra Tienda");
        merchant.put("businessId", "biz_other_" + ts);
        merchant.put("email", "qa." + ts + "@mitienda.local");
        merchant.put("businessType", "RETAIL");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(merchant)
            .when().post("/api/v1/admin/merchants")
            .then().statusCode(409)
                .body("errorCode", equalTo("BUSINESS_RULE_VIOLATION"))
                .body("message", containsString("correo electrónico"));
    }

    @Test
    @Order(3)
    @DisplayName("CP-S1-004: Registro con campos obligatorios vacíos -> HTTP 400 Bad Request")
    void cp_s1_004_registerBlankFields() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body("""
                        {
                          "businessName": "",
                          "businessId": "",
                          "email": "",
                          "businessType": ""
                        }
                        """)
            .when().post("/api/v1/admin/merchants")
            .then().statusCode(400)
                .body("errorCode", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @Order(4)
    @DisplayName("CP-S1-005: Generación exitosa de credenciales -> HTTP 201 + publicKey + secretKey")
    void cp_s1_005_generateCredentials() {
        Map<String, Object> gen = new HashMap<>();
        gen.put("merchantId", merchantId);

        Response genResp = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType("application/json")
                .body(gen)
            .when().post("/api/v1/admin/credentials/generate")
            .then().statusCode(201)
                .body("publicId", startsWith("pk_"))
                .body("secret", startsWith("sk_"))
                .extract().response();

        publicId = genResp.path("publicId");
        secret = genResp.path("secret");
        Assertions.assertNotNull(publicId, "CP-S1-005: publicId no debe ser nulo");
        Assertions.assertNotNull(secret, "CP-S1-005: secret no debe ser nulo");
    }

    @Test
    @Order(5)
    @DisplayName("CP-S1-008: Creación exitosa de transacción -> HTTP 201 + paymentId + status CREATED")
    void cp_s1_008_createTransactionSuccess() {
        Map<String, Object> tx = new HashMap<>();
        tx.put("merchantId", merchantId);
        tx.put("amount", 10000);

        paymentId = given()
                .header("X-Merchant-Id", merchantId)
                .header("X-Public-Id", publicId)
                .header("X-Secret", secret)
                .contentType("application/json")
                .body(tx)
            .when().post("/api/v1/transactions")
            .then().statusCode(201)
                .body("id", notNullValue())
                .body("merchantId", equalTo(merchantId))
                .body("status", equalTo("CREATED"))
                .extract().path("id");

        Assertions.assertNotNull(paymentId, "CP-S1-008: paymentId no debe ser nulo");
    }

    @Test
    @Order(6)
    @DisplayName("CP-S1-008 (GET): Consulta de transacción creada -> HTTP 200 + datos correctos")
    void cp_s1_008_getTransaction() {
        given()
                .header("X-Merchant-Id", merchantId)
                .header("X-Public-Id", publicId)
                .header("X-Secret", secret)
            .when().get("/api/v1/transactions/{id}", paymentId)
            .then().statusCode(200)
                .body("id", equalTo(paymentId))
                .body("merchantId", equalTo(merchantId))
                .body("status", equalTo("CREATED"));
    }

    @Test
    @Order(7)
    @DisplayName("CP-S1-010: Transacción con monto inválido (cero) -> HTTP 400")
    void cp_s1_010_createTransactionZeroAmount() {
        Map<String, Object> tx = new HashMap<>();
        tx.put("merchantId", merchantId);
        tx.put("amount", 0);

        given()
                .header("X-Merchant-Id", merchantId)
                .header("X-Public-Id", publicId)
                .header("X-Secret", secret)
                .contentType("application/json")
                .body(tx)
            .when().post("/api/v1/transactions")
            .then().statusCode(400)
                .body("errorCode", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @Order(8)
    @DisplayName("CP-S1-010: Transacción con monto inválido (negativo) -> HTTP 400")
    void cp_s1_010_createTransactionNegativeAmount() {
        Map<String, Object> tx = new HashMap<>();
        tx.put("merchantId", merchantId);
        tx.put("amount", -500);

        given()
                .header("X-Merchant-Id", merchantId)
                .header("X-Public-Id", publicId)
                .header("X-Secret", secret)
                .contentType("application/json")
                .body(tx)
            .when().post("/api/v1/transactions")
            .then().statusCode(400)
                .body("errorCode", equalTo("VALIDATION_ERROR"));
    }

    @Test
    @Order(9)
    @DisplayName("CP-S1-011: Transacción con credenciales inválidas -> HTTP 401")
    void cp_s1_011_createTransactionInvalidCredentials() {
        Map<String, Object> tx = new HashMap<>();
        tx.put("merchantId", merchantId);
        tx.put("amount", 10000);

        given()
                .header("X-Merchant-Id", merchantId)
                .header("X-Public-Id", "pk_live_fake")
                .header("X-Secret", "sk_live_fake")
                .contentType("application/json")
                .body(tx)
            .when().post("/api/v1/transactions")
            .then().statusCode(401)
                .body("errorCode", equalTo("INVALID_CREDENTIALS"));
    }

    @Test
    @Order(10)
    @DisplayName("CP-S1-011: Sin headers de credenciales -> HTTP 401 MISSING_CREDENTIALS")
    void cp_s1_011_createTransactionMissingCredentials() {
        Map<String, Object> tx = new HashMap<>();
        tx.put("merchantId", merchantId);
        tx.put("amount", 10000);

        given()
                .contentType("application/json")
                .body(tx)
            .when().post("/api/v1/transactions")
            .then().statusCode(401)
                .body("errorCode", equalTo("MISSING_CREDENTIALS"));
    }
}
