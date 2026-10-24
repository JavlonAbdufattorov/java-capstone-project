package com.project.back_end.controllers;
import com.project.back_end.services.*;
import com.project.back_end.services.Service;
import com.project.back_end.models.*;
import com.project.back_end.DTO.Login;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("${api.path}admin")
public class AdminController {
    private final Service service;
    public AdminController(Service service) { this.service = service; }
    @PostMapping({"", "/login"})
    public ResponseEntity<Map<String, Object>> adminLogin(@Valid @RequestBody Admin admin) { return service.validateAdmin(admin); }
}
