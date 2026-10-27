package com.franquicias.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.infrastructure.persistence.FranquiciaDocument;
import com.franquicias.infrastructure.persistence.FranquiciaMongoAdapter;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Comprueba que convertir dominio -> documento -> dominio no pierde datos (sin necesidad de Mongo). */
class FranquiciaMongoAdapterTest {

    @Test
    void ida_y_vuelta_conservaTodosLosDatos() {
        Franquicia original = new Franquicia("f1", "Frutas SA",
                List.of(new Sucursal("s1", "Centro", List.of(new Producto("p1", "Manzana", 10)))), 3L);

        FranquiciaDocument documento = FranquiciaMongoAdapter.aDocumento(original);
        Franquicia vuelta = FranquiciaMongoAdapter.aDominio(documento);

        assertThat(documento.sucursales().get(0).productos().get(0).stock()).isEqualTo(10);
        assertThat(vuelta).isEqualTo(original);
    }
}
