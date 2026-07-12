package com.hackhathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fuel_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fuelDate;

    @Column(nullable = false)
    private Double quantity; // In Liters/Gallons

    @Column(nullable = false)
    private Double cost;

    @Column(nullable = false)
    private Double odometerReading;

    @Column(nullable = false)
    private Double pricePerLiter;

    private String fuelStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;
}
