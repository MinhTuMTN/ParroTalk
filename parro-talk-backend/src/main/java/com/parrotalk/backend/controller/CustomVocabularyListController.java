package com.parrotalk.backend.controller;

import com.parrotalk.backend.dto.ApiResponse;
import com.parrotalk.backend.dto.vocablist.AddVocabularyListItemRequest;
import com.parrotalk.backend.dto.vocablist.CreateVocabularyListRequest;
import com.parrotalk.backend.dto.vocablist.CustomListPracticeQuestionDto;
import com.parrotalk.backend.dto.vocablist.GenerateMoreQuestionsRequest;
import com.parrotalk.backend.dto.vocablist.ShareVocabularyListRequest;
import com.parrotalk.backend.dto.vocablist.SubmitCustomListAnswerRequest;
import com.parrotalk.backend.dto.vocablist.UpdateVocabularyListItemSelectionsRequest;
import com.parrotalk.backend.dto.vocablist.VocabularyListDetailDto;
import com.parrotalk.backend.dto.vocablist.VocabularyListItemDto;
import com.parrotalk.backend.dto.vocablist.VocabularyListSummaryDto;
import com.parrotalk.backend.entity.User;
import com.parrotalk.backend.service.CustomVocabularyListService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

@RestController
@RequestMapping("/api/vocabulary/lists")
@RequiredArgsConstructor
public class CustomVocabularyListController {

    private final CustomVocabularyListService listService;

    @PostMapping
    public ResponseEntity<ApiResponse<VocabularyListSummaryDto>> createList(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateVocabularyListRequest request) {
        return ResponseEntity.ok(ApiResponse.<VocabularyListSummaryDto>builder()
                .result(listService.createList(user, request))
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<VocabularyListSummaryDto>>>> getLists(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.<Map<String, List<VocabularyListSummaryDto>>>builder()
                .result(listService.getMyAndSharedLists(user))
                .build());
    }

    @GetMapping("/search-words")
    public ResponseEntity<ApiResponse<List<VocabularyListItemDto>>> searchWords(
            @RequestParam("q") String query) {
        return ResponseEntity.ok(ApiResponse.<List<VocabularyListItemDto>>builder()
                .result(listService.searchWords(query))
                .build());
    }

    @GetMapping("/{listId}")
    public ResponseEntity<ApiResponse<VocabularyListDetailDto>> getListDetail(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(ApiResponse.<VocabularyListDetailDto>builder()
                .result(listService.getListDetail(user, listId, shareToken))
                .build());
    }

    @PostMapping("/{listId}/items")
    public ResponseEntity<ApiResponse<List<VocabularyListItemDto>>> addWords(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @Valid @RequestBody AddVocabularyListItemRequest request) {
        return ResponseEntity.ok(ApiResponse.<List<VocabularyListItemDto>>builder()
                .result(listService.addWordsToList(user, listId, request))
                .build());
    }

    @PutMapping("/{listId}/items/{itemId}/selections")
    public ResponseEntity<ApiResponse<VocabularyListItemDto>> updateItemSelections(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @PathVariable UUID itemId,
            @RequestBody UpdateVocabularyListItemSelectionsRequest request) {
        return ResponseEntity.ok(ApiResponse.<VocabularyListItemDto>builder()
                .result(listService.updateItemSelections(user, listId, itemId, request))
                .build());
    }

    @PatchMapping("/{listId}/items/{itemId}")
    public ResponseEntity<ApiResponse<VocabularyListItemDto>> patchItemSelections(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @PathVariable UUID itemId,
            @RequestBody UpdateVocabularyListItemSelectionsRequest request) {
        return ResponseEntity.ok(ApiResponse.<VocabularyListItemDto>builder()
                .result(listService.updateItemSelections(user, listId, itemId, request))
                .build());
    }

    @DeleteMapping("/{listId}/items/{itemId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @PathVariable UUID itemId) {
        listService.deleteListItem(user, listId, itemId);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }

    @PostMapping("/{listId}/share")
    public ResponseEntity<ApiResponse<Map<String, Object>>> shareList(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestBody ShareVocabularyListRequest request) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .result(listService.shareList(user, listId, request))
                .build());
    }

    @PostMapping("/{listId}/clone")
    public ResponseEntity<ApiResponse<VocabularyListDetailDto>> cloneList(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestParam(required = false) String shareToken) {
        return ResponseEntity.ok(ApiResponse.<VocabularyListDetailDto>builder()
                .result(listService.cloneList(user, listId, shareToken))
                .build());
    }

    @PostMapping("/{listId}/finalize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizeList(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .result(listService.finalizeAndGenerateQuestions(user, listId))
                .build());
    }

    @PostMapping("/{listId}/generate-more-questions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateMoreQuestions(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestBody GenerateMoreQuestionsRequest request) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .result(listService.generateMoreQuestions(user, listId, request))
                .build());
    }

    @PostMapping("/{listId}/generate-more")
    public ResponseEntity<ApiResponse<Integer>> generateMoreAlias(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestBody GenerateMoreQuestionsRequest request) {
        Map<String, Object> result = listService.generateMoreQuestions(user, listId, request);
        int generated = (result.get("generatedCount") instanceof Number n) ? n.intValue() : 0;
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .result(generated)
                .build());
    }

    @GetMapping("/{listId}/practice/questions")
    public ResponseEntity<ApiResponse<List<CustomListPracticeQuestionDto>>> getPracticeQuestions(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestParam(required = false) String shareToken,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.<List<CustomListPracticeQuestionDto>>builder()
                .result(listService.getPracticeQuestions(user, listId, shareToken, category, limit))
                .build());
    }

    @GetMapping("/{listId}/practice/multiple-choice")
    public ResponseEntity<ApiResponse<List<CustomListPracticeQuestionDto>>> getPracticeQuestionsMcq(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestParam(required = false) String shareToken,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.<List<CustomListPracticeQuestionDto>>builder()
                .result(listService.getPracticeQuestions(user, listId, shareToken, null, limit))
                .build());
    }

    @PostMapping("/{listId}/practice/answer")
    public ResponseEntity<ApiResponse<Void>> recordAnswer(
            @AuthenticationPrincipal User user,
            @PathVariable UUID listId,
            @RequestParam(required = false) String shareToken,
            @RequestBody SubmitCustomListAnswerRequest request) {
        listService.recordAnswerProgress(user, listId, shareToken, request);
        return ResponseEntity.ok(ApiResponse.<Void>builder().build());
    }
}

