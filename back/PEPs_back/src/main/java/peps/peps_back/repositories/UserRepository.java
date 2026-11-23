package peps.peps_back.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.User;

/**
 * Spring Data JPA repository for {@link User} entities, mapping to the {@code users} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Retrieves a user by its unique login.
     *
     * @param login the login of the user to find
     * @return an {@link Optional} containing the matching user, or empty if none exists
     */
    public Optional<User> findByLogin(String login);

    /**
     * Checks whether a user with the given login already exists.
     *
     * @param login the login to check
     * @return {@code true} if a user with this login exists, {@code false} otherwise
     */
    public boolean existsByLogin(String login);
}
