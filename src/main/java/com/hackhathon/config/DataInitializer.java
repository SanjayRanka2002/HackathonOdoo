package com.hackhathon.config;

import com.hackhathon.entity.*;
import com.hackhathon.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final FuelLogRepository fuelLogRepository;
    private final ExpenseRepository expenseRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           VehicleRepository vehicleRepository,
                           DriverRepository driverRepository,
                           TripRepository tripRepository,
                           MaintenanceRepository maintenanceRepository,
                           FuelLogRepository fuelLogRepository,
                           ExpenseRepository expenseRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.expenseRepository = expenseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.count() == 0) {
            // Seed Roles
            Role adminRole = new Role(null, "ADMIN", null);
            Role fleetManager = new Role(null, "FLEET_MANAGER", null);
            Role dispatcher = new Role(null, "DISPATCHER", null);
            Role financeRole = new Role(null, "FINANCIAL_ANALYST", null);
            
            roleRepository.saveAll(Arrays.asList(adminRole, fleetManager, dispatcher, financeRole));

            // Seed Users for all roles
            User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .firstName("System")
                .lastName("Admin")
                .email("admin@transitops.com")
                .role(adminRole)
                .active(true)
                .build();

            User manager = User.builder()
                .username("manager")
                .password(passwordEncoder.encode("manager"))
                .firstName("Fleet")
                .lastName("Manager")
                .email("manager@transitops.com")
                .role(fleetManager)
                .active(true)
                .build();

            User dispatch = User.builder()
                .username("dispatcher")
                .password(passwordEncoder.encode("dispatcher"))
                .firstName("Dispatch")
                .lastName("Officer")
                .email("dispatcher@transitops.com")
                .role(dispatcher)
                .active(true)
                .build();

            User finance = User.builder()
                .username("finance")
                .password(passwordEncoder.encode("finance"))
                .firstName("Finance")
                .lastName("Analyst")
                .email("finance@transitops.com")
                .role(financeRole)
                .active(true)
                .build();
            
            userRepository.saveAll(Arrays.asList(admin, manager, dispatch, finance));

            // Seed Vehicles
            Vehicle v1 = Vehicle.builder()
                .vehicleName("Volvo FH16")
                .vehicleType("Heavy Truck")
                .vehicleModel("2023 FH16 Globetrotter")
                .registrationNumber("KA-01-ME-1234")
                .loadCapacity(25.0)
                .purchaseCost(135000.0)
                .currentOdometer(120500.0)
                .status("AVAILABLE")
                .build();

            Vehicle v2 = Vehicle.builder()
                .vehicleName("Scania R500")
                .vehicleType("Heavy Truck")
                .vehicleModel("2022 Streamline")
                .registrationNumber("KA-01-ME-5678")
                .loadCapacity(22.0)
                .purchaseCost(128000.0)
                .currentOdometer(85200.0)
                .status("ON_TRIP")
                .build();

            Vehicle v3 = Vehicle.builder()
                .vehicleName("Mercedes Actros")
                .vehicleType("Cargo Truck")
                .vehicleModel("2021 Benz Actros")
                .registrationNumber("KA-01-ME-9012")
                .loadCapacity(18.0)
                .purchaseCost(115000.0)
                .currentOdometer(142100.0)
                .status("IN_SHOP")
                .build();

            Vehicle v4 = Vehicle.builder()
                .vehicleName("Isuzu NPR")
                .vehicleType("Medium Duty")
                .vehicleModel("2020 Reward NPR")
                .registrationNumber("KA-01-ME-3456")
                .loadCapacity(7.5)
                .purchaseCost(48000.0)
                .currentOdometer(45000.0)
                .status("AVAILABLE")
                .build();

            vehicleRepository.saveAll(Arrays.asList(v1, v2, v3, v4));

            // Seed Drivers
            Driver d1 = Driver.builder()
                .driverName("John Doe")
                .licenseNumber("DL-12345SHARED")
                .licenseCategory("Heavy Commercial")
                .expiryDate(LocalDate.now().plusYears(2))
                .phone("+91 9876543210")
                .email("john@transitops.com")
                .address("123 City Center St, Bangalore")
                .emergencyContact("Jane Doe (+91 9876543211)")
                .safetyScore(92)
                .status("AVAILABLE")
                .build();

            Driver d2 = Driver.builder()
                .driverName("David Miller")
                .licenseNumber("DL-56789SHARED")
                .licenseCategory("Heavy Commercial")
                .expiryDate(LocalDate.now().plusYears(3))
                .phone("+91 9876543220")
                .email("david@transitops.com")
                .address("456 Green Wood Rd, Bangalore")
                .emergencyContact("Mary Miller (+91 9876543221)")
                .safetyScore(88)
                .status("ON_TRIP")
                .build();

            Driver d3 = Driver.builder()
                .driverName("Sarah Connor")
                .licenseNumber("DL-90123SHARED")
                .licenseCategory("Medium Commercial")
                .expiryDate(LocalDate.now().plusYears(1))
                .phone("+91 9876543230")
                .email("sarah@transitops.com")
                .address("789 Skynet Ave, Bangalore")
                .emergencyContact("John Connor (+91 9876543231)")
                .safetyScore(98)
                .status("AVAILABLE")
                .build();

            driverRepository.saveAll(Arrays.asList(d1, d2, d3));

            // Seed Trips
            Trip trip1 = Trip.builder()
                .source("Warehouse A (Bangalore)")
                .destination("Warehouse B (Chennai)")
                .cargoWeight(15.5)
                .distance(350.0)
                .departure(LocalDateTime.now().minusHours(4))
                .status("DISPATCHED")
                .vehicle(v2)
                .driver(d2)
                .build();

            Trip trip2 = Trip.builder()
                .source("Port Terminal (Chennai)")
                .destination("Distribution Hub (Bangalore)")
                .cargoWeight(24.0)
                .distance(480.0)
                .departure(LocalDateTime.now().minusDays(2))
                .arrival(LocalDateTime.now().minusDays(2).plusHours(8))
                .status("COMPLETED")
                .vehicle(v1)
                .driver(d1)
                .build();

            tripRepository.saveAll(Arrays.asList(trip1, trip2));

            // Seed Maintenance Logs
            Maintenance maintenance = Maintenance.builder()
                .startDate(LocalDateTime.now().minusDays(1))
                .description("Engine cylinder overhaul, fuel filter replacement")
                .cost(1250.0)
                .status("IN_PROGRESS")
                .vehicle(v3)
                .build();

            maintenanceRepository.save(maintenance);

            // Seed Fuel Logs
            FuelLog fuelLog1 = FuelLog.builder()
                .fuelDate(LocalDateTime.now().minusDays(2))
                .quantity(180.0)
                .cost(250.0)
                .odometerReading(120020.0)
                .pricePerLiter(1.39)
                .fuelStation("Chevron Station Hub 1")
                .vehicle(v1)
                .build();

            FuelLog fuelLog2 = FuelLog.builder()
                .fuelDate(LocalDateTime.now().minusDays(1))
                .quantity(150.0)
                .cost(210.0)
                .odometerReading(85050.0)
                .pricePerLiter(1.40)
                .fuelStation("Shell Highway Terminal")
                .vehicle(v2)
                .build();

            fuelLogRepository.saveAll(Arrays.asList(fuelLog1, fuelLog2));

            // Seed Expenses
            Expense expense1 = Expense.builder()
                .expenseDate(LocalDate.now().minusDays(2))
                .category("TOLL")
                .amount(45.0)
                .description("National Highway NH44 Toll Fee")
                .vehicle(v1)
                .build();

            Expense expense2 = Expense.builder()
                .expenseDate(LocalDate.now().minusDays(1))
                .category("PARKING")
                .amount(20.0)
                .description("Port Cargo Parking Fee")
                .vehicle(v2)
                .build();

            expenseRepository.saveAll(Arrays.asList(expense1, expense2));
        }
    }
}
