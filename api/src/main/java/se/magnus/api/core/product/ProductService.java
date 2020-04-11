package se.magnus.api.core.product;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

public interface ProductService {

    /**
     * Sample usage: curl $HOST:$PORT/product/1
     *
     * @param productId
     * @return the product, if found, else null
     */
    @GetMapping(
        value    = "/product/{productId}",
        produces = "application/json")
    Mono<Product> getProduct(@PathVariable int productId,
                             @RequestParam(value = "delay", required = false, defaultValue = "0") int delay,
                             @RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent);

    @PostMapping(value = "/product", consumes = "application/json", produces = "application/json")
    Product createProduct(@RequestBody Product body);

    @DeleteMapping(value = "/product/{productId}")
    void deleteProduct(@PathVariable int productId);
}
