package br.com.microservices.core.composite.product.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.composite.product.ProductAggregate;
import se.magnus.api.composite.product.ProductCompositeService;
import se.magnus.api.composite.product.RecommendationSummary;
import se.magnus.api.composite.product.ReviewSummary;
import se.magnus.api.composite.product.ServiceAddresses;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.http.ServiceUtil;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private final ServiceUtil serviceUtil;
    private final SecurityContext nullSC = new SecurityContextImpl();
    private  ProductCompositeIntegration integration;
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public Mono<ProductAggregate> getCompositeProduct(int productId) {
        return Mono.zip(
                values -> createProductAggregate((SecurityContext) values[0], (Product) values[1], (List<Recommendation>) values[2], (List<Review>) values[3],
                        serviceUtil.getServiceAddress()),
                ReactiveSecurityContextHolder.getContext().defaultIfEmpty(nullSC),
                integration.getProduct(productId),
                integration.getRecommendations(productId).collectList(),
                integration.getReviews(productId).collectList())
                .doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
                .log();
    }

    @Override
    public Mono<Void> createCompositeProduct(ProductAggregate body) {
        return ReactiveSecurityContextHolder.getContext().doOnSuccess(sc -> internalCreateCompositeProduct(sc, body)).then();
    }

    private void internalCreateCompositeProduct(final SecurityContext sc, final ProductAggregate body) {
        try {
            logAuthorizationInfo(sc);
            
            var product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
            integration.createProduct(product);

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    var recommendation = new Recommendation(body.getProductId(), r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
                    integration.createRecommendation(recommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    var review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent(), null);
                    integration.createReview(review);
                });
            }
        } catch (RuntimeException re) {
            LOG.warn("createCompositeProduct failed", re);
            throw re;
        }
    }

    private void logAuthorizationInfo(SecurityContext sc) {
        if(sc != null && sc.getAuthentication() != null && sc.getAuthentication() instanceof JwtAuthenticationToken) {
            Jwt jwtToken = ((JwtAuthenticationToken) sc.getAuthentication()).getToken();
            logAuthorizationInfo(jwtToken);
            return;
        }

        LOG.warn("No JWT based Authentication supplied, running tests are we?");
    }

    private void logAuthorizationInfo(Jwt jwt) {
        if(jwt == null) {
            LOG.warn("No JWT supplied, running tests are we?");
            return;
        }

        if(LOG.isDebugEnabled()) {
            URL issuer = jwt.getIssuer();
            List<String> audience = jwt.getAudience();
            var subject = jwt.getClaims().get("sub");
            var scopes = jwt.getClaims().get("scope");
            var expires = jwt.getClaims().get("exp");

            LOG.debug("Authorization info: Subject: {}, scopes: {}, expires: {}, issuer: {}, audience: {}", subject, scopes, expires, issuer, audience);
        }
    }

    @Override
    public Mono<Void> deleteCompositeProduct(int productId) {
        return ReactiveSecurityContextHolder.getContext().doOnSuccess(sc -> internalDeleteCompositeProduct(sc, productId)).then();
    }

    private void internalDeleteCompositeProduct(SecurityContext sc, int productId) {
        try {
            logAuthorizationInfo(sc);

            integration.deleteProduct(productId);
            integration.deleteRecommendations(productId);
            integration.deleteReviews(productId);
        } catch (RuntimeException ex) {
            LOG.warn("deleteCompositeProduct failed: {}", ex.toString());
        }
    }


    private ProductAggregate createProductAggregate(SecurityContext sc, Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {

        logAuthorizationInfo(sc);

        // 1. Setup product info
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        // 2. Copy summary recommendation info, if available
        List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
                recommendations.stream()
                        .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                        .collect(Collectors.toList());

        // 3. Copy summary review info, if available
        List<ReviewSummary> reviewSummaries = (reviews == null)  ? null :
                reviews.stream()
                        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                        .collect(Collectors.toList());

        // 4. Create info regarding the involved microservices addresses
        String productAddress = product.getServiceAddress();
        String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress = (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }
}
