package com.example.FoodSave.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDto {
    private String username;
    private String email;
    private String phoneNumber;
    private String password;
    private String city;
}
