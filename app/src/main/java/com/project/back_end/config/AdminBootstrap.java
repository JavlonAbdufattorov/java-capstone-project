package com.project.back_end.config;

import com.project.back_end.models.Admin;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.services.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optional first-run administrator provisioning; never replaces an existing password. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final AdminRepository admins;
    private final Service service;
    private final String username;
    private final String password;

    public AdminBootstrap(AdminRepository admins, Service service,
            @Value("${clinic.admin.username:}") String username,
            @Value("${clinic.admin.password:}") String password) {
        this.admins = admins;
        this.service = service;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) return;
        if (username.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Initial admin requires a username and password of at least 12 characters");
        }
        if (admins.findByUsername(username) == null) {
            Admin admin = new Admin();
            admin.setUsername(username);
            admin.setPassword(service.hashPassword(password));
            admins.save(admin);
        }
    }
}
