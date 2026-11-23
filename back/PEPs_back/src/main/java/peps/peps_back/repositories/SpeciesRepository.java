package peps.peps_back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import peps.peps_back.domain.Species;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author mariapazmunoz version 2.0
 */

@Repository
public interface SpeciesRepository extends JpaRepository<Species, Integer> {
    // The entity field is "name", not "speciesName": explicit JPQL keeps the diagram's method name.
    @Query("SELECT s FROM Species s WHERE s.name = :speciesName")
    public Optional<Species> findBySpeciesName(@Param("speciesName") String speciesName);

}
