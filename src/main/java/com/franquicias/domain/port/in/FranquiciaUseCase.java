package com.franquicias.domain.port.in;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoDeSucursal;
import com.franquicias.domain.model.Sucursal;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Puerto de entrada: todo lo que se puede hacer con las franquicias. Los controladores solo conocen esto. */
public interface FranquiciaUseCase {

    Mono<Franquicia> crearFranquicia(String nombre);

    Mono<Franquicia> obtenerFranquicia(String id);

    Flux<Franquicia> listarFranquicias();

    Mono<Franquicia> renombrarFranquicia(String franquiciaId, String nuevoNombre);

    Mono<Sucursal> agregarSucursal(String franquiciaId, String nombre);

    Mono<Sucursal> renombrarSucursal(String franquiciaId, String sucursalId, String nuevoNombre);

    Mono<Producto> agregarProducto(String franquiciaId, String sucursalId, String nombre, int stock);

    Mono<Void> eliminarProducto(String franquiciaId, String sucursalId, String productoId);

    Mono<Producto> modificarStock(String franquiciaId, String sucursalId, String productoId, int nuevoStock);

    Mono<Producto> renombrarProducto(String franquiciaId, String sucursalId, String productoId, String nuevoNombre);

    /** Producto con más stock de cada sucursal de la franquicia. */
    Flux<ProductoDeSucursal> productosConMasStock(String franquiciaId);
}
