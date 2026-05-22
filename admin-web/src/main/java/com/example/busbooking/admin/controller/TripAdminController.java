package com.example.busbooking.admin.controller;

import com.example.busbooking.admin.model.TripDto;
import com.example.busbooking.admin.model.TripForm;
import com.example.busbooking.admin.service.AdminDemoData;
import com.example.busbooking.admin.service.BusAdminService;
import com.example.busbooking.admin.service.RouteAdminService;
import com.example.busbooking.admin.service.TripAdminService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/trips")
public class TripAdminController {
    private final TripAdminService tripService;
    private final RouteAdminService routeService;
    private final BusAdminService busService;

    public TripAdminController(
            TripAdminService tripService,
            RouteAdminService routeService,
            BusAdminService busService
    ) {
        this.tripService = tripService;
        this.routeService = routeService;
        this.busService = busService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("demoMode", false);
        try {
            model.addAttribute("trips", tripService.findAll());
        } catch (IllegalStateException e) {
            model.addAttribute("trips", AdminDemoData.trips());
            model.addAttribute("demoMode", true);
            model.addAttribute("loadError", "Firestore load trips failed: " + rootMessage(e));
        }
        model.addAttribute("pageTitle", "Trips");
        return "trips/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        addFormLists(model);
        model.addAttribute("tripForm", new TripForm());
        model.addAttribute("mode", "create");
        model.addAttribute("pageTitle", "Create trip");
        return "trips/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute TripForm tripForm, BindingResult result, Model model) {
        if (result.hasErrors()) {
            addFormLists(model);
            model.addAttribute("mode", "create");
            return "trips/form";
        }
        tripService.create(tripForm);
        return "redirect:/trips";
    }

    @GetMapping("/{documentId}/edit")
    public String editForm(@PathVariable String documentId, Model model) {
        TripDto trip = tripService.findByDocumentId(documentId);
        TripForm form = new TripForm();
        form.setRouteId(trip.routeId());
        form.setBusId(trip.busId());
        form.setDepartureTime(trip.departureTime());
        form.setArrivalTime(trip.arrivalTime());
        form.setPrice(trip.price());
        form.setTripDate(trip.tripDate());
        form.setStatus(trip.status());
        addFormLists(model);
        model.addAttribute("trip", trip);
        model.addAttribute("seatViews", tripService.findSeatViews(documentId));
        model.addAttribute("tripForm", form);
        model.addAttribute("documentId", documentId);
        model.addAttribute("mode", "edit");
        model.addAttribute("pageTitle", "Edit trip");
        return "trips/form";
    }

    @PostMapping("/{documentId}")
    public String update(
            @PathVariable String documentId,
            @Valid @ModelAttribute TripForm tripForm,
            BindingResult result,
            Model model
    ) {
        if (result.hasErrors()) {
            addFormLists(model);
            model.addAttribute("mode", "edit");
            model.addAttribute("documentId", documentId);
            return "trips/form";
        }
        tripService.update(documentId, tripForm);
        return "redirect:/trips";
    }

    @PostMapping("/{documentId}/cancel")
    public String cancel(@PathVariable String documentId) {
        tripService.cancel(documentId);
        return "redirect:/trips";
    }

    private void addFormLists(Model model) {
        model.addAttribute("routes", routeService.findAll());
        model.addAttribute("buses", busService.findAll());
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? throwable.getMessage() : current.getMessage();
    }
}
