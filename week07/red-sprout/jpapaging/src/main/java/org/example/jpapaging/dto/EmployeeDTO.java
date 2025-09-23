package org.example.jpapaging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private String firstName;
    private String lastName;
    private String departmentName;
    private String streetAddress;
    private String city;
    private String countryName;
    private String regionName;
}