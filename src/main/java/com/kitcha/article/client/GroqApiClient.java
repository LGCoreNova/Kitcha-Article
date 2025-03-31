package com.kitcha.article.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroqApiClient {

    @Value("${groq.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 기사 요약 요청
    public Map<String, String> getArticleSummaries(String content) {
        Map<String, String> summaries = new HashMap<>();
        summaries.put("longSummary", requestSummary(content, "한글로 8문장으로 요약만해서 보여줘"));
        return summaries;
    }

    // 공통 요청 메서드
    private String sendRequest(String model, List<Map<String, String>> messages, int maxTokens, double temperature) {
        try {
            String url = "https://api.groq.com/openai/v1/chat/completions";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", temperature);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText().trim();

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.err.println("429 Too Many Requests: " + e.getMessage());
            return "요청 실패: 너무 많은 요청입니다. 잠시 후 다시 시도해 주세요.";
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.PAYLOAD_TOO_LARGE) {
                System.err.println("413 Payload Too Large: " + e.getMessage());
                return "요청 실패: 요청 크기가 너무 큽니다.";
            }
            System.err.println("HTTP 요청 실패: " + e.getMessage());
            return "요청 실패: " + e.getMessage();
        } catch (IOException e) {
            System.err.println("응답 처리 중 오류: " + e.getMessage());
            return "요청 실패: 응답 처리 중 오류가 발생했습니다.";
        } catch (Exception e) {
            e.printStackTrace();
            return "요청 실패: 알 수 없는 오류가 발생했습니다.";
        }
    }

    // 기사 요약 요청
    private String requestSummary(String content, String prompt) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "당신은 뉴스 기사를 한글로 요약하는 전문가입니다."),
                Map.of("role", "user", "content", prompt + "\n\n" + trimContent(content))
        );
        return sendRequest("mistral-saba-24b", messages, 500, 0.7);
    }

    // 텍스트 핵심 키워드 추출
    public String extractKeyword(String content) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", "당신은 뉴스 기사에서 핵심 키워드를 한글로 하나 추출하는 전문가입니다."),
                Map.of("role", "user", "content", "이 기사에서 가장 중요한 핵심 키워드를 한글로 하나만 알려주세요: \n\n" + content)
        );
        return sendRequest("mistral-saba-24b", messages, 10, 0.3);
    }

    // 이미지 속 키워드 추출
    public String extractKeywordFromImage(MultipartFile imageFile) {
        try {
            String base64Image = encodeImageToBase64(imageFile);

            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", "당신은 이미지에서 핵심 키워드를 추출하는 전문가입니다."),
                    Map.of("role", "user", "content", "\"이 이미지와 가장 관련된 한글 단어 하나만 반환해. 설명이나 추가적인 문구 없이 오직 단어만.\".\n\n[이미지 데이터]\n" + base64Image)
            );

            return sendRequest("llama-3.2-11b-vision-preview", messages, 5, 0.3);
        } catch (IOException e) {
            System.err.println("이미지 처리 실패: " + e.getMessage());
            return "키워드 추출 실패: 이미지 처리 중 오류 발생";
        }
    }

    // 콘텐츠 길이 제한
    private String trimContent(String content) {
        int maxLength = 1500;
        return (content != null && content.length() > maxLength) ? content.substring(0, maxLength) + "..." : content;
    }

    // 이미지 Base64 인코딩
    private String encodeImageToBase64(MultipartFile imageFile) throws IOException {
        byte[] imageBytes = imageFile.getBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
