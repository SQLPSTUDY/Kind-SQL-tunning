package org.example.jpapaging.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "COUNTRIES", schema = "HR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Country {

    @Id
    @Column(name = "COUNTRY_ID", length = 2, nullable = false)
    private String countryId;

    @Column(name = "COUNTRY_NAME", length = 60)
    private String countryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REGION_ID")
    private Region region;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL)
    private List<Location> locations = new ArrayList<>();
}