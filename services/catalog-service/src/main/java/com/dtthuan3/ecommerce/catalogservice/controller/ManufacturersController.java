package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturersRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturersRespone;
import com.dtthuan3.ecommerce.catalogservice.service.ManufacturersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/manufacturers")
@RequiredArgsConstructor
public class ManufacturersController {

    private final ManufacturersService manufacturersService;


    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManufacturersRespone> create(
            @Valid @RequestBody ManufacturersRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        manufacturersService.create(request)
                );
    }


    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ManufacturersRespone> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                manufacturersService.getById(id)
        );
    }


    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ManufacturersRespone>> getAll() {

        return ResponseEntity.ok(
                manufacturersService.getAll()
        );
    }


    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ManufacturersRespone> update(

            @PathVariable Long id,

            @Valid
            @RequestBody
            ManufacturersRequest request
    ) {

        return ResponseEntity.ok(
                manufacturersService.update(
                        id,
                        request
                )
        );
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        manufacturersService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}