package com.learning.catalog_service.controller;
import com.learning.catalog_service.dto.response.ApiResponse;
import com.learning.catalog_service.entity.Tag;
import com.learning.catalog_service.exception.ResourceNotFoundException;
import com.learning.catalog_service.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagRepository tagRepository;

    /**
     * GET /api/v1/tags/by-name?name=electronics
     */
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<Tag>> getByName(@RequestParam String name) {
        Tag result = tagRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + name));
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Tag retrieved", result));
    }

    /**
     * GET /api/v1/tags/by-names?names=electronics,sale,new
     */
    @GetMapping("/by-names")
    public ResponseEntity<ApiResponse<List<Tag>>> getByNames(@RequestParam List<String> names) {
        List<Tag> result = tagRepository.findByNameIn(names);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), "Tags retrieved", result));
    }
}