package com.dtthuan3.ecommerce.authservice.iam.service;

import com.dtthuan3.ecommerce.authservice.iam.entity.Action;
import com.dtthuan3.ecommerce.authservice.iam.repository.ActionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ActionAdminService {

    private final ActionRepository actionRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return actionRepository.findAllByIsDeletedFalseOrderByCodeAsc().stream()
                .map(this::toMap).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> get(Long id) {
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found: " + id));
        return toMap(action);
    }

    @Transactional
    public Map<String, Object> create(String code, String name, String description) {
        String upper = code.trim().toUpperCase();
        if (actionRepository.existsByCode(upper)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Action code already exists: " + upper);
        }
        Action action = Action.builder().code(upper).name(name).description(description)
                .isActive(true).isDeleted(false).build();
        return toMap(actionRepository.save(action));
    }

    @Transactional
    public Map<String, Object> update(Long id, String name, String description) {
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found: " + id));
        if (name != null) {
            action.setName(name);
        }
        if (description != null) {
            action.setDescription(description);
        }
        return toMap(actionRepository.save(action));
    }

    /** Bat/tat action (khong xoa cung) - permission tham chieu giu nguyen nhung danh gia denied khi tat. */
    @Transactional
    public Map<String, Object> toggle(Long id) {
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Action not found: " + id));
        action.setIsActive(!Boolean.TRUE.equals(action.getIsActive()));
        return toMap(actionRepository.save(action));
    }

    public Map<String, Object> toMap(Action action) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", action.getId());
        map.put("code", action.getCode());
        map.put("name", action.getName());
        map.put("description", action.getDescription());
        map.put("active", Boolean.TRUE.equals(action.getIsActive()));
        return map;
    }
}
