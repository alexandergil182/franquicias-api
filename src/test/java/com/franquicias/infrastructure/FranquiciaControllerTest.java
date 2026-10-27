package com.franquicias.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.ConflictoException;
import com.franquicias.domain.exception.RecursoNoEncontradoException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoDeSucursal;
import com.franquicias.domain.port.in.FranquiciaUseCase;
import com.franquicias.infrastructure.web.FranquiciaController;
import com.franquicias.infrastructure.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Prueba la capa HTTP (rutas, validación y códigos de error) con el caso de uso simulado. */
@WebFluxTest(FranquiciaController.class)
@Import(GlobalExceptionHandler.class)
class FranquiciaControllerTest {

    @Autowired
    WebTestClient cliente;

    @MockitoBean
    FranquiciaUseCase casosDeUso;

    @Test
    void postFranquicia_devuelve201ConElCuerpo() {
        when(casosDeUso.crearFranquicia("Frutas SA")).thenReturn(Mono.just(Franquicia.nueva("Frutas SA")));

        cliente.post().uri("/api/franquicias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":\"Frutas SA\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.nombre").isEqualTo("Frutas SA")
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.sucursales").isEmpty();
    }

    @Test
    void postFranquicia_sinNombre_devuelve400ConDetalle() {
        cliente.post().uri("/api/franquicias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":\"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.estado").isEqualTo(400)
                .jsonPath("$.mensaje").isEqualTo("La petición tiene datos inválidos")
                .jsonPath("$.detalles[0]").isEqualTo("nombre: El nombre es obligatorio")
                .jsonPath("$.ruta").isEqualTo("/api/franquicias");
    }

    @Test
    void jsonMalFormado_devuelve400() {
        cliente.post().uri("/api/franquicias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{nombre:")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.estado").isEqualTo(400);
    }

    @Test
    void postSucursal_enFranquiciaInexistente_devuelve404() {
        when(casosDeUso.agregarSucursal(eq("x"), any()))
                .thenReturn(Mono.error(RecursoNoEncontradoException.franquicia("x")));

        cliente.post().uri("/api/franquicias/x/sucursales")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":\"Norte\"}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.estado").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.mensaje").isEqualTo("No existe la franquicia con id x");
    }

    @Test
    void postFranquicia_nombreRepetido_devuelve409() {
        when(casosDeUso.crearFranquicia("Frutas SA"))
                .thenReturn(Mono.error(ConflictoException.franquiciaDuplicada("Frutas SA")));

        cliente.post().uri("/api/franquicias")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":\"Frutas SA\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.estado").isEqualTo(409);
    }

    @Test
    void postProducto_conStockNegativo_devuelve400() {
        cliente.post().uri("/api/franquicias/f/sucursales/s/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"nombre\":\"Uva\",\"stock\":-5}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.detalles[0]").isEqualTo("stock: El stock no puede ser negativo");
    }

    @Test
    void deleteProducto_devuelve204() {
        when(casosDeUso.eliminarProducto("f", "s", "p")).thenReturn(Mono.empty());

        cliente.delete().uri("/api/franquicias/f/sucursales/s/productos/p")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void patchStock_devuelveElProductoActualizado() {
        when(casosDeUso.modificarStock(eq("f"), eq("s"), eq("p"), anyInt()))
                .thenReturn(Mono.just(new Producto("p", "Uva", 50)));

        cliente.patch().uri("/api/franquicias/f/sucursales/s/productos/p/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"stock\":50}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.stock").isEqualTo(50);
    }

    @Test
    void getProductosMasStock_devuelveListaConLaSucursal() {
        when(casosDeUso.productosConMasStock("f")).thenReturn(Flux.just(
                new ProductoDeSucursal("s1", "Centro", new Producto("p1", "Pera", 25)),
                new ProductoDeSucursal("s2", "Norte", new Producto("p2", "Kiwi", 40))));

        cliente.get().uri("/api/franquicias/f/productos-mas-stock")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].sucursalNombre").isEqualTo("Centro")
                .jsonPath("$[0].producto.nombre").isEqualTo("Pera")
                .jsonPath("$[1].sucursalId").isEqualTo("s2")
                .jsonPath("$[1].producto.stock").isEqualTo(40);
    }

    @Test
    void rutaInexistente_devuelve404ConJsonUniforme() {
        cliente.get().uri("/api/nada")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.estado").isEqualTo(404);
    }
}
