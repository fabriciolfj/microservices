package br.com.microservices.core.review.service;

import br.com.microservices.core.review.persistence.ReviewEntity;
import br.com.microservices.core.review.persistence.ReviewRepository;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;

@RestController
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;
    private final ReviewRepository repository;
    private final ReviewMapper mapper;
    private final Scheduler scheduler;

    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil, ReviewRepository repository, ReviewMapper mapper, Scheduler scheduler) {
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
        this.scheduler = scheduler;
    }

    @Override
    public Flux<Review> getReviews(int productId) {

       if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

       return asyncFlux(() -> Flux.fromIterable(getByProductId(productId))).log(null, Level.FINE);
    }

    protected List<Review> getByProductId(int productId) {
        List<ReviewEntity> entityList = repository.findByProductId(productId);
        List<Review> list = mapper.entityListToApiList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

        LOG.debug("getReviews: response size: {}", list.size());
        return list;
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

    private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.defer(publisherSupplier).subscribeOn(scheduler);
    }
}
