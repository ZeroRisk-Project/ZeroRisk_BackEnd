package com.zerorisk.project.domain.competition.dto;

import java.util.List;

public record MyJoinedCompetitionsResponse(
        List<Long> competitionIds) {
}