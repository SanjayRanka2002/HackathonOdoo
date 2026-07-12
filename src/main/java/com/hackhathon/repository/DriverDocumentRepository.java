package com.hackhathon.repository;

import com.hackhathon.entity.DriverDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverDocumentRepository extends JpaRepository<DriverDocument, Long> {
    
    @Query("SELECT d FROM DriverDocument d WHERE d.driver.id = :driverId")
    List<DriverDocument> findByDriverId(@Param("driverId") Long driverId);
}
