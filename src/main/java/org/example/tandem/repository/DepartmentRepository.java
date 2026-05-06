package org.example.tandem.repository;

import org.example.tandem.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.manager")
    Set<Department> findAllWithManagers();
}
