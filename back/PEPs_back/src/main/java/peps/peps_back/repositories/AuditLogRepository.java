package peps.peps_back.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import peps.peps_back.domain.AuditLog;

import java.util.List;

/**
 * @author mariapazmunoz version 2.0
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    public List<AuditLog> findByUser_UserId(Integer userId);
}
