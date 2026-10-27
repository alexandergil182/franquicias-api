package com.franquicias;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Prueba de extremo a extremo contra un MongoDB real. Solo corre si defines MONGODB_URI_TEST, por ejemplo:
 * {@code MONGODB_URI_TEST=mongodb://localhost:27017 mvn test}
 * Usa una base de datos con nombre único para no ensuciar los datos locales.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@EnabledIfEnvironmentVariable(named = "MONGODB_URI_TEST", matches = ".+")
class FranquiciaIntegracionTest {

    @DynamicPropertySource
    static void mongo(DynamicPropertyRegistry registry) {
        String base = System.getenv("MONGODB_URI_TEST").replaceAll("/+$", "");
        registry.add("spring.data.mongodb.uri", () -> base + "/franquicias_test_" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Autowired
    WebTestClient cliente;

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String uri, String json) {
        return cliente.post().uri(uri).header("Content-Type", "application/json").bodyValue(json)
                .exchange().expectStatus().isCreated().expectBody(Map.class).returnResult().getResponseBody();
    }

    @Test
    void flujoCompleto() {
        String f = (String) post("/api/franquicias", "{\"nombre\":\"Frutas SA\"}").get("id");
        String s1 = (String) post("/api/franquicias/" + f + "/sucursales", "{\"nombre\":\"Centro\"}").get("id");
        String s2 = (String) post("/api/franquicias/" + f + "/sucursales", "{\"nombre\":\"Norte\"}").get("id");
        String base1 = "/api/franquicias/" + f + "/sucursales/" + s1 + "/productos";
        String manzana = (String) post(base1, "{\"nombre\":\"Manzana\",\"stock\":10}").get("id");
        post(base1, "{\"nombre\":\"Pera\",\"stock\":25}");
        post("/api/franquicias/" + f + "/sucursales/" + s2 + "/productos", "{\"nombre\":\"Kiwi\",\"stock\":3}");

        // subir el stock de la manzana la convierte en la de mayor stock de Centro
        cliente.patch().uri(base1 + "/" + manzana + "/stock").header("Content-Type", "application/json")
                .bodyValue("{\"stock\":100}").exchange().expectStatus().isOk();

        cliente.get().uri("/api/franquicias/" + f + "/productos-mas-stock").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].sucursalNombre").isEqualTo("Centro")
                .jsonPath("$[0].producto.nombre").isEqualTo("Manzana")
                .jsonPath("$[0].producto.stock").isEqualTo(100)
                .jsonPath("$[1].sucursalNombre").isEqualTo("Norte")
                .jsonPath("$[1].producto.nombre").isEqualTo("Kiwi");

        // eliminar la manzana deja a la pera como la de mayor stock
        cliente.delete().uri(base1 + "/" + manzana).exchange().expectStatus().isNoContent();
        cliente.get().uri("/api/franquicias/" + f + "/productos-mas-stock").exchange()
                .expectBody().jsonPath("$[0].producto.nombre").isEqualTo("Pera");

        // errores: franquicia inexistente y nombre repetido
        cliente.get().uri("/api/franquicias/no-existe/productos-mas-stock").exchange().expectStatus().isNotFound();
        cliente.post().uri("/api/franquicias").header("Content-Type", "application/json")
                .bodyValue("{\"nombre\":\"frutas sa\"}").exchange().expectStatus().isEqualTo(409);

        var franquicia = cliente.get().uri("/api/franquicias/" + f).exchange().expectStatus().isOk()
                .expectBody(Map.class).returnResult().getResponseBody();
        assertThat(franquicia).containsEntry("nombre", "Frutas SA");
    }
}
