package com.franquicias.infrastructure.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/** Spring Data genera la implementación (como un Manager de Django): save, findById, findAll... y la consulta derivada del nombre. */
public interface FranquiciaMongoRepository extends ReactiveCrudRepository<FranquiciaDocument, String> {

    Mono<FranquiciaDocument> findFirstByNombreIgnoreCase(String nombre);
}
