package peps.peps_back.repositories;

import peps.peps_back.domain.Sound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author mariapazmunoz version 2.0
 */

@Repository
public interface SoundRepository extends JpaRepository<Sound, Integer> {

    public Optional<Sound> findByMinioKey(String MinioKey);

    public List<Sound> findBySoundType(String soundType);

}
