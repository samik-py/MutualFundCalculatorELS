package com.mfcalculator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@Primary
public class GeminiPortfolioProfileClassifier implements PortfolioProfileClassifier {
  private static final Logger logger = LoggerFactory.getLogger(GeminiPortfolioProfileClassifier.class);

  private static final String CLASSIFIER_PROMPT = """
      You classify investor prompts into exactly one portfolio risk profile.
      Return only JSON matching the supplied schema.
      Choose the single best fit from:
      VERY_AGGRESSIVE
      AGGRESSIVE
      SLIGHTLY_AGGRESSIVE
      MODERATE
      SLIGHTLY_CONSERVATIVE
      CONSERVATIVE
      VERY_CONSERVATIVE

      Base the classification on stated risk tolerance, time horizon, need for income, drawdown tolerance, age/life stage cues, and preservation vs growth language.
      Do not avoid aggressive categories just because they imply higher risk. If the user clearly prioritizes growth, accepts volatility, has a long time horizon, or explicitly asks for aggressive positioning, choose AGGRESSIVE or VERY_AGGRESSIVE.
      Use VERY_AGGRESSIVE for language like maximum growth, highest growth, willing to take major losses, high volatility is acceptable, swing for the fences, speculative, or very long horizon with explicit high-risk tolerance.
      Use AGGRESSIVE for language like aggressive growth, strong growth focus, long-term growth, comfortable with significant volatility, can handle drawdowns, or young investor with decades until withdrawal.
      Use MODERATE only when the signal is genuinely mixed or balanced between growth and preservation.
      If the user explicitly names a risk tier, honor that tier unless the rest of the prompt directly contradicts it.
      """;

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final HeuristicPortfolioProfileClassifier fallbackClassifier;
  private final String apiKey;
  private final String model;
  private final String baseUrl;

  public GeminiPortfolioProfileClassifier(
      RestTemplateBuilder restTemplateBuilder,
      ObjectMapper objectMapper,
      HeuristicPortfolioProfileClassifier fallbackClassifier,
      @Value("${gemini.apiKey:${GEMINI_API_KEY:}}") String apiKey,
      @Value("${gemini.model:${GEMINI_MODEL:gemini-2.5-flash}}") String model,
      @Value("${gemini.baseUrl:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
      @Value("${gemini.timeoutSeconds:20}") long timeoutSeconds
  ) {
    this.restTemplate = restTemplateBuilder
        .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
        .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
        .build();
    this.objectMapper = objectMapper;
    this.fallbackClassifier = fallbackClassifier;
    this.apiKey = apiKey;
    this.model = model;
    this.baseUrl = baseUrl;
  }

  @Override
  public PortfolioRiskProfile classify(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      return PortfolioRiskProfile.MODERATE;
    }
    if (apiKey == null || apiKey.isBlank()) {
      logger.debug("Gemini portfolio classifier fallback: missing Gemini API key");
      return fallbackClassifier.classify(prompt);
    }

    try {
      String content = requestClassification(prompt);
      PortfolioRiskProfile parsed = parseProfile(content);
      logger.debug("Gemini portfolio classifier selected profile={}", parsed);
      return parsed;
    } catch (RestClientResponseException ex) {
      logger.warn(
          "Gemini portfolio classification failed: status={}, message={}",
          ex.getRawStatusCode(),
          ex.getMessage()
      );
    } catch (RuntimeException ex) {
      logger.warn("Gemini portfolio classification failed: message={}", ex.getMessage());
    }

    return fallbackClassifier.classify(prompt);
  }

  private String requestClassification(String prompt) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-goog-api-key", apiKey);

    Map<String, Object> payload = Map.of(
        "systemInstruction", Map.of(
            "parts", List.of(Map.of("text", CLASSIFIER_PROMPT))
        ),
        "contents", List.of(
            Map.of("parts", List.of(Map.of("text", prompt)))
        ),
        "generationConfig", Map.of(
            "temperature", 0,
            "responseMimeType", "application/json",
            "responseJsonSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "profile", Map.of(
                        "type", "string",
                        "description", "The best-fit portfolio risk profile.",
                        "enum", List.of(
                            "VERY_AGGRESSIVE",
                            "AGGRESSIVE",
                            "SLIGHTLY_AGGRESSIVE",
                            "MODERATE",
                            "SLIGHTLY_CONSERVATIVE",
                            "CONSERVATIVE",
                            "VERY_CONSERVATIVE"
                        )
                    )
                ),
                "required", List.of("profile"),
                "propertyOrdering", List.of("profile")
            )
        )
    );

    ResponseEntity<Map> entity = restTemplate.exchange(
        baseUrl + "/models/" + model + ":generateContent",
        HttpMethod.POST,
        new HttpEntity<>(payload, headers),
        Map.class
    );

    return extractText(entity.getBody());
  }

  private String extractText(Map<?, ?> response) {
    if (response == null) {
      throw new IllegalStateException("Gemini response body was empty");
    }
    Object candidates = response.get("candidates");
    if (!(candidates instanceof List<?> candidateList) || candidateList.isEmpty()) {
      throw new IllegalStateException("Gemini response contained no candidates");
    }
    Object firstCandidate = candidateList.get(0);
    if (!(firstCandidate instanceof Map<?, ?> candidateMap)) {
      throw new IllegalStateException("Gemini candidate was not an object");
    }
    Object content = candidateMap.get("content");
    if (!(content instanceof Map<?, ?> contentMap)) {
      throw new IllegalStateException("Gemini response contained no content");
    }
    Object parts = contentMap.get("parts");
    if (!(parts instanceof List<?> partList) || partList.isEmpty()) {
      throw new IllegalStateException("Gemini response contained no parts");
    }
    Object firstPart = partList.get(0);
    if (!(firstPart instanceof Map<?, ?> partMap)) {
      throw new IllegalStateException("Gemini response part was not an object");
    }
    Object text = partMap.get("text");
    if (!(text instanceof String json) || json.isBlank()) {
      throw new IllegalStateException("Gemini response text was empty");
    }
    return json;
  }

  PortfolioRiskProfile parseProfile(String content) {
    try {
      JsonNode root = objectMapper.readTree(content);
      return PortfolioRiskProfile.fromModelValue(root.path("profile").asText());
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to parse Gemini portfolio profile JSON", ex);
    }
  }
}
