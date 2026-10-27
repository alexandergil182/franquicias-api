package com.franquicias.infrastructure.persistence;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Cómo se guarda una franquicia en MongoDB: un único documento con las sucursales y productos anidados
 * (equivale a un Model de Django, pero con los hijos embebidos en vez de tablas con ForeignKey).
 * Es independiente del modelo de dominio: el adaptador convierte entre uno y otro.
 */
@Document(collection = "franquicias")
public record FranquiciaDocument(
        @Id String id,
        @Indexed(unique = true) String nombre,
        List<SucursalDocument> sucursales,
        @Version Long version) {

    public record SucursalDocument(String id, String nombre, List<ProductoDocument> productos) {
    }

    public record ProductoDocument(String id, String nombre, int stock) {
    }
}
