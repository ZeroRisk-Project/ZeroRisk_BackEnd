package com.zerorisk.project.domain.competition.controller;

import com.zerorisk.project.domain.competition.dto.CompetitionCreateRequest;
import com.zerorisk.project.domain.competition.dto.CompetitionParticipantAdminResponse;
import com.zerorisk.project.domain.competition.dto.CompetitionUpdateRequest;
import com.zerorisk.project.domain.competition.service.CompetitionService;
import com.zerorisk.project.global.security.CurrentUserId;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/competitions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    @PostMapping
    public ResponseEntity<Long> create(
            @Valid @RequestBody CompetitionCreateRequest request,
            @CurrentUserId Long adminUserId) {
        Long competitionId = competitionService.createCompetition(request, adminUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(competitionId);
    }

    @PutMapping("/{competitionId}")
    public void update(
            @PathVariable Long competitionId,
            @Valid @RequestBody CompetitionUpdateRequest request) {
        competitionService.updateCompetition(competitionId, request);
    }

    @DeleteMapping("/{competitionId}")
    public void delete(@PathVariable Long competitionId) {
        competitionService.deleteCompetition(competitionId);
    }

    @GetMapping("/{competitionId}/participants")
    public List<CompetitionParticipantAdminResponse> getParticipants(@PathVariable Long competitionId) {
        return competitionService.getParticipantsForAdmin(competitionId);
    }

    @DeleteMapping("/{competitionId}/participants/{userId}")
    public void expelParticipant(
            @PathVariable Long competitionId,
            @PathVariable Long userId) {
        competitionService.expelParticipant(competitionId, userId);
    }
}