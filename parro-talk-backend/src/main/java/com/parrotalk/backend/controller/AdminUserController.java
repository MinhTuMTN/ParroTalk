package com.parrotalk.backend.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.constant.UserStatus;
import com.parrotalk.backend.dto.AdminResetPasswordResponse;
import com.parrotalk.backend.dto.AdminUserResponse;
import com.parrotalk.backend.dto.CreateUserRequest;
import com.parrotalk.backend.dto.PageResponse;
import com.parrotalk.backend.dto.UpdateUserRequest;
import com.parrotalk.backend.dto.UpdateUserStatusRequest;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.AdminUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<PageResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminUserService.searchUsers(search, role, status, page, size));
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(adminUserService.createUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getUserDetail(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal User currentAdmin) {
        return ResponseEntity.ok(
                adminUserService.updateStatus(id, request.getStatus(), currentAdmin.getId()));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<AdminResetPasswordResponse> resetPassword(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.resetPassword(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentAdmin) {
        adminUserService.deleteUser(id, currentAdmin.getId());
        return ResponseEntity.noContent().build();
    }
}
