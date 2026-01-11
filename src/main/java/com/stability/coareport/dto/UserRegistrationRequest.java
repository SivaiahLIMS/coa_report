package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationRequest {
    private String empId;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String mobileNumber;
    private String department;
    private String role;
    private Long branchId;
}
