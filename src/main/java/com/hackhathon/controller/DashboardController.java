package com.hackhathon.controller;

import com.hackhathon.entity.FuelLog;
import com.hackhathon.entity.Expense;
import com.hackhathon.entity.Maintenance;
import com.hackhathon.entity.Trip;
import com.hackhathon.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final TripRepository tripRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final FuelLogRepository fuelLogRepository;
    private final ExpenseRepository expenseRepository;

    public DashboardController(VehicleRepository vehicleRepository,
                               DriverRepository driverRepository,
                               TripRepository tripRepository,
                               MaintenanceRepository maintenanceRepository,
                               FuelLogRepository fuelLogRepository,
                               ExpenseRepository expenseRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.tripRepository = tripRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.fuelLogRepository = fuelLogRepository;
        this.expenseRepository = expenseRepository;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        // Vehicle Stats
        long totalVehicles = vehicleRepository.count();
        long activeVehicles = vehicleRepository.findAll().stream().filter(v -> "ON_TRIP".equalsIgnoreCase(v.getStatus())).count();
        long availableVehicles = vehicleRepository.findAll().stream().filter(v -> "AVAILABLE".equalsIgnoreCase(v.getStatus())).count();
        long inShopVehicles = vehicleRepository.findAll().stream().filter(v -> "IN_SHOP".equalsIgnoreCase(v.getStatus())).count();

        // Driver Stats
        long totalDrivers = driverRepository.count();
        long availableDrivers = driverRepository.findAll().stream().filter(d -> "AVAILABLE".equalsIgnoreCase(d.getStatus())).count();
        long activeDrivers = driverRepository.findAll().stream().filter(d -> "ON_TRIP".equalsIgnoreCase(d.getStatus())).count();

        // Trip Stats
        long activeTrips = tripRepository.findAll().stream().filter(t -> "DISPATCHED".equalsIgnoreCase(t.getStatus())).count();
        long completedTrips = tripRepository.findAll().stream().filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus())).count();

        // Maintenance Stats
        long pendingMaintenance = maintenanceRepository.findAll().stream().filter(m -> !"COMPLETED".equalsIgnoreCase(m.getStatus())).count();

        // Financial Calculations
        double fuelCost = fuelLogRepository.findAll().stream().mapToDouble(FuelLog::getCost).sum();
        double helperExpenses = expenseRepository.findAll().stream().mapToDouble(Expense::getAmount).sum();
        double maintenanceCost = maintenanceRepository.findAll().stream()
                .filter(m -> m.getCost() != null)
                .mapToDouble(Maintenance::getCost).sum();

        double totalOperatingCost = fuelCost + helperExpenses + maintenanceCost;

        // Calculate Revenue from Trips: distance * 5 + weight * 20
        double totalRevenue = tripRepository.findAll().stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.getStatus()))
                .mapToDouble(t -> {
                    double dist = t.getDistance() != null ? t.getDistance() : 0.0;
                    double weight = t.getCargoWeight() != null ? t.getCargoWeight() : 0.0;
                    return (dist * 4.5) + (weight * 12.0);
                }).sum();

        double netProfit = totalRevenue - totalOperatingCost;

        // Add attributes to Model
        model.addAttribute("totalVehicles", totalVehicles);
        model.addAttribute("activeVehicles", activeVehicles);
        model.addAttribute("availableVehicles", availableVehicles);
        model.addAttribute("inShopVehicles", inShopVehicles);
        model.addAttribute("totalDrivers", totalDrivers);
        model.addAttribute("availableDrivers", availableDrivers);
        model.addAttribute("activeDrivers", activeDrivers);
        model.addAttribute("activeTrips", activeTrips);
        model.addAttribute("completedTrips", completedTrips);
        model.addAttribute("pendingMaintenance", pendingMaintenance);
        model.addAttribute("fuelCost", fuelCost);
        model.addAttribute("maintenanceCost", maintenanceCost);
        model.addAttribute("otherCost", helperExpenses);
        model.addAttribute("totalOperatingCost", totalOperatingCost);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("netProfit", netProfit);

        // Recent Trips list
        List<Trip> recentTrips = tripRepository.findAll().stream()
                .sorted((t1, t2) -> {
                    if (t1.getDeparture() == null) return 1;
                    if (t2.getDeparture() == null) return -1;
                    return t2.getDeparture().compareTo(t1.getDeparture());
                })
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentTrips", recentTrips);

        // Recent Maintenance list
        List<Maintenance> recentMaintenance = maintenanceRepository.findAll().stream()
                .sorted((m1, m2) -> {
                    if (m1.getStartDate() == null) return 1;
                    if (m2.getStartDate() == null) return -1;
                    return m2.getStartDate().compareTo(m1.getStartDate());
                })
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentMaintenance", recentMaintenance);

        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
