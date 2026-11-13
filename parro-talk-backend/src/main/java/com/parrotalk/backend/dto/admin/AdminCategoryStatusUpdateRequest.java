package com.parrotalk.backend.dto.admin;

import com.parrotalk.backend.constant.CmsItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminCategoryStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private CmsItemStatus status;
}
