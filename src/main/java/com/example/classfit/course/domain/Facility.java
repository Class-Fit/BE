package com.example.classfit.course.domain;

import com.example.classfit.course.dto.PublicFacilityItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "facilities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_facilities_brno_facil_sn",
                columnNames = {"brno", "facil_sn"}
        )
)
public class Facility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brno", nullable = false, length = 20)
    private String businessRegistrationNumber;

    @Column(name = "facil_sn", nullable = false, length = 30)
    private String facilitySerialNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "city_cd", length = 10)
    private String cityCode;

    @Column(name = "city_nm")
    private String cityName;

    @Column(name = "local_cd", length = 20)
    private String localCode;

    @Column(name = "local_nm")
    private String localName;

    @Column(name = "road_addr")
    private String roadAddress;

    @Column(name = "detail_addr")
    private String detailAddress;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "main_event_cd", length = 20)
    private String mainSportCode;

    @Column(name = "main_event_nm")
    private String mainSportName;

    private Facility(PublicFacilityItem item) {
        update(item);
    }

    public static Facility from(PublicFacilityItem item) {
        return new Facility(item);
    }

    public boolean update(PublicFacilityItem item) {
        String businessRegistrationNumber = normalize(item.businessRegistrationNumber());
        String facilitySerialNumber = normalize(item.facilitySerialNumber());
        String name = normalize(item.facilityName());
        String cityCode = normalize(item.cityCode());
        String cityName = normalize(item.cityName());
        String localCode = normalize(item.localCode());
        String localName = normalize(item.localName());
        String roadAddress = normalize(item.roadAddress());
        String detailAddress = normalize(item.detailAddress());
        String zipCode = normalize(item.zipCode());
        String mainSportCode = normalize(item.mainSportCode());
        String mainSportName = normalize(item.mainSportName());

        boolean changed = !Objects.equals(this.businessRegistrationNumber, businessRegistrationNumber)
                || !Objects.equals(this.facilitySerialNumber, facilitySerialNumber)
                || !Objects.equals(this.name, name)
                || !Objects.equals(this.cityCode, cityCode)
                || !Objects.equals(this.cityName, cityName)
                || !Objects.equals(this.localCode, localCode)
                || !Objects.equals(this.localName, localName)
                || !Objects.equals(this.roadAddress, roadAddress)
                || !Objects.equals(this.detailAddress, detailAddress)
                || !Objects.equals(this.zipCode, zipCode)
                || !Objects.equals(this.mainSportCode, mainSportCode)
                || !Objects.equals(this.mainSportName, mainSportName);

        if (changed) {
            this.businessRegistrationNumber = businessRegistrationNumber;
            this.facilitySerialNumber = facilitySerialNumber;
            this.name = name;
            this.cityCode = cityCode;
            this.cityName = cityName;
            this.localCode = localCode;
            this.localName = localName;
            this.roadAddress = roadAddress;
            this.detailAddress = detailAddress;
            this.zipCode = zipCode;
            this.mainSportCode = mainSportCode;
            this.mainSportName = mainSportName;
        }
        return changed;
    }

    public String fullAddress() {
        if (detailAddress == null || detailAddress.isBlank()) {
            return roadAddress;
        }
        if (roadAddress == null || roadAddress.isBlank()) {
            return detailAddress;
        }
        return roadAddress + " " + detailAddress;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
