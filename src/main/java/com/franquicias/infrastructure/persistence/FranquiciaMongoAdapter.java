package com.franquicias.infrastructure.persistence;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.out.FranquiciaRepositoryPort;
import com.franquicias.infrastructure.persistence.FranquiciaDocument.ProductoDocument;
import com.franquicias.infrastructure.persistence.FranquiciaDocument.SucursalDocument;
import java.util.List;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Adaptador de salida: implementa el puerto del dominio usando MongoDB y traduce documento <-> modelo. */
@Component
public class FranquiciaMongoAdapter implements FranquiciaRepositoryPort {

    private final FranquiciaMongoRepository repositorio;

    public FranquiciaMongoAdapter(FranquiciaMongoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Mono<Franquicia> guardar(Franquicia franquicia) {
        return repositorio.save(aDocumento(franquicia)).map(FranquiciaMongoAdapter::aDominio);
    }

    @Override
    public Mono<Franquicia> buscarPorId(String id) {
        return repositorio.findById(id).map(FranquiciaMongoAdapter::aDominio);
    }

    @Override
    public Mono<Franquicia> buscarPorNombre(String nombre) {
        return repositorio.findFirstByNombreIgnoreCase(nombre).map(FranquiciaMongoAdapter::aDominio);
    }

    @Override
    public Flux<Franquicia> buscarTodas() {
        return repositorio.findAll().map(FranquiciaMongoAdapter::aDominio);
    }

    // ---- mapeos (los "serializers" entre dominio y documento) ----

    static FranquiciaDocument aDocumento(Franquicia f) {
        return new FranquiciaDocument(f.id(), f.nombre(),
                f.sucursales().stream().map(FranquiciaMongoAdapter::aDocumento).toList(), f.version());
    }

    private static SucursalDocument aDocumento(Sucursal s) {
        return new SucursalDocument(s.id(), s.nombre(),
                s.productos().stream().map(p -> new ProductoDocument(p.id(), p.nombre(), p.stock())).toList());
    }

    static Franquicia aDominio(FranquiciaDocument d) {
        var sucursales = d.sucursales() == null ? List.<Sucursal>of()
                : d.sucursales().stream().map(FranquiciaMongoAdapter::aDominio).toList();
        return new Franquicia(d.id(), d.nombre(), sucursales, d.version());
    }

    private static Sucursal aDominio(SucursalDocument d) {
        var productos = d.productos() == null ? List.<Producto>of()
                : d.productos().stream().map(p -> new Producto(p.id(), p.nombre(), p.stock())).toList();
        return new Sucursal(d.id(), d.nombre(), productos);
    }
}
