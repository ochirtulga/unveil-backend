// src/main/java/com/unveil/dto/VerificationTokenDto.java
package com.unveil.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationTokenDto {
    private String token;
    private String email;
    private LocalDateTime expiresAt;
    private boolean verified;
}