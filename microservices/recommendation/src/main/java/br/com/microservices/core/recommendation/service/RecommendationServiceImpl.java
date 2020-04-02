package br.com.microservices.core.recommendation.service;

import br.com.microservices.core.recommendation.persistence.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);

    @Autowired
    private ServiceUtil serviceUtil;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private RecommendationMapper mapper;

    @Override
    public Flux<Recommendation> getRecommendations(int productId) {

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return recommendationRepository.findByProductId(productId)
                .log()
                .map(r -> {
                    var result = mapper.entityToApi(r);
                    result.setServiceAddress(serviceUtil.getServiceAddress());
                    return result;
                });
    }

    @Override
    public void deleteRecommendations(int productId) {
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        recommendationRepository.deleteAll(recommendationRepository.findByProductId(productId)).block();
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        LOG.debug("createRecommendation: created a recommendation entity: {}/{}", body.getProductId(), body.getRecommendationId());
        var entity = mapper.apiToEntity(body);
        return recommendationRepository.save(entity)
                .log()
                .onErrorMap(DuplicateKeyException.class, e -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id:" + body.getRecommendationId()))
                .map(r -> mapper.entityToApi(r)).block();
    }
}
