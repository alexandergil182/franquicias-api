package com.franquicias.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.domain.exception.ConflictoException;
import com.franquicias.domain.exception.DatoInvalidoException;
import com.franquicias.domain.exception.RecursoNoEncontradoException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.out.FranquiciaRepositoryPort;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** Pruebas de los casos de uso con el repositorio simulado (Mockito) y verificando el Mono con StepVerifier. */
@ExtendWith(MockitoExtension.class)
class FranquiciaServiceTest {

    @Mock
    FranquiciaRepositoryPort repositorio;

    FranquiciaService servicio;

    // Datos de partida: franquicia "f1" con la sucursal "s1" (Centro) que tiene dos productos
    Producto manzana = new Producto("p1", "Manzana", 10);
    Producto pera = new Producto("p2", "Pera", 25);
    Sucursal centro = new Sucursal("s1", "Centro", List.of(manzana, pera));
    Franquicia franquicia = new Franquicia("f1", "Frutas SA", List.of(centro), 0L);

    @BeforeEach
    void preparar() {
        servicio = new FranquiciaService(repositorio);
    }

    /** Hace que guardar() devuelva lo mismo que recibe, como haría la base de datos. */
    private void guardarDevuelveLoRecibido() {
        when(repositorio.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
    }

    private Franquicia franquiciaGuardada() {
        ArgumentCaptor<Franquicia> captor = ArgumentCaptor.forClass(Franquicia.class);
        verify(repositorio).guardar(captor.capture());
        return captor.getValue();
    }

    // ---- franquicias ----

    @Test
    void crearFranquicia_guardaYDevuelveLaNueva() {
        when(repositorio.buscarPorNombre("Frutas SA")).thenReturn(Mono.empty());
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.crearFranquicia("  Frutas SA "))
                .assertNext(f -> {
                    assertThat(f.id()).isNotBlank();
                    assertThat(f.nombre()).isEqualTo("Frutas SA");
                    assertThat(f.sucursales()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void crearFranquicia_conNombreRepetido_dispara409() {
        when(repositorio.buscarPorNombre("Frutas SA")).thenReturn(Mono.just(franquicia));

        StepVerifier.create(servicio.crearFranquicia("Frutas SA"))
                .expectError(ConflictoException.class)
                .verify();
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void crearFranquicia_conNombreVacio_dispara400() {
        StepVerifier.create(servicio.crearFranquicia("   "))
                .expectError(DatoInvalidoException.class)
                .verify();
    }

    @Test
    void renombrarFranquicia_cambiaElNombre() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));
        when(repositorio.buscarPorNombre("Nuevo")).thenReturn(Mono.empty());
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.renombrarFranquicia("f1", "Nuevo"))
                .assertNext(f -> assertThat(f.nombre()).isEqualTo("Nuevo"))
                .verifyComplete();
    }

    // ---- sucursales ----

    @Test
    void agregarSucursal_laAgregaALaFranquicia() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.agregarSucursal("f1", "Norte"))
                .assertNext(s -> {
                    assertThat(s.nombre()).isEqualTo("Norte");
                    assertThat(s.productos()).isEmpty();
                })
                .verifyComplete();

