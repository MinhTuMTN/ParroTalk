package com.parrotalk.backend.controller.admin;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.admin.AdminDashboardStatsResponse;
import com.parrotalk.backend.service.admin.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminDashboardService dashboardService;

    @InjectMocks
    private AdminDashboardController dashboardController;

    @Test
    void getDashboardStats_ReturnsOkWithResponse() {
        AdminDashboardStatsResponse mockStats = AdminDashboardStatsResponse.builder()
                .totalUsers(150)
                .activeUsers(120)
                .totalLessons(45)
                .publishedLessons(40)
                .hiddenLessons(5)
                .totalCategories(12)
                .totalTags(25)
                .totalVocabularies(350)
                .pendingLessonReports(3)
                .pendingFeedbacks(2)
                .build();

        when(dashboardService.getDashboardStats()).thenReturn(mockStats);

        ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> response = dashboardController.getDashboardStats();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(150, response.getBody().getResult().getTotalUsers());
        assertEquals(45, response.getBody().getResult().getTotalLessons());
        assertEquals(3, response.getBody().getResult().getPendingLessonReports());

        verify(dashboardService).getDashboardStats();
    }
}
