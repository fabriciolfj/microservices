package br.com.microservices.core.product;

import br.com.microservices.core.product.persistence.ProductEntity;
import br.com.microservices.core.product.persistence.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.IntStream.rangeClosed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.data.domain.Sort.Direction.ASC;

@DataMongoTest
@ExtendWith(SpringExtension.class)
public class PersistenceTests {

    @Autowired
    private ProductRepository repository;

    private ProductEntity productEntity;

    @BeforeEach
    public void setupDb() throws Exception {
        repository.deleteAll();
        var entity = new ProductEntity(1, "n", 1);
        productEntity = repository.save(entity);
        assertEqualsProduct(entity, productEntity);
    }

    @Test
    public void create() {
        var newEntity = new ProductEntity(2, "n", 2);
        productEntity = repository.save(newEntity);

        var foundEntity = repository.findById(newEntity.getId()).get();

        assertEqualsProduct(newEntity, foundEntity);
        assertEquals(2, repository.count());
    }

    @Test
    public void update() {
        productEntity.setName("n2");
        repository.save(productEntity);

        var foundEntity = repository.findById(productEntity.getId()).get();
        assertEquals(1, (long) foundEntity.getVersion());
        assertEquals("n2", foundEntity.getName());
    }

    @Test
    public void delete() {
        repository.delete(productEntity);
        assertFalse(repository.existsById(productEntity.getId()));
    }

    @Test
    public void getByProductId() {
        Optional<ProductEntity> entity = repository.findByProductId(productEntity.getProductId());
        assertTrue(entity.isPresent());
        assertEquals(productEntity, entity.get());
    }

    @Test
    public void duplicateError() {
        ProductEntity entity = new ProductEntity(productEntity.getProductId(), "n", 1);
        Assertions.assertThrows(DuplicateKeyException.class, () -> repository.save(entity));
    }

    @Test
    public void optimisticLockError() {
        var entity1 = repository.findById(productEntity.getId()).get();
        var entity2 = repository.findById(productEntity.getId()).get();

        entity1.setName("n1");
        repository.save(entity1);

        try{
            entity2.setName("n2");
            repository.save(entity2);
        } catch (OptimisticLockingFailureException e) {

        }

        var updateEntity = repository.findById(productEntity.getId()).get();
        assertEquals(1, updateEntity.getVersion());
        assertEquals("n1", updateEntity.getName());
    }

    @Test
    public void paging() {

        repository.deleteAll();

        List<ProductEntity> newProducts = rangeClosed(1001, 1010)
                .mapToObj(i -> new ProductEntity(i, "name " + i, i))
                .collect(Collectors.toList());
        repository.saveAll(newProducts);

        Pageable nextPage = PageRequest.of(0, 4, ASC, "productId");
        nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
        nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
        nextPage = testNextPage(nextPage, "[1009, 1010]", false);
    }

    private Pageable testNextPage(Pageable nextPage, String expectedProductIds, boolean expectsNextPage) {
        Page<ProductEntity> productPage = repository.findAll(nextPage);
        assertEquals(expectedProductIds, productPage.getContent().stream().map(p -> p.getProductId()).collect(Collectors.toList()).toString());
        assertEquals(expectsNextPage, productPage.hasNext());
        return productPage.nextPageable();
    }

    private void assertEqualsProduct(ProductEntity expectedEntity, ProductEntity actualEntity) {
        assertEquals(expectedEntity.getId(),               actualEntity.getId());
        assertEquals(expectedEntity.getVersion(),          actualEntity.getVersion());
        assertEquals(expectedEntity.getProductId(),        actualEntity.getProductId());
        assertEquals(expectedEntity.getName(),           actualEntity.getName());
        assertEquals(expectedEntity.getWeight(),           actualEntity.getWeight());
    }
}
