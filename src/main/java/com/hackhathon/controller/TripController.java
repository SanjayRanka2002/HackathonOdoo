package com.hackhathon.controller;

import com.hackhathon.entity.Driver;
import com.hackhathon.entity.Trip;
import com.hackhathon.entity.Vehicle;
import com.hackhathon.repository.DriverRepository;
import com.hackhathon.repository.TripRepository;
import com.hackhathon.repository.VehicleRepository;
import com.hackhathon.service.AuditLogService;
import com.hackhathon.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/trips")
public class TripController {

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public TripController(TripRepository tripRepository,
            VehicleRepository vehicleRepository,
            DriverRepository driverRepository,
            AuditLogService auditLogService,
            NotificationService notificationService) {
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String listTrips(Model model) {
        List<Trip> trips = tripRepository.findAll();
        model.addAttribute("trips", trips);
        return "trips/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("trip", new Trip());
        // Load available drivers and vehicles
        List<Vehicle> availableVehicles = vehicleRepository.findAll().stream()
                .filter(v -> "AVAILABLE".equalsIgnoreCase(v.getStatus()))
                .toList();
        List<Driver> availableDrivers = driverRepository.findAll().stream()
                .filter(d -> "AVAILABLE".equalsIgnoreCase(d.getStatus()))
                .toList();
        System.out.println("Available Vehicles: " + availableVehicles);
        model.addAttribute("vehicles", availableVehicles);
        model.addAttribute("drivers", availableDrivers);
        return "trips/form";
    }

    @PostMapping("/new")
    public String createTrip(@Valid @ModelAttribute("trip") Trip trip,
            BindingResult result,
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("driverId") Long driverId,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("vehicles", vehicleRepository.findAll());
            model.addAttribute("drivers", driverRepository.findAll());
            return "trips/form";
        }

        Optional<Vehicle> vOpt = vehicleRepository.findById(vehicleId);
        Optional<Driver> dOpt = driverRepository.findById(driverId);

        if (vOpt.isEmpty() || dOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Selected vehicle or driver not found");
            return "redirect:/trips/new";
        }

        Vehicle vehicle = vOpt.get();
        Driver driver = dOpt.get();

        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setDeparture(LocalDateTime.now());

        tripRepository.save(trip);

        // If trip is dispatched, mark driver/vehicle busy
        if ("DISPATCHED".equalsIgnoreCase(trip.getStatus())) {
            vehicle.setStatus("ON_TRIP");
            driver.setStatus("ON_TRIP");
            vehicleRepository.save(vehicle);
            driverRepository.save(driver);
        }

        auditLogService.log("TRIP_DISPATCH", "Dispatched trip " + trip.getId() + " from " + trip.getSource() + " to " + trip.getDestination() + " using vehicle " + vehicle.getVehicleName());
        notificationService.createNotification("Trip dispatched to " + trip.getDestination() + " (Driver: " + driver.getDriverName() + ")", "ADMIN");
        notificationService.createNotification("Trip dispatched to " + trip.getDestination() + " (Driver: " + driver.getDriverName() + ")", "DISPATCHER");

        redirectAttributes.addFlashAttribute("successMessage", "Trip dispatched successfully!");
        return "redirect:/trips";
    }

    @GetMapping("/complete/{id}")
    public String completeTrip(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<Trip> opt = tripRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trip not found");
            return "redirect:/trips";
        }

        Trip trip = opt.get();
        if (!"DISPATCHED".equalsIgnoreCase(trip.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only dispatched trips can be completed");
            return "redirect:/trips";
        }

        trip.setStatus("COMPLETED");
        trip.setArrival(LocalDateTime.now());
        tripRepository.save(trip);
        auditLogService.log("TRIP_COMPLETE", "Completed trip " + id + " to " + trip.getDestination());
        notificationService.createNotification("Trip completed: TRIP-" + id + " has arrived at " + trip.getDestination(), "ADMIN");

        // Free vehicle and driver
        Vehicle vehicle = trip.getVehicle();
        if (vehicle != null) {
            vehicle.setStatus("AVAILABLE");
            // update vehicle mileage
            if (trip.getDistance() != null) {
                vehicle.setCurrentOdometer(vehicle.getCurrentOdometer() + trip.getDistance());
            }
            vehicleRepository.save(vehicle);
        }

        Driver driver = trip.getDriver();
        if (driver != null) {
            driver.setStatus("AVAILABLE");
            driverRepository.save(driver);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Trip completed successfully! Vehicle odometer updated.");
        return "redirect:/trips";
    }

    @GetMapping("/cancel/{id}")
    public String cancelTrip(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Optional<Trip> opt = tripRepository.findById(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trip not found");
            return "redirect:/trips";
        }

        Trip trip = opt.get();
        if ("COMPLETED".equalsIgnoreCase(trip.getStatus()) || "CANCELLED".equalsIgnoreCase(trip.getStatus())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Completed or already cancelled trips cannot be modified");
            return "redirect:/trips";
        }

        trip.setStatus("CANCELLED");
        tripRepository.save(trip);
        auditLogService.log("TRIP_CANCEL", "Cancelled trip " + id);
        notificationService.createNotification("Trip cancelled: TRIP-" + id, "ADMIN");

        // Free vehicle and driver
        Vehicle vehicle = trip.getVehicle();
        if (vehicle != null) {
            vehicle.setStatus("AVAILABLE");
            vehicleRepository.save(vehicle);
        }

        Driver driver = trip.getDriver();
        if (driver != null) {
            driver.setStatus("AVAILABLE");
            driverRepository.save(driver);
        }

        redirectAttributes.addFlashAttribute("successMessage", "Trip cancelled successfully.");
        return "redirect:/trips";
    }

    @GetMapping("/delete/{id}")
    public String deleteTrip(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        tripRepository.deleteById(id);
        auditLogService.log("TRIP_DELETE", "Deleted trip " + id);
        redirectAttributes.addFlashAttribute("successMessage", "Trip deleted successfully!");
        return "redirect:/trips";
    }
}
