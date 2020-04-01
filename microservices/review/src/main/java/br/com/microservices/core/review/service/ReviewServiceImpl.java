package br.com.microservices.core.review.service;

import br.com.microservices.core.review.persistence.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ReviewRepository repository;
    private final ReviewMapper mapper;

    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewRepository repository, ReviewMapper mapper) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Review> getReviews(int productId) {

       if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

       return repository.findByProductId(productId).stream()
               .map(review -> {
                   var result = mapper.entityToApi(review);
                   result.setServiceAddress(serviceUtil.getServiceAddress());
                   return result;
               }).collect(Collectors.toList());
    }

    @Override
    public Review createReview(Review body) {
        try {
            return Optional.of(mapper.apiToEntity(body))
                    .map(repository::save)
                    .map(mapper::entityToApi)
                    .get();
        } catch (DataIntegrityViolationException e ){
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Review Id: " + body.getReviewId());
        }
    }

    @Override
    public void deleteReviews(int productId) {
        LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}
