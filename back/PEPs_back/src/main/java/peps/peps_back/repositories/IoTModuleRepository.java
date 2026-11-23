package peps.peps_back.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.IoTModule;

/**
 * Spring Data JPA repository for {@link IoTModule} entities, mapping to the {@code iot_module} table.
 * <p>
 * Inherits standard CRUD operations from {@link JpaRepository}. Query methods declared
 * here are implemented automatically by Spring Data from their names at runtime.
 *
 * @author María Paz Muñoz
 * @version 2.0
 */
@Repository
public interface IoTModuleRepository extends JpaRepository<IoTModule, Integer> {

    /**
     * Retrieves an IoT module by its network IP address.
     *
     * @param ipAddress the IP address of the module to find
     * @return an {@link Optional} containing the matching module, or empty if none exists
     */
    public Optional<IoTModule> findByIpAddress(String ipAddress);

    /**
     * Retrieves all IoT modules matching the given operational status and activation flag.
     *
     * @param status   the operational status to filter by (e.g., {@code "ONLINE"}, {@code "OFFLINE"})
     * @param isActive {@code true} to return enabled modules, {@code false} for disabled ones
     * @return the list of matching modules; empty if none match
     */
    public List<IoTModule> findByStatusAndIsActive(String status, Boolean isActive);
}
