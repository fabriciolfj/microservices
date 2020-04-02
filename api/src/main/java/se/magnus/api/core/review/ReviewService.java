package se.magnus.api.core.review;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ReviewService {

    /**
     * Sample usage: curl $HOST:$PORT/review?productId=1
     *
     * @param productId
     * @return
     */
    @GetMapping(
        value    = "/review",
        produces = "application/json")
    Flux<Review> getReviews(@RequestParam(value = "productId", required = true) int productId);

    @PostMapping(value = "/review", consumes = "application/json", produces = "application/json")
    Review createReview(@RequestBody Review body);

    @DeleteMapping(value = "/review")
    void deleteReviews(@RequestParam(value = "productId", required = true) int productId);
}