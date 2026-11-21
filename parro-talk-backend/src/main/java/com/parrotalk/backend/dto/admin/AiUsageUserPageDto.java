package com.parrotalk.backend.dto.admin;



public record AiUsageUserPageDto(java.util.List<AiUsageByUserDto> items, int page, int size, long totalElements, int totalPages) {
}
