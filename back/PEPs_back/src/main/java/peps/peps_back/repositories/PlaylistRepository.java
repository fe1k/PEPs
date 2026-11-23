package peps.peps_back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.Playlist;
import peps.peps_back.domain.User;

import java.util.Optional;

/**
 * @author mariapazmunoz version 2.0
 */

@Repository
public interface PlaylistRepository extends JpaRepository<User, Integer>{

    public Optional<Playlist> findByName(String name);

}
