package com.yachaerang.batch.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yachaerang.batch.domain.dto.KamisApiResponse;
import com.yachaerang.batch.domain.dto.KamisPriceItem;
import com.yachaerang.batch.exception.GeneralException;
import com.yachaerang.batch.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.springframework.util.StringUtils.truncate;

/*
KAMIS로부터 데이터를 가져오는 코드
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KamisApiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String KAMIS_DAILY_PRICE_URL = "http://www.kamis.or.kr/service/price/xml.do";

    @Value("${external.kamis.api-key}")
    private String kamisApiKey;

    @Value("${external.kamis.id}")
    private String kamisId;

    /**
     * 특정 날짜의 일별 가격 정보 조회
     */
    public List<KamisPriceItem> getDailyPrices(LocalDate date, String categoryCode) {
        String url = buildApiUrl(date, categoryCode);

        log.info("KAMIS API 호출: date={}, category={}, url={}", date, categoryCode, url);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            MediaType contentType = response.getHeaders().getContentType();
            String body = response.getBody();

            log.info("KAMIS 응답 status = {}, contentType = {}",
                    response.getStatusCode(), contentType);
            log.info("KAMIS 응답 body(앞부분)={}", truncate(body));

            if (contentType != null && contentType.includes(MediaType.TEXT_HTML)) {
                throw new GeneralException("KAMIS API HTML 에러 응답: " + truncate(body));
            }

            if (body == null || body.isBlank()) {
                log.warn("KAMIS API 응답 body가 비어있습니다: date={}", date);
                return Collections.emptyList();
            }

            KamisApiResponse apiResponse;
            try {
                JsonNode root = objectMapper.readTree(body);
                JsonNode data = root.get("data");

                // 001 (데이터 비어있는) 경우
                if (data != null && data.isArray() && data.size() == 1 && data.get(0).isTextual()
                && "001".equals(data.get(0).asText())) {
                    log.warn("KAMIS API data=001 응답 (데이터 없음): date={}, body={}",
                            date, truncate(body));
                    return Collections.emptyList();
                }

                // 파싱이 되는 경우
                apiResponse = objectMapper.readValue(body, KamisApiResponse.class);

            } catch (Exception e) {
                log.error("KAMIS JSON 파싱 실패: body={}", truncate(body), e);
                throw new GeneralException("KAMIS JSON 파싱 실패", e);
            }

            if (apiResponse == null || apiResponse.getData() == null) {
                log.warn("KAMIS API 응답 데이터가 없습니다: date={}", date);
                return Collections.emptyList();
            }

            if (!"000".equals(apiResponse.getData().getErrorCode())) {
                log.warn("KAMIS API 에러 코드: {}, message={}",
                        apiResponse.getData().getErrorCode());
                return Collections.emptyList();
            }

            List<KamisPriceItem> items = apiResponse.getData().getItems();
            log.info("KAMIS API 응답 수신: {} 건", items != null ? items.size() : 0);

            return items != null ? items : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("KAMIS API 호출 실패: date={}, error={}", date, e.getMessage(), e);
            throw new GeneralException("KAMIS API 호출 실패", e);
        }
    }

    private String buildApiUrl(LocalDate date, String categoryCode) {
        return UriComponentsBuilder.fromHttpUrl(KAMIS_DAILY_PRICE_URL)
                .queryParam("action", "dailyPriceByCategoryList")
                .queryParam("p_product_cls_code", "02") // 01: 소매, 02: 도매
                .queryParam("p_regday", DateUtils.formatForApi(date))
                .queryParam("p_convert_kg_yn", "N")
                .queryParam("p_item_category_code", categoryCode)
                .queryParam("p_cert_key", kamisApiKey)
                .queryParam("p_cert_id", kamisId)
                .queryParam("p_returntype", "json")
                .build()
                .toUriString();
    }
}
