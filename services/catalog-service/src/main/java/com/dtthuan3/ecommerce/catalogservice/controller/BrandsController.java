package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.dto.request.BrandsRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.BrandsRespone;
import com.dtthuan3.ecommerce.catalogservice.service.BrandsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/catalog/brands")
@RequiredArgsConstructor
public class BrandsController {

    private final BrandsService brandsService;


    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandsRespone> create(

            @Valid
            @ModelAttribute
            BrandsRequest request,

            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        brandsService.create(
                                request,
                                image
                        )
                );
    }


    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BrandsRespone> getById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                brandsService.getByID(id)
        );
    }


    @GetMapping(
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BrandsRespone>> getAll() {

        return ResponseEntity.ok(
                brandsService.getAll()
        );
    }


    @PutMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BrandsRespone> update(

            @PathVariable Long id,

            @Valid
            @ModelAttribute
            BrandsRequest request,

            @RequestPart(
                    value = "image",
                    required = false
            )
            MultipartFile image
    ) {

        return ResponseEntity.ok(
                brandsService.update(
                        id,
                        request,
                        image
                )
        );
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {

        brandsService.delete(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}