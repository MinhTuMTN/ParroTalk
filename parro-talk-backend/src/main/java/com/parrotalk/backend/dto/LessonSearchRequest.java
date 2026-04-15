package com.parrotalk.backend.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class LessonSearchRequest {
    private String q;
    private UUID categoryId;
    private int page = 0;
    private int size = 10;
}
