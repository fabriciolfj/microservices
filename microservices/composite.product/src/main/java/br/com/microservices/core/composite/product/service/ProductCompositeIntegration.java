package br.com.microservices.core.composite.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
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
import java.net.URI;

import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

@Component
@EnableBinding(ProductCompositeIntegration.MessageSources.class)
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

    private WebClient webClient;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper mapper;

    private MessageSources messageSources;

    private final String productServiceUrl = "http://product";
    private final String recommendationServiceUrl = "http://recommendation";
    private final String reviewServiceUrl = "http://review";

    private final int productServiceTimeoutSec;

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
            WebClient.Builder webClientBuilder,
            ObjectMapper mapper,
            MessageSources messageSources,
            @Value("${app.product-service.timeoutSec}") int productServiceTimeoutSec) {

        this.webClientBuilder = webClientBuilder;
        this.mapper = mapper;
        this.messageSources = messageSources;
        this.productServiceTimeoutSec = productServiceTimeoutSec;
    }

    @Retry(name = "product")
    @CircuitBreaker(name = "product" , fallbackMethod = "getProductFallbackValue")
    public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
        URI url = UriComponentsBuilder.fromUriString(productServiceUrl + "/product/{productId}?delay={delay}&faultPercent={faultPercent}").build(productId, delay, faultPercent);
        LOG.debug("Will call getProduct API on URL: {}", url);

        return getWebClient().get().uri(url)
                .retrieve()
                .bodyToMono(Product.class)
                .log()
                .onErrorMap(WebClientResponseException.class, ex -> handleException(ex));
    }

    private Mono<Product> getProductFallbackValue(int productId, int delay, int faultPercent, Throwable cause) {
        LOG.warn(cause.getMessage());
        if (handleException(cause) instanceof NotFoundException) {
            throw new NotFoundException("Product Id: " + productId + " not found in fallback cache!");
        }

        return Mono.just(new Product(productId, "Fallback product" + productId, productId, "0.0.0.0"));
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
        String url = recommendationServiceUrl + "/recommendation?productId=" + productId;
         LOG.debug("Will call the getRecommendations API on URL: {}", url);

         return getWebClient().get()
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
        String url = reviewServiceUrl + "/review?productId=" + productId;

        LOG.debug("Will call getReviews API on URL: {}", url);

        return getWebClient().get()
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

        LOG.info("Verify exception");
        if (!(e instanceof WebClientResponseException)) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", e.toString());
            return e;
        }

        LOG.info("Exception WebClientResponseException");
        WebClientResponseException ex = (WebClientResponseException)e;

        switch (ex.getStatusCode()) {

            case NOT_FOUND:
                LOG.info("Exception NotFoundException");
                return new NotFoundException(getErrorMessage(ex));

            case UNPROCESSABLE_ENTITY:
                LOG.info("Exception InvalidInputException");
                return new InvalidInputException(getErrorMessage(ex));

            default:
                LOG.info("Exception not identified");
                LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", ex.getStatusCode());
                LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                return ex;
        }
    }

    private WebClient getWebClient() {
        if(webClient == null) {
            this.webClient = webClientBuilder.build();
        }

        return webClient;
    }
}
