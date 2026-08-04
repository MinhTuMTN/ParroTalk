package com.parrotalk.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Tag response DTO.
 * 
 * @author MinhTuMTN
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagResponse {
    /** Tag ID */
    private UUID id;
    
    /** Tag name */
    private String name;
}
