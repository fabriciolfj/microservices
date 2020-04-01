package br.com.microservices.core.product.services;

import br.com.microservices.core.product.persistence.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ProductServiceImpl implements ProductService {

    private final ServiceUtil serviceUtil;
    private final ProductRepository repository;
    private final ProductMapper mapper;

    private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    public ProductServiceImpl(ServiceUtil serviceUtil, ProductRepository repository, ProductMapper mapper){
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Product getProduct(int productId) {
        LOG.debug("/product return the found product for productId={}", productId);

        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

        return repository.findByProductId(productId)
                .map(prod -> {
                    var response = mapper.entityToApi(prod);
                    response.setServiceAddress(serviceUtil.getServiceAddress());
                    return response;
                })
                .orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));
    }

    @Override
    public Product createProduct(Product body) {
        try {
            var entity = mapper.apiToEntity(body);
            var newEntity = repository.save(entity);
            return mapper.entityToApi(newEntity);
        } catch (DuplicateKeyException dke) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId());
        }
    }

    @Override
    public void deleteProduct(int productId) {
        repository.findByProductId(productId)
                .ifPresent(e -> repository.delete(e));

    }
}
