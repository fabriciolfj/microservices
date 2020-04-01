package se.magnus.api.core.recommendation;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface RecommendationService {

    /**
     * Sample usage: curl $HOST:$PORT/recommendation?productId=1
     *
     * @param productId
     * @return
     */
    @GetMapping(
        value    = "/recommendation",
        produces = "application/json")
    List<Recommendation> getRecommendations(@RequestParam(value = "productId", required = true) int productId);

    @DeleteMapping(value = "/recommendation")
    void deleteRecommendations(@RequestParam(value = "productId", required = true) int productId);

    @PostMapping(
            value = "/recommendation",
            consumes = "application/json",
            produces = "application/json")
    Recommendation createRecommendation(@RequestBody Recommendation body);
}
