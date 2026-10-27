package com.franquicias.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.franquicias.domain.exception.ConflictoException;
import com.franquicias.domain.exception.DatoInvalidoException;
import com.franquicias.domain.exception.RecursoNoEncontradoException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.Sucursal;
import org.junit.jupiter.api.Test;

/** Reglas del modelo de dominio, sin Spring ni base de datos. */
class FranquiciaTest {

    @Test
    void elModeloEsInmutable_agregarDevuelveUnaCopia() {
        Franquicia original = Franquicia.nueva("Frutas");
        Franquicia conSucursal = original.conSucursalAgregada(Sucursal.nueva("Centro"));

        assertThat(original.sucursales()).isEmpty();
        assertThat(conSucursal.sucursales()).hasSize(1);
    }

    @Test
    void nombresSeLimpianYNoPuedenEstarVacios() {
        assertThat(Producto.nuevo("  Uva  ", 1).nombre()).isEqualTo("Uva");
        assertThatThrownBy(() -> Sucursal.nueva(" ")).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void stockNegativoNoEsValido() {
        assertThatThrownBy(() -> Producto.nuevo("Uva", -1)).isInstanceOf(DatoInvalidoException.class);
    }

    @Test
    void noPermiteDosProductosConElMismoNombreEnUnaSucursal() {
        Sucursal s = Sucursal.nueva("Centro").conProductoAgregado(Producto.nuevo("Uva", 1));

        assertThatThrownBy(() -> s.conProductoAgregado(Producto.nuevo("UVA", 2)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void renombrarUnProductoConSuPropioNombreEsValido() {
        Producto uva = Producto.nuevo("Uva", 1);
        Sucursal s = Sucursal.nueva("Centro").conProductoAgregado(uva);

        assertThat(s.conProductoRenombrado(uva.id(), "Uva").productos()).hasSize(1);
    }

    @Test
    void buscarSucursalInexistenteLanza404() {
        assertThatThrownBy(() -> Franquicia.nueva("F").buscarSucursal("x"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
