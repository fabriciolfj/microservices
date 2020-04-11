package br.com.microservices.core.product.services;

import br.com.microservices.core.product.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import java.util.Random;

@RestController
public class ProductServiceImpl implements ProductService {

    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final Random  randomNumberGenerator = new Random();

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository repository, ProductMapper mapper){
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
        if (productId < 1 ) throw new InvalidInputException("Invalid productId: " + productId);

        if (delay > 0) simulateDelay(delay);

        if (faultPercent > 0) throwErrorIfBadLuck(faultPercent);

        return repository.findByProductId(productId)
                .switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
                .log()
                .map(prod -> {
                    var response = mapper.entityToApi(prod);
                    response.setServiceAddress(serviceUtil.getServiceAddress());
                    return response;
                });
    }

    @Override
    public Product createProduct(Product body) {
        var entity = mapper.apiToEntity(body);
        return repository.save(entity)
                .log()
                .onErrorMap(DuplicateKeyException.class, ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()))
                .map(e -> mapper.entityToApi(e)).block();

    }

    @Override
    public void deleteProduct(int productId) {
        repository.findByProductId(productId)
                .log()
                .map(e -> repository.delete(e))
                .flatMap(e -> e).block();
    }

    private void simulateDelay(int delay) {
        LOG.debug("Sleeping for {} seconds...", delay);
        try {
            Thread.sleep(delay * 1000);
        } catch (InterruptedException e) {};
    }

    private void throwErrorIfBadLuck(int faultPercent) {
        int randomThreshold = getRandomNumber(1, 100);
        if(faultPercent < randomThreshold) {
            LOG.debug("We got lucky, no error occurred, {} < {}", faultPercent, randomThreshold);
            return;
        }

        LOG.debug("Bad luck, an error ocurred, {} >= {}", faultPercent, randomThreshold);
        throw new RuntimeException("Something went wrong...");
    }

    private int getRandomNumber(int min, int max) {
        if(max < min) {
            throw new RuntimeException("Max must be greater than min");
        }

        return randomNumberGenerator.nextInt((max - min) + 1) + min;
    }
}
