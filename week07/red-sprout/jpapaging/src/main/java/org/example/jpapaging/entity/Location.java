package org.example.jpapaging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "LOCATIONS", schema = "HR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @Column(name = "LOCATION_ID", nullable = false)
    private Long locationId;

    @Column(name = "STREET_ADDRESS", length = 40)
    private String streetAddress;

    @Column(name = "POSTAL_CODE", length = 12)
    private String postalCode;

    @Column(name = "CITY", length = 30, nullable = false)
    private String city;

    @Column(name = "STATE_PROVINCE", length = 25)
    private String stateProvince;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUNTRY_ID")
    private Country country;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL)
    private List<Department> departments = new ArrayList<>();
}