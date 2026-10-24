package com.project.back_end.mvc;
import com.project.back_end.services.Service;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
@Controller
public class DashboardController {
    private final Service service;
    public DashboardController(Service service) { this.service = service; }
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        return service.validateToken(token, "admin").isEmpty()
            ? "admin/adminDashboard" : "redirect:http://localhost:8080";
    }
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        return service.validateToken(token, "doctor").isEmpty()
            ? "doctor/doctorDashboard" : "redirect:http://localhost:8080";
    }
}
