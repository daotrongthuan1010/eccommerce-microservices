package com.dtthuan3.ecommerce.catalogservice.service.impl;

import com.dtthuan3.ecommerce.catalogservice.contstant.CategoryStatus;
import com.dtthuan3.ecommerce.catalogservice.dto.request.CategoryCreateRequest;
import com.dtthuan3.ecommerce.catalogservice.dto.response.CategoryResponse;
import com.dtthuan3.ecommerce.catalogservice.entity.Category;
import com.dtthuan3.ecommerce.catalogservice.mapper.CategoryMapper;
import com.dtthuan3.ecommerce.catalogservice.repository.CategoryRepository;
import com.dtthuan3.ecommerce.catalogservice.service.CacheKeys;
import com.dtthuan3.ecommerce.catalogservice.service.OutboxEventService;
import com.dtthuan3.ecommerce.catalogservice.service.RedisCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression test cho bug NPE: CategoryServiceImpl.create() dung Map.of()
 * cho payload outbox — Map.of() tu choi gia tri null (parentId/description/
 * imageObjectKey cua danh muc goc) nen moi POST /categories deu 500.
 * Fix: categoryPayload() null-safe (HashMap).
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private RedisCacheService redisCacheService;

    @Mock
    private OutboxEventService outboxEventService;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(
                categoryRepository,
                new CategoryMapper(),
                redisCacheService,
                outboxEventService
        );
    }

    private CategoryCreateRequest rootCategoryRequest() {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Danh muc goc");
        request.setSlug("danh-muc-goc");
        // Bo trong: parentId/description/imageObjectKey = null nhu request that
        return request;
    }

    @Test
    @DisplayName("Tao danh muc GOC (parentId=null, khong anh, khong mo ta) khong NPE va van phat outbox event")
    void create_rootCategory_withoutOptionalFields_doesNotThrow() {

        CategoryCreateRequest request = rootCategoryRequest();

        when(categoryRepository.existsBySlugAndParentId(eq("danh-muc-goc"), any()))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> categoryService.create(request))
                .doesNotThrowAnyException();

        ArgumentCaptor<Map<String, Object>> payloadCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(outboxEventService).createEvent(
                eq("CATEGORY"),
                any(),
                any(),
                eq("CATEGORY_CREATED"),
                payloadCaptor.capture()
        );

        Map<String, Object> payload = payloadCaptor.getValue();

        assertThat(payload)
                .containsEntry("name", "Danh muc goc")
                .containsEntry("slug", "danh-muc-goc")
                .containsEntry("status", CategoryStatus.ACTIVE.name());

        // Cac truong nullable phai co mat trong payload (null) chu khong nem NPE
        assertThat(payload).containsKeys(
                "parentId", "description", "imageObjectKey", "displayOrder"
        );
    }

    @Test
    @DisplayName("Tao danh muc CON phat payload day du parentId")
    void create_childCategory_includesParentIdInPayload() {

        CategoryCreateRequest request = rootCategoryRequest();
        request.setParentId("cat-parent-01");
        request.setDescription("Mo ta");
        request.setImageObjectKey("img/qa.png");

        when(categoryRepository.existsById("cat-parent-01")).thenReturn(true);

        // validateDepth di len cay bang findCategory(parentId) -> findById
        Category parent = new Category();
        parent.setId("cat-parent-01");
        parent.setName("Danh muc cha");
        parent.setSlug("danh-muc-cha");
        parent.setStatus(CategoryStatus.ACTIVE);
        when(categoryRepository.findById("cat-parent-01"))
                .thenReturn(java.util.Optional.of(parent));

        when(categoryRepository.existsBySlugAndParentId("danh-muc-goc", "cat-parent-01"))
                .thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.create(request);

        assertThat(response).isNotNull();

        ArgumentCaptor<Map<String, Object>> payloadCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(outboxEventService).createEvent(
                eq("CATEGORY"),
                any(),
                any(),
                eq("CATEGORY_CREATED"),
                payloadCaptor.capture()
        );

        assertThat(payloadCaptor.getValue())
                .containsEntry("parentId", "cat-parent-01")
                .containsEntry("description", "Mo ta")
                .containsEntry("imageObjectKey", "img/qa.png");
    }

    @Test
    @DisplayName("Mapper copy imageObjectKey tu create request sang entity")
    void mapper_copiesImageObjectKey() {

        CategoryCreateRequest request = rootCategoryRequest();
        request.setImageObjectKey("img/abc.png");

        Category entity = new CategoryMapper().toEntity(request);

        assertThat(entity.getImageObjectKey()).isEqualTo("img/abc.png");
    }
}
