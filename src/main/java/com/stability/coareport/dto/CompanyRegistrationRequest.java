package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRegistrationRequest {
    private String name;
    private String code;
    private String address;
    private String contactEmail;
    private String contactPhone;
}
