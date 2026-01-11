package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BranchRegistrationRequest {
    private String name;
    private String code;
    private String location;
    private String contactEmail;
    private String contactPhone;
    private Long companyId;
}
