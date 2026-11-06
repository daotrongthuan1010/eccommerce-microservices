package com.dtthuan3.ecommerce.searchservice.controller;

import com.dtthuan3.ecommerce.common.response.ApiResponse;
import com.dtthuan3.ecommerce.searchservice.service.ReindexService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin reindex (§3 controller mong): logic nam o ReindexService. Chi ADMIN
 * (SecurityConfig). POST /admin/reindex?pageSize=50.
 */
@RestController
@RequestMapping("/admin")
public class ReindexController {

    private final ReindexService reindexService;

    public ReindexController(ReindexService reindexService) {
        this.reindexService = reindexService;
    }

    @PostMapping("/reindex")
    public ApiResponse<Map<String, Object>> reindex(
            @RequestParam(defaultValue = "50") int pageSize) {
        long indexed = reindexService.reindexAll(pageSize);
        return ApiResponse.ok("Reindex hoàn tất", Map.of("indexed", indexed));
    }
}
