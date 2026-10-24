package com.project.back_end.DTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
public class Login {
    @NotBlank @Email private String email;
    @NotBlank private String password;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
