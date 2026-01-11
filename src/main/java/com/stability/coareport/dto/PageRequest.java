package com.stability.coareport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy;
    private String sortDirection = "asc";

    public org.springframework.data.domain.PageRequest toSpringPageRequest() {
        org.springframework.data.domain.Sort.Direction direction =
            "desc".equalsIgnoreCase(sortDirection)
                ? org.springframework.data.domain.Sort.Direction.DESC
                : org.springframework.data.domain.Sort.Direction.ASC;

        if (sortBy != null && !sortBy.isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort.by(direction, sortBy)
            );
        }
        return org.springframework.data.domain.PageRequest.of(page, size);
    }
}
