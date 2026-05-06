package org.example.tandem.repository;

import org.example.tandem.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    @Query("SELECT DISTINCT r FROM Role r LEFT JOIN FETCH r.permissions")
    Set<Role> findAllWithPermissions();

    Set<Role> findByNameIn(Set<String> names);
}
