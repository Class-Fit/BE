package com.example.classfit.course.external;

import com.example.classfit.course.config.PublicDataProperties;
import com.example.classfit.course.dto.PublicCourseItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
public class VoucherCourseApiClient {

    private static final int MAX_RETRY_COUNT = 3;
    private static final long REQUEST_INTERVAL_MILLIS = 300L;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;
    private final PublicDataProperties properties;

    public VoucherCourseApiClient(ObjectMapper objectMapper, PublicDataProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public PublicDataPage<PublicCourseItem> fetchCourses(
            String businessRegistrationNumber,
            String facilitySerialNumber,
            int pageNumber
    ) {
        String requestUrl = UriComponentsBuilder
                .fromUriString(properties.getCourseUrl())
                .queryParam("serviceKey", properties.getCourseServiceKey())
                .queryParam("pageNo", pageNumber)
                .queryParam("numOfRows", properties.getPageSize())
                .queryParam("resultType", "json")
                .queryParam("brno", businessRegistrationNumber)
                .queryParam("facil_sn", facilitySerialNumber)
                .build()
                .encode()
                .toUriString();

        JsonNode root = requestJson(requestUrl);
        validateSuccess(root, "등록강좌");
        return new PublicDataPage<>(readItems(root), totalCount(root));
    }

    private List<PublicCourseItem> readItems(JsonNode root) {
        List<PublicCourseItem> items = new ArrayList<>();
        JsonNode itemNodes = root.path("response").path("body").path("items").path("item");
        if (itemNodes.isArray()) {
            itemNodes.forEach(node -> items.add(readItem(node)));
        } else if (itemNodes.isObject()) {
            items.add(readItem(itemNodes));
        }
        return items;
    }

    private JsonNode requestJson(String requestUrl) {
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            pause(REQUEST_INTERVAL_MILLIS);

            try {
                String responseBody = restClient.get().uri(requestUrl).retrieve().body(String.class);
                return objectMapper.readTree(responseBody);
            } catch (RestClientResponseException exception) {
                boolean rateLimited = exception.getStatusCode().value() == 429;
                if (rateLimited && attempt < MAX_RETRY_COUNT) {
                    pause(1000L * attempt);
                    continue;
                }
                throw new IllegalStateException(
                        "등록강좌 API 응답을 읽지 못했습니다: " + exception.getMessage(),
                        exception
                );
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "등록강좌 API 응답을 읽지 못했습니다: " + exception.getMessage(),
                        exception
                );
            }
        }

        throw new IllegalStateException("등록강좌 API 재시도 횟수를 초과했습니다.");
    }

    private void pause(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("등록강좌 API 호출이 중단되었습니다.", exception);
        }
    }

    private PublicCourseItem readItem(JsonNode node) {
        try {
            return objectMapper.treeToValue(node, PublicCourseItem.class);
        } catch (Exception exception) {
            throw new IllegalStateException("등록강좌 API 응답을 변환하지 못했습니다.", exception);
        }
    }

    private int totalCount(JsonNode root) {
        return root.path("response").path("body").path("totalCount").asInt();
    }

    private void validateSuccess(JsonNode root, String apiName) {
        String resultCode = root.path("response").path("header").path("resultCode").asText();
        if (!"00".equals(resultCode)) {
            String resultMessage = root.path("response").path("header").path("resultMsg").asText();
            throw new IllegalStateException(apiName + " API 호출 실패: " + resultMessage);
        }
    }
}
