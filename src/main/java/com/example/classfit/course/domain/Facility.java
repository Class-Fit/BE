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

    public void update(PublicFacilityItem item) {
        this.businessRegistrationNumber = item.businessRegistrationNumber();
        this.facilitySerialNumber = item.facilitySerialNumber();
        this.name = item.facilityName();
        this.cityCode = item.cityCode();
        this.cityName = item.cityName();
        this.localCode = item.localCode();
        this.localName = item.localName();
        this.roadAddress = item.roadAddress();
        this.detailAddress = item.detailAddress();
        this.zipCode = item.zipCode();
        this.mainSportCode = item.mainSportCode();
        this.mainSportName = item.mainSportName();
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
}
