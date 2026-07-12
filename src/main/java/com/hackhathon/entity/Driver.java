package com.hackhathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String driverName;

    @Column(nullable = false, unique = true)
    private String licenseNumber;

    @Column(nullable = false)
    private String licenseCategory;

    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false)
    private String phone;

    private String email;
    private String address;
    private String emergencyContact;

    @Builder.Default
    private Integer safetyScore = 100;

    @Column(nullable = false)
    private String status; // AVAILABLE, ON_TRIP, SUSPENDED

    private String licenseImagePath;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Trip> trips = new ArrayList<>();

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL)
    @Builder.Default
    private List<DriverDocument> documents = new ArrayList<>();
}
