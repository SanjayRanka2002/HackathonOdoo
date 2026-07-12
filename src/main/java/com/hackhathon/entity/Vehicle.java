package com.hackhathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Column(nullable = false)
    private String vehicleName;

    @Column(nullable = false)
    private String vehicleType;

    private String vehicleModel;

    @Column(nullable = false)
    private Double loadCapacity;

    private Double purchaseCost;

    @Column(nullable = false)
    @Builder.Default
    private Double currentOdometer = 0.0;

    @Column(nullable = false)
    private String status; // AVAILABLE, ON_TRIP, IN_SHOP, RETIRED

    private String imagePath;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Trip> trips = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Maintenance> maintenanceLogs = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    private List<FuelLog> fuelLogs = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Expense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL)
    @Builder.Default
    private List<VehicleDocument> documents = new ArrayList<>();
}
