package at.qe.skeleton.mappers;

/**
 * DTOMapper Interface. Provides common base methods for all mappers.
 *
 * This class is part of the skeleton project provided for students of the
* course "Software Engineering" offered by Innsbruck University.
 * 
 * @param <E> the entity a DTO is mapped from
 * @param <D> the DTO an entity is mapped to
 */
public interface DTOMapper<E, D> {
    
    D mapTo(E entity);
    E mapFrom(D dto);
}
