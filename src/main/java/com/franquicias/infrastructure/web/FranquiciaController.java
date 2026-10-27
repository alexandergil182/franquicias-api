package com.franquicias.infrastructure.web;

import com.franquicias.domain.port.in.FranquiciaUseCase;
import com.franquicias.infrastructure.web.dto.FranquiciaResponse;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.ProductoRequest;
import com.franquicias.infrastructure.web.dto.ProductoResponse;
import com.franquicias.infrastructure.web.dto.ProductoSucursalResponse;
import com.franquicias.infrastructure.web.dto.StockRequest;
import com.franquicias.infrastructure.web.dto.SucursalResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Adaptador de entrada REST (equivale a un ViewSet de DRF): recibe la petición, valida el cuerpo
 * con @Valid, llama al caso de uso y convierte el resultado a DTO. No contiene lógica de negocio.
 */
@RestController
@RequestMapping("/api/franquicias")
public class FranquiciaController {

    private final FranquiciaUseCase casosDeUso;

    public FranquiciaController(FranquiciaUseCase casosDeUso) {
        this.casosDeUso = casosDeUso;
    }

    // ---- Franquicias ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<FranquiciaResponse> crearFranquicia(@Valid @RequestBody NombreRequest request) {
        return casosDeUso.crearFranquicia(request.nombre()).map(FranquiciaResponse::desde);
    }

    @GetMapping
    public Flux<FranquiciaResponse> listarFranquicias() {
        return casosDeUso.listarFranquicias().map(FranquiciaResponse::desde);
    }

    @GetMapping("/{id}")
    public Mono<FranquiciaResponse> obtenerFranquicia(@PathVariable String id) {
        return casosDeUso.obtenerFranquicia(id).map(FranquiciaResponse::desde);
    }

    @PutMapping("/{id}/nombre")
    public Mono<FranquiciaResponse> renombrarFranquicia(@PathVariable String id,
                                                        @Valid @RequestBody NombreRequest request) {
        return casosDeUso.renombrarFranquicia(id, request.nombre()).map(FranquiciaResponse::desde);
    }

    // ---- Sucursales ----

    @PostMapping("/{franquiciaId}/sucursales")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SucursalResponse> agregarSucursal(@PathVariable String franquiciaId,
                                                  @Valid @RequestBody NombreRequest request) {
        return casosDeUso.agregarSucursal(franquiciaId, request.nombre()).map(SucursalResponse::desde);
    }

    @PutMapping("/{franquiciaId}/sucursales/{sucursalId}/nombre")
    public Mono<SucursalResponse> renombrarSucursal(@PathVariable String franquiciaId,
                                                    @PathVariable String sucursalId,
                                                    @Valid @RequestBody NombreRequest request) {
        return casosDeUso.renombrarSucursal(franquiciaId, sucursalId, request.nombre()).map(SucursalResponse::desde);
    }

    // ---- Productos ----

    @PostMapping("/{franquiciaId}/sucursales/{sucursalId}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductoResponse> agregarProducto(@PathVariable String franquiciaId,
                                                  @PathVariable String sucursalId,
                                                  @Valid @RequestBody ProductoRequest request) {
        return casosDeUso.agregarProducto(franquiciaId, sucursalId, request.nombre(), request.stock())
                .map(ProductoResponse::desde);
    }

    @DeleteMapping("/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminarProducto(@PathVariable String franquiciaId,
                                       @PathVariable String sucursalId,
                                       @PathVariable String productoId) {
        return casosDeUso.eliminarProducto(franquiciaId, sucursalId, productoId);
    }

    @PatchMapping("/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/stock")
    public Mono<ProductoResponse> modificarStock(@PathVariable String franquiciaId,
                                                 @PathVariable String sucursalId,
                                                 @PathVariable String productoId,
                                                 @Valid @RequestBody StockRequest request) {
        return casosDeUso.modificarStock(franquiciaId, sucursalId, productoId, request.stock())
                .map(ProductoResponse::desde);
    }

    @PutMapping("/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/nombre")
    public Mono<ProductoResponse> renombrarProducto(@PathVariable String franquiciaId,
                                                    @PathVariable String sucursalId,
                                                    @PathVariable String productoId,
                                                    @Valid @RequestBody NombreRequest request) {
        return casosDeUso.renombrarProducto(franquiciaId, sucursalId, productoId, request.nombre())
                .map(ProductoResponse::desde);
    }

    // ---- Consulta ----

    /** Producto con más stock de cada sucursal de la franquicia, indicando la sucursal. */
    @GetMapping("/{id}/productos-mas-stock")
    public Flux<ProductoSucursalResponse> productosConMasStock(@PathVariable String id) {
        return casosDeUso.productosConMasStock(id).map(ProductoSucursalResponse::desde);
    }
}
