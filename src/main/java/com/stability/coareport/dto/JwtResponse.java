package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String username;
    private String email;
    private String role;
    private Long branchId;
    private String branchName;
    private Long companyId;
    private String companyName;

    public JwtResponse(String token, Long id, String username, String email, String role, Long branchId, String branchName, Long companyId, String companyName) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.branchId = branchId;
        this.branchName = branchName;
        this.companyId = companyId;
        this.companyName = companyName;
    }
}
