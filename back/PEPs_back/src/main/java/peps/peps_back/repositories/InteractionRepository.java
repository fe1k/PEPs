/**
 * @author mariapazmunoz version 2.0
  */
package peps.peps_back.repositories;

import peps.peps_back.domain.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Integer> {

    List<Interaction> findByModule_ModuleIdAndTriggeredAtBetween(Integer moduleId, java.util.Date start, java.util.Date end);

    List<Interaction> findByModule_Species_SpeciesId(Integer speciesId);
}