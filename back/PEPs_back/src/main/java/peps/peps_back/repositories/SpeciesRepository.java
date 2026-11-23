package peps.peps_back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import peps.peps_back.domain.Species;
import peps.peps_back.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author mariapazmunoz version 2.0
 */

@Repository
public interface SpeciesRepository extends JpaRepository<User, Integer> {
    public Optional<Species> findBySpeciesName(String speciesName);

}
