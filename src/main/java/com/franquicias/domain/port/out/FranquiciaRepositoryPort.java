package com.franquicias.domain.port.out;

import com.franquicias.domain.model.Franquicia;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Puerto de salida: lo que el dominio necesita de la persistencia (como una interfaz de repositorio/ORM). */
public interface FranquiciaRepositoryPort {

    /** Inserta o actualiza la franquicia completa y devuelve la versión guardada. */
    Mono<Franquicia> guardar(Franquicia franquicia);

    Mono<Franquicia> buscarPorId(String id);

    /** Busca ignorando mayúsculas; vacío si no hay ninguna con ese nombre. */
    Mono<Franquicia> buscarPorNombre(String nombre);

    Flux<Franquicia> buscarTodas();
}
