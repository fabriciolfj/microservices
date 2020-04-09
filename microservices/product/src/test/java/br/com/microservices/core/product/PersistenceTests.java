package br.com.microservices.core.product;

import br.com.microservices.core.product.persistence.ProductEntity;
import br.com.microservices.core.product.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

@DataMongoTest(properties = {"spring.cloud.config.enabled=false"})
@ExtendWith(SpringExtension.class)
public class PersistenceTests {

    @Autowired
    private ProductRepository repository;

    private ProductEntity productEntity;

    @BeforeEach
    public void setupDb() throws Exception {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
        var entity = new ProductEntity(1, "n", 1);
        StepVerifier.create(repository.save(entity))
                .expectNextMatches(createEntity -> {
                    productEntity = createEntity;
                    return areProductEqual(entity, productEntity);
                })
                .verifyComplete();
    }

    @Test
    public void create() {
        var newEntity = new ProductEntity(2, "n", 2);

        StepVerifier.create(repository.save(newEntity))
                .expectNextMatches(createEntity -> newEntity.getProductId() == createEntity.getProductId())
                .verifyComplete();
    }

    @Test
    public void update() {
        productEntity.setName("n2");
        StepVerifier.create(repository.save(productEntity))
                .expectNextMatches(updateEntity -> updateEntity.getName().equals("n2"))
                .verifyComplete();

        StepVerifier.create(repository.findById(productEntity.getId()))
                .expectNextMatches(foundEntity -> foundEntity.getVersion() == 1 && foundEntity.getName().equals("n2"))
                .verifyComplete();
    }

    @Test
    public void delete() {
        StepVerifier.create(repository.delete(productEntity)).verifyComplete();
        StepVerifier.create(repository.existsById(productEntity.getId())).expectNext(false).verifyComplete();

    }

    @Test
    public void getByProductId() {
        StepVerifier.create(repository.findByProductId(productEntity.getProductId()))
                .expectNextMatches(foundEntity -> areProductEqual(productEntity, foundEntity))
                .verifyComplete();
    }

    @Test
    public void duplicateError() {
        ProductEntity entity = new ProductEntity(productEntity.getProductId(), "n", 1);
        StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
    }

    @Test
    public void optimisticLockError() {
        // Store the saved entity in two separate entity objects
        ProductEntity entity1 = repository.findById(productEntity.getId()).block();
        ProductEntity entity2 = repository.findById(productEntity.getId()).block();

        // Update the entity using the first entity object
        entity1.setName("n1");
        repository.save(entity1).block();

        //  Update the entity using the second entity object.
        // This should fail since the second entity now holds a old version number, i.e. a Optimistic Lock Error
        StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

        // Get the updated entity from the database and verify its new sate
        StepVerifier.create(repository.findById(productEntity.getId()))
                .expectNextMatches(foundEntity ->
                        foundEntity.getVersion() == 1 &&
                                foundEntity.getName().equals("n1"))
                .verifyComplete();
    }

    private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
        return
                (expectedEntity.getId().equals(actualEntity.getId())) &&
                        (expectedEntity.getVersion() == actualEntity.getVersion()) &&
                        (expectedEntity.getProductId() == actualEntity.getProductId()) &&
                        (expectedEntity.getName().equals(actualEntity.getName())) &&
                        (expectedEntity.getWeight() == actualEntity.getWeight());
    }
}
