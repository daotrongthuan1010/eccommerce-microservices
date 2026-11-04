package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.contstant.ManufacturerStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.ManufacturerUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.ManufacturerResponse;

import com.dtthuan3.ecommerce.catalogservice.service.ManufacturersService;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manufacturers")
@RequiredArgsConstructor
public class ManufacturerController {

    private final ManufacturersService manufacturerService;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ManufacturerResponse> create(
            @Valid @RequestBody ManufacturerCreateRequest request
    ) {

        return ApiResponse.created(
                manufacturerService.create(request)
        );
    }


    @GetMapping("/{id}")
    public ApiResponse<ManufacturerResponse> getById(
            @PathVariable String id
    ) {

        return ApiResponse.ok(
                manufacturerService.getById(id)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<ManufacturerResponse>>
    getManufacturers(

            @RequestParam(required = false)
            ManufacturerStatus status,

            @RequestParam(required = false)
            String name,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ApiResponse.ok(
                manufacturerService.getManufacturers(
                        status,
                        name,
                        page,
                        size
                )
        );
    }


    @GetMapping("/active")
    public ApiResponse<List<ManufacturerResponse>>
    getActiveManufacturers() {

        return ApiResponse.ok(
                manufacturerService.getActiveManufacturers()
        );
    }


    @GetMapping("/search")
    public ApiResponse<List<ManufacturerResponse>>
    search(
            @RequestParam String keyword
    ) {

        return ApiResponse.ok(
                manufacturerService.search(keyword)
        );
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ManufacturerResponse> update(
            @PathVariable String id,
            @Valid @RequestBody ManufacturerUpdateRequest request
    ) {

        return ApiResponse.ok(
                manufacturerService.update(
                        id,
                        request
                )
        );
    }


    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ManufacturerResponse> updateStatus(
            @PathVariable String id,
            @RequestParam ManufacturerStatus status
    ) {

        return ApiResponse.ok(
                manufacturerService.updateStatus(
                        id,
                        status
                )
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String id
    ) {

        manufacturerService.delete(id);
    }
}