        assertThat(franquiciaGuardada().sucursales()).extracting(Sucursal::nombre).containsExactly("Centro", "Norte");
    }

    @Test
    void agregarSucursal_conNombreRepetido_dispara409() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));

        StepVerifier.create(servicio.agregarSucursal("f1", "centro"))
                .expectError(ConflictoException.class)
                .verify();
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void agregarSucursal_aFranquiciaInexistente_dispara404() {
        when(repositorio.buscarPorId("no-existe")).thenReturn(Mono.empty());

        StepVerifier.create(servicio.agregarSucursal("no-existe", "Norte"))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void renombrarSucursal_cambiaElNombre() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.renombrarSucursal("f1", "s1", "Centro Histórico"))
                .assertNext(s -> assertThat(s.nombre()).isEqualTo("Centro Histórico"))
                .verifyComplete();
    }

    // ---- productos ----

    @Test
    void agregarProducto_loAgregaALaSucursal() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.agregarProducto("f1", "s1", "Uva", 7))
                .assertNext(p -> {
                    assertThat(p.nombre()).isEqualTo("Uva");
                    assertThat(p.stock()).isEqualTo(7);
                })
                .verifyComplete();

        assertThat(franquiciaGuardada().buscarSucursal("s1").productos()).hasSize(3);
    }

    @Test
    void agregarProducto_aSucursalInexistente_dispara404() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));

        StepVerifier.create(servicio.agregarProducto("f1", "no-existe", "Uva", 7))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }

    @Test
    void eliminarProducto_loQuitaDeLaSucursal() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.eliminarProducto("f1", "s1", "p1")).verifyComplete();

        assertThat(franquiciaGuardada().buscarSucursal("s1").productos()).containsExactly(pera);
    }

    @Test
    void eliminarProducto_inexistente_dispara404_yNoGuarda() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));

        StepVerifier.create(servicio.eliminarProducto("f1", "s1", "no-existe"))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void modificarStock_actualizaSoloEseProducto() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.modificarStock("f1", "s1", "p1", 99))
                .assertNext(p -> {
                    assertThat(p.id()).isEqualTo("p1");
                    assertThat(p.stock()).isEqualTo(99);
                })
                .verifyComplete();

        Sucursal guardada = franquiciaGuardada().buscarSucursal("s1");
        assertThat(guardada.productos()).extracting(Producto::stock).containsExactly(99, 25);
    }

    @Test
    void modificarStock_negativo_dispara400() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));

        StepVerifier.create(servicio.modificarStock("f1", "s1", "p1", -1))
                .expectError(DatoInvalidoException.class)
                .verify();
        verify(repositorio, never()).guardar(any());
    }

    @Test
    void renombrarProducto_cambiaElNombre() {
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(franquicia));
        guardarDevuelveLoRecibido();

        StepVerifier.create(servicio.renombrarProducto("f1", "s1", "p1", "Manzana roja"))
                .assertNext(p -> assertThat(p.nombre()).isEqualTo("Manzana roja"))
                .verifyComplete();
    }

    // ---- consulta: producto con más stock por sucursal ----

    @Test
    void productosConMasStock_devuelveUnoPorSucursalIndicandoLaSucursal() {
        Sucursal norte = new Sucursal("s2", "Norte", List.of(new Producto("p3", "Uva", 5), new Producto("p4", "Kiwi", 40)));
        Sucursal vacia = new Sucursal("s3", "Sur", List.of()); // sin productos: no aparece
        Franquicia conTres = new Franquicia("f1", "Frutas SA", List.of(centro, norte, vacia), 0L);
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(conTres));

        StepVerifier.create(servicio.productosConMasStock("f1"))
                .assertNext(r -> {
                    assertThat(r.sucursalNombre()).isEqualTo("Centro");
                    assertThat(r.producto().nombre()).isEqualTo("Pera");
                    assertThat(r.producto().stock()).isEqualTo(25);
                })
                .assertNext(r -> {
                    assertThat(r.sucursalId()).isEqualTo("s2");
                    assertThat(r.producto().nombre()).isEqualTo("Kiwi");
                })
                .verifyComplete();
    }

    @Test
    void productosConMasStock_enCasoDeEmpate_ganaElPrimeroAgregado() {
        Sucursal empate = new Sucursal("s1", "Centro", List.of(new Producto("a", "A", 5), new Producto("b", "B", 5)));
        when(repositorio.buscarPorId("f1")).thenReturn(Mono.just(new Franquicia("f1", "F", List.of(empate), 0L)));

        StepVerifier.create(servicio.productosConMasStock("f1"))
                .assertNext(r -> assertThat(r.producto().nombre()).isEqualTo("A"))
                .verifyComplete();
    }

    @Test
    void productosConMasStock_franquiciaInexistente_dispara404() {
        when(repositorio.buscarPorId("no-existe")).thenReturn(Mono.empty());

        StepVerifier.create(servicio.productosConMasStock("no-existe"))
                .expectError(RecursoNoEncontradoException.class)
                .verify();
    }
}
