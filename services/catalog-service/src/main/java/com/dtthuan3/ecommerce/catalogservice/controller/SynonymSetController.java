package com.dtthuan3.ecommerce.catalogservice.controller;

import com.dtthuan3.ecommerce.catalogservice.contstant.SynonymSetStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymSetCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymSetUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.request.SynonymUpdateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymResponse;
import com.dtthuan3.ecommerce.catalogservice.dto.response.SynonymSetResponse;
import com.dtthuan3.ecommerce.catalogservice.service.SynonymService;
import com.dtthuan3.ecommerce.catalogservice.service.SynonymSetService;
import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/synonym-sets")
@RequiredArgsConstructor
public class SynonymSetController {

    private final SynonymSetService synonymSetService;

    private final SynonymService synonymService;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SynonymSetResponse>> create(
            @Valid
            @RequestBody
            SynonymSetCreateRequest request
    ) {

        SynonymSetResponse response =
                synonymSetService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(response)
                );
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SynonymSetResponse>> getById(
            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        synonymSetService.getById(id)
                )
        );
    }


    @GetMapping
    public ResponseEntity<
            ApiResponse<PageResponse<SynonymSetResponse>>
            > getSynonymSets(

            @RequestParam(required = false)
            SynonymSetStatus status,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        synonymSetService.getSynonymSets(
                                status,
                                page,
                                size
                        )
                )
        );
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SynonymSetResponse>> update(
            @PathVariable String id,

            @Valid
            @RequestBody
            SynonymSetUpdateRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        synonymSetService.update(
                                id,
                                request
                        )
                )
        );
    }


    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SynonymSetResponse>> updateStatus(
            @PathVariable String id,

            @RequestParam
            SynonymSetStatus status
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        synonymSetService.updateStatus(
                                id,
                                status
                        )
                )
        );
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id
    ) {

        synonymSetService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.ok()
        );
    }


    // =====================================================
    // SYNONYM
    // =====================================================

    @PostMapping("/{synonymSetId}/synonyms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SynonymResponse>> createSynonym(

            @PathVariable String synonymSetId,

            @Valid
            @RequestBody
            SynonymCreateRequest request
    ) {

        SynonymResponse response =
                synonymService.create(
                        synonymSetId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.created(response)
                );
    }


    @GetMapping("/{synonymSetId}/synonyms")
    public ResponseEntity<ApiResponse<List<SynonymResponse>>> getSynonyms(

            @PathVariable String synonymSetId
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        synonymService.getBySynonymSetId(
                                synonymSetId
                        )
                )
        );
    }


    @GetMapping("/synonyms/{id}")
    public ResponseEntity<ApiResponse<SynonymResponse>> getSynonymById(

            @PathVariable String id
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        synonymService.getById(id)
                )
        );
    }


    @PutMapping("/synonyms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SynonymResponse>> updateSynonym(

            @PathVariable String id,

            @Valid
            @RequestBody
            SynonymUpdateRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.ok(
                        synonymService.update(
                                id,
                                request
                        )
                )
        );
    }


    @DeleteMapping("/synonyms/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSynonym(

            @PathVariable String id
    ) {

        synonymService.delete(id);

        return ResponseEntity.ok(
                ApiResponse.ok()
        );
    }
}