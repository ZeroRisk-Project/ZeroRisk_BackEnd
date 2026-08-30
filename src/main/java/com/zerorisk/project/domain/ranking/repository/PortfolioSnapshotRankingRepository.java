package com.zerorisk.project.domain.ranking.repository;

import com.zerorisk.project.domain.ranking.dto.RankingRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// 주의: PORTFOLIO_SNAPSHOTS, ACCOUNTS는 오상민님 도메인 소유 테이블.
// 우리 쪽엔 해당 엔티티가 없어서 JdbcTemplate 네이티브 쿼리로 직접 조회함.
// RANK()가 정렬+순위 부여까지 SQL에서 전부 끝내므로, 서비스 레이어에는 자바 정렬 로직이 없다.
@Repository
public class PortfolioSnapshotRankingRepository {

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public PortfolioSnapshotRankingRepository(JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    // 전체 누적 랭킹 - 초기 시드머니 대비 현재 자산으로 계산, RANK()가 정본
    // ALL은 비교 기준일 개념이 없어 base_date를 NULL로 둔다.
    private static final String ALL_TIME_QUERY = """
            SELECT
                RANK() OVER (ORDER BY return_rate DESC) AS rank_position,
                user_id, nickname, user_level, return_rate,
                CAST(NULL AS DATE) AS base_date
            FROM (
                SELECT u.ID AS user_id, u.NICKNAME AS nickname, u.USER_LEVEL AS user_level,
                       ROUND((last_snap.TOTAL_ASSET - a.INITIAL_SEED_MONEY)
                             / a.INITIAL_SEED_MONEY * 100, 2) AS return_rate
                FROM USERS u
                JOIN ACCOUNTS a ON a.USER_ID = u.ID AND a.ACCOUNT_TYPE = 'BASIC'
                JOIN PROFILE_SETTINGS ps ON ps.USER_ID = u.ID AND ps.SHOW_RETURN_RATE = 1
                JOIN (
                    SELECT ACCOUNT_ID, TOTAL_ASSET,
                           ROW_NUMBER() OVER (PARTITION BY ACCOUNT_ID ORDER BY SNAPSHOT_DATE DESC) AS RN
                    FROM PORTFOLIO_SNAPSHOTS
                ) last_snap ON last_snap.ACCOUNT_ID = a.ID AND last_snap.RN = 1
                WHERE a.INITIAL_SEED_MONEY > 0
            )
            ORDER BY rank_position
            """;

    // 기간별 랭킹 - 오늘 스냅샷과 N일 전 스냅샷을 각각 조인해서 계산+순위 부여까지 한 번에 처리
    // base_date는 비교에 실제로 쓰인 :baseDate 파라미터를 그대로 결과에 실어 보낸다.
    private static final String PERIOD_QUERY = """
            SELECT
                RANK() OVER (ORDER BY return_rate DESC) AS rank_position,
                user_id, nickname, user_level, return_rate,
                :baseDate AS base_date
            FROM (
                SELECT u.ID AS user_id, u.NICKNAME AS nickname, u.USER_LEVEL AS user_level,
                       ROUND((latest.TOTAL_ASSET - base.TOTAL_ASSET) / base.TOTAL_ASSET * 100, 2) AS return_rate
                FROM USERS u
                JOIN ACCOUNTS a ON a.USER_ID = u.ID AND a.ACCOUNT_TYPE = 'BASIC'
                JOIN PROFILE_SETTINGS ps ON ps.USER_ID = u.ID AND ps.SHOW_RETURN_RATE = 1
                JOIN PORTFOLIO_SNAPSHOTS latest ON latest.ACCOUNT_ID = a.ID AND latest.SNAPSHOT_DATE = :today
                JOIN PORTFOLIO_SNAPSHOTS base ON base.ACCOUNT_ID = a.ID AND base.SNAPSHOT_DATE = :baseDate
                WHERE base.TOTAL_ASSET > 0
            )
            ORDER BY rank_position
            """;

    public List<RankingRow> findAllTimeRankings() {
        return jdbcTemplate.query(ALL_TIME_QUERY, this::mapRow);
    }

    public List<RankingRow> findPeriodRankings(LocalDate today, LocalDate baseDate) {
        return namedParameterJdbcTemplate.query(PERIOD_QUERY,
                Map.of("today", today, "baseDate", baseDate),
                this::mapRow);
    }

    private RankingRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new RankingRow(
                rs.getInt("rank_position"),
                rs.getLong("user_id"),
                rs.getString("nickname"),
                rs.getString("user_level"),
                rs.getBigDecimal("return_rate"),
                rs.getObject("base_date", LocalDate.class));
    }
}
