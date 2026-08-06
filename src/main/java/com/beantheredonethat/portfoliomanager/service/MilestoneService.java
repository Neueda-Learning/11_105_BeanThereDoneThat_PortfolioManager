package com.beantheredonethat.portfoliomanager.service;

import com.beantheredonethat.portfoliomanager.dto.CreateMilestoneRequest;
import com.beantheredonethat.portfoliomanager.dto.MilestoneResponse;
import com.beantheredonethat.portfoliomanager.dto.NextMilestoneResponse;
import com.beantheredonethat.portfoliomanager.dto.UpdateMilestoneOrderRequest;
import com.beantheredonethat.portfoliomanager.dto.UpdateMilestoneRequest;
import com.beantheredonethat.portfoliomanager.entity.Milestone;
import com.beantheredonethat.portfoliomanager.exception.CustomerNotFoundException;
import com.beantheredonethat.portfoliomanager.exception.ResourceNotFoundException;
import com.beantheredonethat.portfoliomanager.repository.CustomerRepository;
import com.beantheredonethat.portfoliomanager.repository.MilestoneRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MilestoneService {

    private static final Logger logger = LoggerFactory.getLogger(MilestoneService.class);

    private final MilestoneRepository milestoneRepository;
    private final CustomerRepository customerRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${pexels.api.key:YOURAPIKEY}")
    private String pexelsApiKey;

    @Value("${pexels.api.base-url:https://api.pexels.com/v1/search}")
    private String pexelsBaseUrl;

        private static final String LEGACY_FALLBACK_IMAGE =
            "https://images.pexels.com/photos/196644/pexels-photo-196644.jpeg";

    public MilestoneService(MilestoneRepository milestoneRepository,
                            CustomerRepository customerRepository) {
        this.milestoneRepository = milestoneRepository;
        this.customerRepository = customerRepository;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public MilestoneResponse createMilestone(Integer customerId, CreateMilestoneRequest request) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found with ID: " + customerId));

        String item = request.getItem().trim();
        String imageUrl = fetchMilestoneImage(item);
        int nextDisplayOrder = milestoneRepository.getNextDisplayOrder(customerId);

        Milestone milestone = new Milestone(customerId, item, request.getPrice(), imageUrl, nextDisplayOrder);

        Milestone saved = milestoneRepository.save(milestone);
        BigDecimal totalProfit = normalizeProfit(milestoneRepository.getTotalProfitByCustomerId(customerId));
        return toResponse(saved, totalProfit);
    }

    public List<MilestoneResponse> getMilestones(Integer customerId) {
        BigDecimal totalProfit = normalizeProfit(milestoneRepository.getTotalProfitByCustomerId(customerId));
        return milestoneRepository.findByCustomerId(customerId)
                .stream()
            .map(milestone -> toResponse(milestone, totalProfit))
                .collect(Collectors.toList());
    }

    public NextMilestoneResponse getNextMilestone(Integer customerId) {
        List<Milestone> milestones = milestoneRepository.findByCustomerId(customerId);
        if (milestones.isEmpty()) {
            return null;
        }

        BigDecimal totalProfit = normalizeProfit(milestoneRepository.getTotalProfitByCustomerId(customerId));
        Milestone nextMilestone = milestones.get(0);

        BigDecimal milestonePrice = nullSafe(nextMilestone.getPrice());
        BigDecimal completedAmount = totalProfit.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal progressPercentage = calculateBoundedProgress(totalProfit, milestonePrice);

        boolean achieved = totalProfit.compareTo(milestonePrice) >= 0;
        BigDecimal remainingAmount = achieved
                ? BigDecimal.ZERO
                : milestonePrice.subtract(totalProfit).setScale(2, RoundingMode.HALF_UP);

        return new NextMilestoneResponse(
                nextMilestone.getMilestoneId(),
                nextMilestone.getItem(),
                milestonePrice.setScale(2, RoundingMode.HALF_UP),
                resolveImageUrl(nextMilestone),
                totalProfit.setScale(2, RoundingMode.HALF_UP),
                completedAmount,
                progressPercentage,
                remainingAmount,
                achieved);
    }

    public MilestoneResponse updateMilestone(Integer customerId,
                                             Integer milestoneId,
                                             UpdateMilestoneRequest request) {
        Milestone existing = milestoneRepository.findByIdAndCustomerId(milestoneId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found with id: " + milestoneId));

        String item = request.getItem().trim();
        existing.setItem(item);
        existing.setPrice(request.getPrice());
        existing.setImageUrl(fetchMilestoneImage(item));

        Milestone saved = milestoneRepository.save(existing);
        BigDecimal totalProfit = normalizeProfit(milestoneRepository.getTotalProfitByCustomerId(customerId));
        return toResponse(saved, totalProfit);
    }

    public void deleteMilestone(Integer customerId, Integer milestoneId) {
        milestoneRepository.findByIdAndCustomerId(milestoneId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Milestone not found with id: " + milestoneId));

        milestoneRepository.deleteByIdAndCustomerId(milestoneId, customerId);
        milestoneRepository.reorderSequentially(customerId);
    }

    public List<MilestoneResponse> updateMilestoneOrder(Integer customerId,
                                                         UpdateMilestoneOrderRequest request) {
        List<Milestone> milestones = milestoneRepository.findByCustomerId(customerId);
        Set<Integer> existingIds = milestones.stream()
                .map(Milestone::getMilestoneId)
                .collect(Collectors.toCollection(HashSet::new));

        List<Integer> requestedIds = request.getMilestoneIds();
        Set<Integer> requestedSet = new HashSet<>(requestedIds);

        if (existingIds.size() != requestedSet.size() || !existingIds.equals(requestedSet)) {
            throw new IllegalArgumentException("Milestone order must include each milestone exactly once.");
        }

        milestoneRepository.updateDisplayOrder(customerId, requestedIds);
        return getMilestones(customerId);
    }

    private MilestoneResponse toResponse(Milestone milestone, BigDecimal totalProfit) {
        String resolvedImageUrl = ensureMilestoneImageUrl(milestone);

        BigDecimal milestonePrice = nullSafe(milestone.getPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal completedAmount = totalProfit.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = milestonePrice.subtract(totalProfit);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }
        remainingAmount = remainingAmount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal progressPercentage = calculateBoundedProgress(totalProfit, milestonePrice);

        return new MilestoneResponse(
                milestone.getMilestoneId(),
                milestone.getCustomerId(),
                milestone.getItem(),
                milestonePrice,
                resolvedImageUrl,
                milestone.getDisplayOrder(),
                totalProfit.setScale(2, RoundingMode.HALF_UP),
                completedAmount,
                remainingAmount,
                progressPercentage);
    }

    private BigDecimal calculateBoundedProgress(BigDecimal totalProfit, BigDecimal milestonePrice) {
        if (milestonePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal raw = totalProfit.multiply(BigDecimal.valueOf(100))
                .divide(milestonePrice, 4, RoundingMode.HALF_UP);

        if (raw.compareTo(BigDecimal.ZERO) < 0) {
            raw = BigDecimal.ZERO;
        }
        if (raw.compareTo(BigDecimal.valueOf(100)) > 0) {
            raw = BigDecimal.valueOf(100);
        }

        return raw.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeProfit(BigDecimal totalProfit) {
        return totalProfit == null ? BigDecimal.ZERO : totalProfit;
    }

    private String resolveImageUrl(Milestone milestone) {
        String imageUrl = milestone.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return getPlaceholderImageUrl(milestone.getItem());
        }
        return imageUrl;
    }

    private String ensureMilestoneImageUrl(Milestone milestone) {
        String current = milestone.getImageUrl();
        if (current != null && !current.isBlank() && !LEGACY_FALLBACK_IMAGE.equals(current)) {
            return current;
        }

        String fetched = fetchMilestoneImage(milestone.getItem());
        milestone.setImageUrl(fetched);
        milestoneRepository.save(milestone);
        return fetched;
    }

    private String fetchMilestoneImage(String item) {
        if (item == null || item.isBlank()) {
            return getPlaceholderImageUrl(item);
        }

        if (pexelsApiKey == null || pexelsApiKey.isBlank() || "YOURAPIKEY".equals(pexelsApiKey)) {
            logger.warn("[MilestoneImage] Pexels API key is missing or placeholder. Query='{}'", item);
            return getPlaceholderImageUrl(item);
        }

        try {
            String encodedQuery = URLEncoder.encode(item, StandardCharsets.UTF_8);
            String requestUrl = pexelsBaseUrl + "?query=" + encodedQuery + "&per_page=1&page=1&orientation=landscape";

            logger.info("[MilestoneImage] Search query sent to Pexels: {}", item);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("Authorization", pexelsApiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("[MilestoneImage] Full Pexels response: {}", response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("[MilestoneImage] Pexels API returned non-success status {} for query '{}'", response.statusCode(), item);
                return getPlaceholderImageUrl(item);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode photos = root.path("photos");
            if (!photos.isArray() || photos.isEmpty()) {
                logger.warn("[MilestoneImage] No images found for query '{}'", item);
                return getPlaceholderImageUrl(item);
            }

            JsonNode firstPhoto = photos.get(0);
            String large = firstPhoto.path("src").path("large").asText();
            if (large == null || large.isBlank()) {
                logger.warn("[MilestoneImage] First Pexels item had no usable image URL for query '{}'", item);
                return getPlaceholderImageUrl(item);
            }

            logger.info("[MilestoneImage] Selected image URL: {}", large);
            return large;
        } catch (Exception e) {
            logger.error("[MilestoneImage] Pexels lookup failed for query '{}': {}", item, e.getMessage(), e);
            return getPlaceholderImageUrl(item);
        }
    }

    private String getPlaceholderImageUrl(String item) {
        String text = URLEncoder.encode((item == null || item.isBlank()) ? "Goal" : item, StandardCharsets.UTF_8);
        return "https://dummyimage.com/300x180/e0e0e0/616161.png?text=" + text;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}
