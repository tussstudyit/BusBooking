package com.example.busbooking.admin.controller;

import com.example.busbooking.admin.service.TicketAdminService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TicketAdminController {
    private final TicketAdminService ticketService;

    public TicketAdminController(TicketAdminService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/tickets")
    public String list(Model model) {
        try {
            model.addAttribute("tickets", ticketService.findAll());
        } catch (IllegalStateException e) {
            model.addAttribute("tickets", List.of());
            model.addAttribute("loadError", "Không tải được dữ liệu vé. Firestore đang hết quota hoặc tạm thời không phản hồi.");
        }
        model.addAttribute("pageTitle", "Tickets");
        return "tickets/list";
    }
}
