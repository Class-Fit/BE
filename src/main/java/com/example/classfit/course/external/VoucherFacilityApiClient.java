package com.example.classfit.course.external;

import com.example.classfit.course.config.PublicDataProperties;
import com.example.classfit.course.dto.PublicFacilityItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class VoucherFacilityApiClient {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;
    private final PublicDataProperties properties;

    public VoucherFacilityApiClient(ObjectMapper objectMapper, PublicDataProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public PublicDataPage<PublicFacilityItem> fetchGangwonFacilities(int pageNumber) {
        String requestUrl = UriComponentsBuilder
                .fromUriString(properties.getFacilityUrl())
                .queryParam("serviceKey", properties.getFacilityServiceKey())
                .queryParam("pageNo", pageNumber)
                .queryParam("numOfRows", properties.getPageSize())
                .queryParam("resultType", "json")
                .queryParam("city_cd", properties.getGangwonCityCode())
                .build()
                .encode()
                .toUriString();

        JsonNode root = requestJson(requestUrl);
        validateSuccess(root);
        return new PublicDataPage<>(readItems(root), totalCount(root));
    }

    private List<PublicFacilityItem> readItems(JsonNode root) {
        List<PublicFacilityItem> items = new ArrayList<>();
        JsonNode itemNodes = root.path("response").path("body").path("items").path("item");
        if (itemNodes.isArray()) {
            itemNodes.forEach(node -> items.add(readItem(node)));
        } else if (itemNodes.isObject()) {
            items.add(readItem(itemNodes));
        }
        return items;
    }

    private JsonNode requestJson(String requestUrl) {
        try {
            String responseBody = restClient.get().uri(requestUrl).retrieve().body(String.class);
            return objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            throw new IllegalStateException("등록시설 API 응답을 읽지 못했습니다.", exception);
        }
    }

    private PublicFacilityItem readItem(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, PublicFacilityItem.class);
        } catch (Exception exception) {
            throw new IllegalStateException("등록시설 API 응답을 변환하지 못했습니다.", exception);
        }
    }

    private int totalCount(JsonNode root) {
        return root.path("response").path("body").path("totalCount").asInt();
    }

    private void validateSuccess(JsonNode root) {
        String resultCode = root.path("response").path("header").path("resultCode").asText();
        if (!"00".equals(resultCode)) {
            String resultMessage = root.path("response").path("header").path("resultMsg").asText();
            throw new IllegalStateException("등록시설 API 호출 실패: " + resultMessage);
        }
    }
}
