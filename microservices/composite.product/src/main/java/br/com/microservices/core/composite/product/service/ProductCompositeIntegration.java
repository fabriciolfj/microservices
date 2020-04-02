package br.com.microservices.core.composite.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.HttpErrorInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

@Component
@EnableBinding(ProductCompositeIntegration.MessageSources.class)
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private final WebClient webClient;
    private final ObjectMapper mapper;

    private MessageSources messageSources;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    public interface MessageSources {

        String OUTPUT_PRODUCTS = "output-products";
        String OUTPUT_RECOMMENDATIONS = "output-recommendations";
        String OUTPUT_REVIEWS = "output-reviews";

        @Output(OUTPUT_PRODUCTS)
        MessageChannel outputProducts();

        @Output(OUTPUT_RECOMMENDATIONS)
        MessageChannel outputRecommendations();

        @Output(OUTPUT_REVIEWS)
        MessageChannel outputReviews();
    }

    @Autowired
    public ProductCompositeIntegration(
            WebClient.Builder webClient,
            ObjectMapper mapper,
            MessageSources messageSources,

            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int    productServicePort,

            @Value("${app.recommendation-service.host}") String recommendationServiceHost,
            @Value("${app.recommendation-service.port}") int    recommendationServicePort,

            @Value("${app.review-service.host}") String reviewServiceHost,
            @Value("${app.review-service.port}") int    reviewServicePort) {

        this.webClient = webClient.build();
        this.mapper = mapper;
        this.messageSources = messageSources;

        productServiceUrl        = "http://" + productServiceHost + ":" + productServicePort + "/product/";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation";
        reviewServiceUrl         = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review";
    }

    public Mono<Product> getProduct(int productId) {
        String url = productServiceUrl + productId;
        LOG.debug("Will call getProduct API on URL: {}", url);

        return webClient.get().uri(url)
                .retrieve()
                .bodyToMono(Product.class)
                .log()
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    @Override
    public Product createProduct(Product body) {
        messageSources.outputProducts().send(MessageBuilder.withPayload(new Event(CREATE, body.getProductId(), body)).build());
        return body;
    }

    @Override
    public void deleteProduct(int productId) {
        messageSources.outputProducts().send(MessageBuilder.withPayload(new Event(DELETE, productId, null)).build());
    }

    private String getErrorMessage(WebClientResponseException ex) {
        try {
            return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
        } catch (IOException ioex) {
            return ex.getMessage();
        }
    }

    public Flux<Recommendation> getRecommendations(int productId) {
        String url = recommendationServiceUrl + "?productId=" + productId;
         LOG.debug("Will call the getRecommendations API on URL: {}", url);

         return webClient.get()
                 .uri(url)
                 .retrieve()
                 .bodyToFlux(Recommendation.class)
                 .log()
                 .onErrorResume(error -> Flux.empty());
    }

    @Override
    public void deleteRecommendations(int productId) {
        messageSources.outputRecommendations().send(MessageBuilder.withPayload(new Event(DELETE, productId, null)).build());
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        messageSources.outputRecommendations().send(MessageBuilder.withPayload(new Event(CREATE, body.getProductId(), body)).build());
        return body;
    }

    public Flux<Review> getReviews(int productId) {
        String url = reviewServiceUrl + "?productId=" + productId;

        LOG.debug("Will call getReviews API on URL: {}", url);

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToFlux(Review.class)
                .log()
                .onErrorResume(e -> Flux.empty());

    }

    @Override
    public Review createReview(Review body) {
        messageSources.outputReviews().send(MessageBuilder.withPayload(new Event(CREATE, body.getProductId(), body)).build());
        return body;
    }

    @Override
    public void deleteReviews(int productId) {
        messageSources.outputReviews().send(MessageBuilder.withPayload(new Event(DELETE, productId, null)).build());
    }

    private Throwable handleException(Throwable e) {

        if (!(e instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", e.toString());
            return e;
        }
        WebClientResponseException ex = (WebClientResponseException)e;

        switch (ex.getStatusCode()) {

            case NOT_FOUND:
                return new NotFoundException(getErrorMessage(ex));

            case UNPROCESSABLE_ENTITY:
                return new InvalidInputException(getErrorMessage(ex));

            default:
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                return ex;
        }
    }
}
