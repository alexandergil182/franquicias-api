package com.franquicias.domain.model;

import com.franquicias.domain.exception.ConflictoException;
import com.franquicias.domain.exception.RecursoNoEncontradoException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * Raíz del agregado: una franquicia con sus sucursales (y estas con sus productos).
 * Todas las reglas de negocio viven aquí; no sabe nada de Mongo ni de HTTP.
 *
 * @param version número de versión para el bloqueo optimista (null mientras no se haya guardado)
 */
public record Franquicia(String id, String nombre, List<Sucursal> sucursales, Long version) {

    public Franquicia {
        nombre = Nombres.limpiar(nombre);
        sucursales = sucursales == null ? List.of() : List.copyOf(sucursales);
    }

    public static Franquicia nueva(String nombre) {
        return new Franquicia(UUID.randomUUID().toString(), nombre, List.of(), null);
    }

    public Franquicia conNombre(String nuevoNombre) {
        return new Franquicia(id, nuevoNombre, sucursales, version);
    }

    public Franquicia conSucursalAgregada(Sucursal nueva) {
        if (sucursales.stream().anyMatch(s -> s.tieneNombre(nueva.nombre()))) {
            throw ConflictoException.sucursalDuplicada(nueva.nombre());
        }
        List<Sucursal> lista = new ArrayList<>(sucursales);
        lista.add(nueva);
        return new Franquicia(id, nombre, lista, version);
    }

    public Franquicia conSucursalRenombrada(String sucursalId, String nuevoNombre) {
        String limpio = Nombres.limpiar(nuevoNombre);
        boolean usado = sucursales.stream().anyMatch(s -> !s.id().equals(sucursalId) && s.tieneNombre(limpio));
        if (usado) {
            throw ConflictoException.sucursalDuplicada(limpio);
        }
        return modificarSucursal(sucursalId, s -> s.conNombre(limpio));
    }

    public Franquicia conProductoAgregado(String sucursalId, Producto producto) {
        return modificarSucursal(sucursalId, s -> s.conProductoAgregado(producto));
    }

    public Franquicia conProductoEliminado(String sucursalId, String productoId) {
        return modificarSucursal(sucursalId, s -> s.conProductoEliminado(productoId));
    }

    public Franquicia conStockModificado(String sucursalId, String productoId, int nuevoStock) {
        return modificarSucursal(sucursalId, s -> s.conStockModificado(productoId, nuevoStock));
    }

    public Franquicia conProductoRenombrado(String sucursalId, String productoId, String nuevoNombre) {
        return modificarSucursal(sucursalId, s -> s.conProductoRenombrado(productoId, nuevoNombre));
    }

    public Sucursal buscarSucursal(String sucursalId) {
        return sucursales.stream()
                .filter(s -> s.id().equals(sucursalId))
                .findFirst()
                .orElseThrow(() -> RecursoNoEncontradoException.sucursal(sucursalId));
    }

    public Producto buscarProducto(String sucursalId, String productoId) {
        return buscarSucursal(sucursalId).productos().stream()
                .filter(p -> p.id().equals(productoId))
                .findFirst()
                .orElseThrow(() -> RecursoNoEncontradoException.producto(productoId));
    }

    /** Por cada sucursal con productos, su producto de mayor stock junto al nombre de la sucursal. */
    public List<ProductoDeSucursal> productosConMasStockPorSucursal() {
        return sucursales.stream()
                .flatMap(s -> s.productoConMasStock()
                        .map(p -> new ProductoDeSucursal(s.id(), s.nombre(), p))
                        .stream())
                .toList();
    }

    private Franquicia modificarSucursal(String sucursalId, UnaryOperator<Sucursal> cambio) {
        buscarSucursal(sucursalId); // lanza 404 si no existe
        List<Sucursal> lista = sucursales.stream()
                .map(s -> s.id().equals(sucursalId) ? cambio.apply(s) : s)
                .toList();
        return new Franquicia(id, nombre, lista, version);
    }
}
