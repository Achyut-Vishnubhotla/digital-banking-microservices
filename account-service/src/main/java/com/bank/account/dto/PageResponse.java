package com.bank.account.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Our own page shape, so the JSON contract doesn't depend on Spring's PageImpl internals. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
