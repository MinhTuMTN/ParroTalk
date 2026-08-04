package com.parrotalk.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parrotalk.backend.constant.ModerationTargetType;
import com.parrotalk.backend.constant.Role;
import com.parrotalk.backend.dto.moderation.ModerationUpdateRequest;
import com.parrotalk.backend.entity.ModerationState;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.exception.ParroTalkException;
import com.parrotalk.backend.repository.ModerationEventRepository;
import com.parrotalk.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private ModerationEventRepository moderationEventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ModerationService moderationService;

    @Test
    void applyUpdate_ThrowsException_WhenNoFieldsChanged() {
        ModerationState state = ModerationState.initial();
        ModerationUpdateRequest emptyRequest = new ModerationUpdateRequest(null, null, null, false, null, null);

        ParroTalkException ex = assertThrows(ParroTalkException.class, 
            () -> moderationService.applyUpdate(state, ModerationTargetType.APP_FEEDBACK, UUID.randomUUID(), emptyRequest, new User()));

        assertEquals("MODERATION_NO_CHANGES", ex.getErrorCode());
    }

    @Test
    void applyUpdate_ThrowsException_WhenAssigneeIsNotAdmin() {
        UUID nonAdminId = UUID.randomUUID();
        User nonAdmin = new User();
        nonAdmin.setId(nonAdminId);
        nonAdmin.setRole(Role.USER);

        when(userRepository.findById(nonAdminId)).thenReturn(Optional.of(nonAdmin));

        ModerationState state = ModerationState.initial();
        ModerationUpdateRequest request = new ModerationUpdateRequest(null, null, nonAdminId, false, null, null);

        ParroTalkException ex = assertThrows(ParroTalkException.class, 
            () -> moderationService.applyUpdate(state, ModerationTargetType.APP_FEEDBACK, UUID.randomUUID(), request, new User()));

        assertEquals("ASSIGNEE_NOT_ADMIN", ex.getErrorCode());
    }

    @Test
    void applyUpdate_ThrowsException_WhenUnassignIsTrueAndAssigneeIdIsNotNull() {
        ModerationState state = ModerationState.initial();
        ModerationUpdateRequest request = new ModerationUpdateRequest(null, null, UUID.randomUUID(), true, null, null);

        ParroTalkException ex = assertThrows(ParroTalkException.class, 
            () -> moderationService.applyUpdate(state, ModerationTargetType.APP_FEEDBACK, UUID.randomUUID(), request, new User()));

        assertEquals("MODERATION_INVALID_ASSIGNMENT", ex.getErrorCode());
    }
}
