package com.hackhathon.repository;

import com.hackhathon.entity.VehicleDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleDocumentRepository extends JpaRepository<VehicleDocument, Long> {
    
    @Query("SELECT d FROM VehicleDocument d WHERE d.vehicle.id = :vehicleId")
    List<VehicleDocument> findByVehicleId(@Param("vehicleId") Long vehicleId);
}
