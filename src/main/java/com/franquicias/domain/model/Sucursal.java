package com.franquicias.domain.model;

import com.franquicias.domain.exception.ConflictoException;
import com.franquicias.domain.exception.RecursoNoEncontradoException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;

/** Sucursal de una franquicia con su lista de productos. Inmutable. */
public record Sucursal(String id, String nombre, List<Producto> productos) {

    public Sucursal {
        nombre = Nombres.limpiar(nombre);
        productos = productos == null ? List.of() : List.copyOf(productos);
    }

    public static Sucursal nueva(String nombre) {
        return new Sucursal(UUID.randomUUID().toString(), nombre, List.of());
    }

    public Sucursal conNombre(String nuevoNombre) {
        return new Sucursal(id, nuevoNombre, productos);
    }

    public boolean tieneNombre(String otroNombre) {
        return nombre.equalsIgnoreCase(otroNombre.trim());
    }

    public Sucursal conProductoAgregado(Producto nuevo) {
        if (productos.stream().anyMatch(p -> p.tieneNombre(nuevo.nombre()))) {
            throw ConflictoException.productoDuplicado(nuevo.nombre());
        }
        List<Producto> lista = new ArrayList<>(productos);
        lista.add(nuevo);
        return new Sucursal(id, nombre, lista);
    }

    public Sucursal conProductoEliminado(String productoId) {
        buscarProducto(productoId); // lanza 404 si no existe
        return new Sucursal(id, nombre, productos.stream().filter(p -> !p.id().equals(productoId)).toList());
    }

    public Sucursal conStockModificado(String productoId, int nuevoStock) {
        return modificarProducto(productoId, p -> p.conStock(nuevoStock));
    }

    public Sucursal conProductoRenombrado(String productoId, String nuevoNombre) {
        String limpio = Nombres.limpiar(nuevoNombre);
        boolean usado = productos.stream().anyMatch(p -> !p.id().equals(productoId) && p.tieneNombre(limpio));
        if (usado) {
            throw ConflictoException.productoDuplicado(limpio);
        }
        return modificarProducto(productoId, p -> p.conNombre(limpio));
    }

    /** Producto con más stock de la sucursal (si hay empate, el primero agregado). Vacío si no hay productos. */
    public Optional<Producto> productoConMasStock() {
        return productos.stream().max(Comparator.comparingInt(Producto::stock));
    }

    private Producto buscarProducto(String productoId) {
        return productos.stream()
                .filter(p -> p.id().equals(productoId))
                .findFirst()
                .orElseThrow(() -> RecursoNoEncontradoException.producto(productoId));
    }

    private Sucursal modificarProducto(String productoId, UnaryOperator<Producto> cambio) {
        buscarProducto(productoId);
        return new Sucursal(id, nombre,
                productos.stream().map(p -> p.id().equals(productoId) ? cambio.apply(p) : p).toList());
    }
}
