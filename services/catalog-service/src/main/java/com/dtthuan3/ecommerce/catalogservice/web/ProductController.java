package com.dtthuan3.ecommerce.catalogservice.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * API san pham (product-service) chay in-memory de hoc sinh test RBAC ngay.
 *
 * <ul>
 *   <li>GET /products, GET /products/{id}: can dang nhap (USER hoac ADMIN)</li>
 *   <li>POST/PUT/DELETE /products/**: chi ADMIN</li>
 * </ul>
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final Map<String, ProductDto> store = new ConcurrentHashMap<>();

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public List<ProductDto> list() {
        return List.copyOf(store.values());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ProductDto get(@PathVariable String id) {
        ProductDto product = store.get(id);
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        return product;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto create(@RequestBody ProductRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (request.price() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price is required");
        }
        String sku = request.sku() == null || request.sku().isBlank()
                ? "SKU-" + System.currentTimeMillis()
                : request.sku().trim();
        ProductDto product = ProductDto.of(sku, request.name().trim(), request.price(), request.stock() == null ? 0 : request.stock());
        store.put(product.id(), product);
        return product;
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDto update(@PathVariable String id, @RequestBody ProductRequest request) {
        ProductDto existing = store.get(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        ProductDto updated = new ProductDto(
                existing.id(),
                request.sku() == null || request.sku().isBlank() ? existing.sku() : request.sku().trim(),
                request.name() == null || request.name().isBlank() ? existing.name() : request.name().trim(),
                existing.slug(),
                request.description() == null ? existing.description() : request.description(),
                request.price() == null ? existing.price() : request.price(),
                request.currency() == null ? existing.currency() : request.currency(),
                request.stock() == null ? existing.stock() : request.stock(),
                request.status() == null ? existing.status() : request.status(),
                request.category() == null ? existing.category() : request.category(),
                existing.createdAt(),
                Instant.now());
        store.put(id, updated);
        return updated;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> delete(@PathVariable String id) {
        ProductDto removed = store.remove(id);
        if (removed == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        return Map.of("deleted", id);
    }
}
