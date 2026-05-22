package com.example.busbooking.admin.controller;

import com.example.busbooking.admin.service.UserAdminService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserAdminController {
    private final UserAdminService userService;

    public UserAdminController(UserAdminService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String list(@RequestParam(defaultValue = "") String q, Model model) {
        try {
            model.addAttribute("users", userService.findAll(q));
        } catch (IllegalStateException e) {
            model.addAttribute("users", List.of());
            model.addAttribute("loadError", "Không tải được dữ liệu người dùng. Firestore đang hết quota hoặc tạm thời không phản hồi.");
        }
        model.addAttribute("q", q);
        model.addAttribute("pageTitle", "Users");
        return "users/list";
    }

    @PostMapping("/users/{uid}/blocked/{blocked}")
    public String setBlocked(@PathVariable String uid, @PathVariable boolean blocked) {
        userService.setBlocked(uid, blocked);
        return "redirect:/users";
    }
}
