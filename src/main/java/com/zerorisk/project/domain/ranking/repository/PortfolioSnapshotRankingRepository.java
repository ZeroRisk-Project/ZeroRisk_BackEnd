package com.zerorisk.project.domain.ranking.repository;

import com.zerorisk.project.domain.ranking.dto.AccountReturnRateRow;
import com.zerorisk.project.domain.ranking.dto.AccountUserInfoRow;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// 주의: PORTFOLIO_SNAPSHOTS, ACCOUNTS는 오상민님 도메인 소유 테이블.
// 우리 쪽엔 해당 엔티티가 없어서 JdbcTemplate 네이티브 쿼리로 직접 조회함.
//
// TODO: ACCOUNTS.INITIAL_SEED_MONEY 컬럼은 아직 추가되지 않은 예정 컬럼임.
// 실제 컬럼명이 확정되면 아래 QUERY 문자열의 "INITIAL_SEED_MONEY" 부분만 고치면 됨.
@Repository
public class PortfolioSnapshotRankingRepository {

    private final JdbcTemplate jdbcTemplate;

    public PortfolioSnapshotRankingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String QUERY = """
            SELECT
                u.ID AS USER_ID,
                u.NICKNAME AS NICKNAME,
                u.USER_LEVEL AS USER_LEVEL,
                a.INITIAL_SEED_MONEY AS INITIAL_ASSET,
                last_snap.TOTAL_ASSET AS CURRENT_ASSET
            FROM USERS u
            JOIN ACCOUNTS a ON a.USER_ID = u.ID AND a.ACCOUNT_TYPE = 'BASIC'
            JOIN PROFILE_SETTINGS ps ON ps.USER_ID = u.ID AND ps.RETURN_RATE_PUBLIC = 1
            JOIN (
                SELECT ACCOUNT_ID, TOTAL_ASSET,
                       ROW_NUMBER() OVER (PARTITION BY ACCOUNT_ID ORDER BY SNAPSHOT_DATE DESC) AS RN
                FROM PORTFOLIO_SNAPSHOTS
            ) last_snap ON last_snap.ACCOUNT_ID = a.ID AND last_snap.RN = 1
            WHERE a.INITIAL_SEED_MONEY > 0
              AND u.STATUS != 'QUIT'
            """;

    public List<AccountReturnRateRow> findAllReturnRates() {
        return jdbcTemplate.query(QUERY, (rs, rowNum) -> new AccountReturnRateRow(
                rs.getLong("USER_ID"),
                rs.getString("NICKNAME"),
                rs.getString("USER_LEVEL"),
                rs.getBigDecimal("INITIAL_ASSET"),
                rs.getBigDecimal("CURRENT_ASSET")));
    }

    // 기간별(일간/주간/월간) 랭킹 계산용 - 계좌 ID -> 유저 정보 매핑.
    // 위 findAllReturnRates()와 동일하게 BASIC 계좌 + 수익률 공개 설정(PROFILE_SETTINGS.RETURN_RATE_PUBLIC)된 유저만 대상으로 함.
    private static final String ACCOUNT_USER_INFO_QUERY = """
            SELECT
                a.ID AS ACCOUNT_ID,
                u.ID AS USER_ID,
                u.NICKNAME AS NICKNAME,
                u.USER_LEVEL AS USER_LEVEL
            FROM USERS u
            JOIN ACCOUNTS a ON a.USER_ID = u.ID AND a.ACCOUNT_TYPE = 'BASIC'
            JOIN PROFILE_SETTINGS ps ON ps.USER_ID = u.ID AND ps.RETURN_RATE_PUBLIC = 1
            WHERE u.STATUS != 'QUIT'
            """;

    public List<AccountUserInfoRow> findAllAccountUserInfo() {
        return jdbcTemplate.query(ACCOUNT_USER_INFO_QUERY, (rs, rowNum) -> new AccountUserInfoRow(
                rs.getLong("ACCOUNT_ID"),
                rs.getLong("USER_ID"),
                rs.getString("NICKNAME"),
                rs.getString("USER_LEVEL")));
    }
}