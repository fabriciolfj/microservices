package br.com.microservices.core.recommendation.service;

import br.com.microservices.core.recommendation.persistence.RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOG = LoggerFactory.getLogger(RecommendationServiceImpl.class);
    private final ServiceUtil serviceUtil;
    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;

    @Autowired
    private RecommendationServiceImpl(ServiceUtil serviceUtil, RecommendationRepository repository, RecommendationMapper mapper){
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return repository.findByProductId(productId).stream()
                .map(rec -> {
                    var result = mapper.entityToApi(rec);
                    result.setServiceAddress(serviceUtil.getServiceAddress());
                    return result;
                }).collect(Collectors.toList());
    }

    @Override
    public void deleteRecommendations(int productId) {
        LOG.debug("deleteRecommendations: tries to delete recommendations for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        LOG.debug("createRecommendation: created a recommendation entity: {}/{}", body.getProductId(), body.getRecommendationId());
        try {
            return Optional.of(mapper.apiToEntity(body))
                    .map(r -> repository.save(r))
                    .map(entity -> mapper.entityToApi(entity)).get();
        } catch (DuplicateKeyException e) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Recommendation Id: " + body.getRecommendationId());
        }
    }
}
