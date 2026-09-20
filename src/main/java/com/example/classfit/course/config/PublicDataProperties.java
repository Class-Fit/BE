package com.example.classfit.course.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "public-data")
public class PublicDataProperties {

    private String facilityServiceKey;
    private String courseServiceKey;
    private String facilityUrl;
    private String courseUrl;
    private String gangwonCityCode = "42";
    private int pageSize = 1000;

    public void validateForSync() {
        if (isBlank(facilityServiceKey) || isBlank(courseServiceKey)
                || isBlank(facilityUrl) || isBlank(courseUrl)) {
            throw new IllegalStateException(
                    "시설·강좌 API 키와 URL을 application-local.yaml에 설정해야 합니다."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
