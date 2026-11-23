/**
 * @author mariapazmunoz version 2.0
 */
package peps.peps_back.repositories;

import peps.peps_back.domain.IoTModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IoTModuleRepository extends JpaRepository<IoTModule, Integer> {

    public Optional<IoTModule> findByIpAddress(String ipAddress);

    public List<IoTModule> findByStatusAndIsActive(String status, Boolean isActive);

}