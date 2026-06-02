package com.codefactory.appstripe.serenity.steps;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.codefactory.appstripe.AppstripeApplication;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;


import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.Data;
import net.serenitybdd.rest.SerenityRest;

/**
 * Steps comunes compartidos por todos los features.
 * Configuración global del actor, Background de features y estado compartido.
 */
public class CommonSteps {

    private static ConfigurableApplicationContext springContext;

    private static final ThreadLocal<TestContext> contextHolder = new ThreadLocal<>();

    /**
     * Contexto compartido entre steps dentro de un mismo escenario.
     * Almacena IDs, tokens, credenciales y última respuesta.
     */
    @Data
    public static class TestContext {
        private String merchantId;
        private String publicId;
        private String secret;
        private String adminToken;
        private String csrfToken;
        private String csrfHeaderName;
        private String csrfCookie;
        private String transactionId;
        private Response lastResponse;
        private long timestamp = System.currentTimeMillis();

        public String getUniqueEmail(String prefix) {
            return prefix + "." + timestamp + "@test.local";
        }
    }

    public static TestContext context() {
        if (contextHolder.get() == null) {
            contextHolder.set(new TestContext());
        }
        return contextHolder.get();
    }

    public static void resetContext() {
        contextHolder.remove();
    }

    public static ConfigurableApplicationContext springContext() {
        return springContext;
    }

    @BeforeAll
    public static void startSpringBoot() {
        if (springContext == null || !springContext.isRunning()) {
            springContext = SpringApplication.run(AppstripeApplication.class,
                    "--server.port=8083",
                    "--spring.profiles.active=test");
        }
        RestAssured.port = 8083;
    }

    @Before(order = 0)
    public void setTheStage() {
        // Configurar puerto del servidor embebido
        RestAssured.port = 8083;
        resetContext();
    }

    @ParameterType(".*")
    public String text(String value) {
        return value;
    }

    @Given("el sistema de pagos está listo para recibir peticiones")
    public void elSistemaEstaListo() {
        SerenityRest.get("/actuator/health")
                .then().statusCode(200);
    }
}
