package com.example.ecombackend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String role;
    private boolean enabled;
    private LocalDateTime createdDate;
}
