package com.franquicias.application;

import com.franquicias.domain.exception.ConflictoException;
import com.franquicias.domain.exception.RecursoNoEncontradoException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoDeSucursal;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.in.FranquiciaUseCase;
import com.franquicias.domain.port.out.FranquiciaRepositoryPort;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Casos de uso. Patrón de casi todos los métodos: cargar la franquicia, aplicar el cambio
 * (la regla de negocio vive en el modelo de dominio) y guardarla. Nada se ejecuta hasta que
 * alguien se suscribe al Mono/Flux resultante (lo hace WebFlux al atender la petición).
 */
@Service
public class FranquiciaService implements FranquiciaUseCase {

    private final FranquiciaRepositoryPort repositorio;

    public FranquiciaService(FranquiciaRepositoryPort repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Mono<Franquicia> crearFranquicia(String nombre) {
        // defer: así los errores de validación del modelo viajan como error del Mono
        return Mono.defer(() -> {
            Franquicia nueva = Franquicia.nueva(nombre);
            return verificarNombreLibre(nueva.nombre(), nueva.id())
                    .then(Mono.defer(() -> repositorio.guardar(nueva)));
        });
    }

    @Override
    public Mono<Franquicia> obtenerFranquicia(String id) {
        return cargar(id);
    }

    @Override
    public Flux<Franquicia> listarFranquicias() {
        return repositorio.buscarTodas();
    }

    @Override
    public Mono<Franquicia> renombrarFranquicia(String franquiciaId, String nuevoNombre) {
        return cargar(franquiciaId)
                .map(f -> f.conNombre(nuevoNombre))
                .flatMap(f -> verificarNombreLibre(f.nombre(), f.id()).then(Mono.defer(() -> repositorio.guardar(f))));
    }

    @Override
    public Mono<Sucursal> agregarSucursal(String franquiciaId, String nombre) {
        return Mono.defer(() -> {
            Sucursal nueva = Sucursal.nueva(nombre);
            return actualizar(franquiciaId, f -> f.conSucursalAgregada(nueva)).thenReturn(nueva);
        });
    }

    @Override
    public Mono<Sucursal> renombrarSucursal(String franquiciaId, String sucursalId, String nuevoNombre) {
        return actualizar(franquiciaId, f -> f.conSucursalRenombrada(sucursalId, nuevoNombre))
                .map(f -> f.buscarSucursal(sucursalId));
    }

    @Override
    public Mono<Producto> agregarProducto(String franquiciaId, String sucursalId, String nombre, int stock) {
        return Mono.defer(() -> {
            Producto nuevo = Producto.nuevo(nombre, stock);
            return actualizar(franquiciaId, f -> f.conProductoAgregado(sucursalId, nuevo)).thenReturn(nuevo);
        });
    }

    @Override
    public Mono<Void> eliminarProducto(String franquiciaId, String sucursalId, String productoId) {
        return actualizar(franquiciaId, f -> f.conProductoEliminado(sucursalId, productoId)).then();
    }

    @Override
    public Mono<Producto> modificarStock(String franquiciaId, String sucursalId, String productoId, int nuevoStock) {
        return actualizar(franquiciaId, f -> f.conStockModificado(sucursalId, productoId, nuevoStock))
                .map(f -> f.buscarProducto(sucursalId, productoId));
    }

    @Override
    public Mono<Producto> renombrarProducto(String franquiciaId, String sucursalId, String productoId,
                                            String nuevoNombre) {
        return actualizar(franquiciaId, f -> f.conProductoRenombrado(sucursalId, productoId, nuevoNombre))
                .map(f -> f.buscarProducto(sucursalId, productoId));
    }

    @Override
    public Flux<ProductoDeSucursal> productosConMasStock(String franquiciaId) {
        return cargar(franquiciaId)
                .flatMapMany(f -> Flux.fromIterable(f.productosConMasStockPorSucursal()));
    }

    // ---- ayudas internas ----

    private Mono<Franquicia> cargar(String id) {
        return repositorio.buscarPorId(id)
                .switchIfEmpty(Mono.error(() -> RecursoNoEncontradoException.franquicia(id)));
    }

    /** Carga la franquicia, le aplica el cambio y la guarda. Si el cambio lanza una excepción, no se guarda nada. */
    private Mono<Franquicia> actualizar(String franquiciaId, UnaryOperator<Franquicia> cambio) {
        return cargar(franquiciaId)
                .map(cambio)
                .flatMap(repositorio::guardar);
    }

    /** Falla con 409 si otra franquicia (distinta de {@code idPropio}) ya usa ese nombre. */
    private Mono<Void> verificarNombreLibre(String nombre, String idPropio) {
        return repositorio.buscarPorNombre(nombre)
                .filter(otra -> !otra.id().equals(idPropio))
                .flatMap(otra -> Mono.<Void>error(ConflictoException.franquiciaDuplicada(nombre)));
    }
}
