package io.mango.infra.kv.core.jdbc;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import io.mango.common.result.Require;
import io.mango.infra.kv.api.IKvSortedSet;
import io.mango.infra.kv.api.IKvStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JdbcKvStore implementation using try-UPDATE-then-INSERT pattern.
 * Avoids ON DUPLICATE KEY UPDATE for better cross-database compatibility.
 */
@Slf4j
public class JdbcKvStore implements IKvStore, IKvSortedSet {

    public static final String DEFAULT_TABLE_NAME = "infra_kv_entry";

    private static final String SORTED_SET_MEMBER_SEPARATOR = ":member:";
    private static final String SQL_ACTIVE_WHERE = " WHERE kv_key = ? AND expire_time > ?";
    private static final String SQL_ACTIVE_PREFIX_WHERE = " WHERE kv_key LIKE ? AND expire_time > ?";
    private static final String SQL_DECIMAL_SCORE = "CAST(kv_value AS DECIMAL(30, 10))";
    private static final String SQL_SIGNED_VALUE = "CAST(kv_value AS SIGNED)";

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Snowflake ID_GENERATOR = IdUtil.getSnowflake();

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;

    public JdbcKvStore(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, DEFAULT_TABLE_NAME);
    }

    public JdbcKvStore(JdbcTemplate jdbcTemplate, String tableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = normalizeTableName(tableName);
    }

    @Override
    @Transactional
    public boolean setIfAbsent(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            return putNonPositiveTtl(key);
        }
        LocalDateTime currentTime = now();
        if (findActiveValue(key, currentTime).isPresent()) {
            return false;
        }
        try {
            replaceValue(key, value, currentTime.plusSeconds(expireSeconds));
            return true;
        } catch (DuplicateKeyException e) {
            log.debug("JdbcKvStore setIfAbsent conflict, key={}", key, e);
            return false;
        }
    }

    @Override
    @Transactional
    public void set(String key, String value, long expireSeconds) {
        validateKey(key);
        if (expireSeconds <= 0) {
            putNonPositiveTtl(key);
            return;
        }
        LocalDateTime currentTime = now();
        replaceValue(key, value, currentTime.plusSeconds(expireSeconds));
    }

    @Override
    public boolean put(String key, String value, long expireSeconds) {
        return setIfAbsent(key, value, expireSeconds);
    }

    @Override
    public String get(String key) {
        validateKey(key);
        LocalDateTime currentTime = now();
        return findActiveValue(key, currentTime).orElse(null);
    }

    @Override
    @Transactional
    public long incrementBy(String key, long delta, long windowSeconds) {
        validateKey(key);
        Require.positive(windowSeconds, "windowSeconds must be positive, was: " + windowSeconds);
        LocalDateTime currentTime = now();
        LocalDateTime expireTime = currentTime.plusSeconds(windowSeconds);
        int updated = incrementExistingValue(key, delta, expireTime, currentTime);
        if (updated == 0) {
            replaceValue(key, String.valueOf(delta), expireTime);
        }
        return findLatestActiveValue(key, currentTime).map(Long::parseLong).orElse(0L);
    }

    @Override
    public long increment(String key, long windowSeconds) {
        return incrementBy(key, 1, windowSeconds);
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        deleteByKey(key);
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);
        LocalDateTime currentTime = now();
        return countActiveByKey(key, currentTime) > 0;
    }

    @Override
    public void add(String key, String member, double score, long ttlSeconds) {
        validateKey(key);
        validateKey(member);
        set(sortedSetMemberKey(key, member), String.valueOf(score), ttlSeconds);
    }

    @Override
    public void remove(String key, String member) {
        validateKey(key);
        validateKey(member);
        delete(sortedSetMemberKey(key, member));
    }

    @Override
    public Collection<String> rangeByScore(String key, double minScore, double maxScore, int limit) {
        validateKey(key);
        String keyPrefix = sortedSetMemberPrefix(key);
        LocalDateTime currentTime = now();
        return findSortedSetMembersByScore(keyPrefix, minScore, maxScore, limit, currentTime);
    }

    @Override
    public long removeByScore(String key, double minScore, double maxScore) {
        validateKey(key);
        String keyPrefix = sortedSetMemberPrefix(key);
        LocalDateTime currentTime = now();
        return deleteSortedSetMembersByScore(keyPrefix, minScore, maxScore, currentTime);
    }

    @Override
    public long size(String key) {
        validateKey(key);
        String keyPrefix = sortedSetMemberPrefix(key);
        LocalDateTime currentTime = now();
        return countActiveByPrefix(keyPrefix, currentTime);
    }

    private String sortedSetMemberKey(String key, String member) {
        return sortedSetMemberPrefix(key) + member;
    }

    private String sortedSetMemberPrefix(String key) {
        return key + SORTED_SET_MEMBER_SEPARATOR;
    }

    private String sortedSetMemberFromKey(String keyPrefix, String key) {
        return key.substring(keyPrefix.length());
    }

    private void validateKey(String key) {
        Require.notBlank(key, "key cannot be null or blank");
    }

    private java.util.Optional<String> findActiveValue(String key, LocalDateTime currentTime) {
        return jdbcTemplate.query(
                sqlSelectActiveValue(),
                (rs, rowNum) -> rs.getString("kv_value"),
                key, currentTime)
            .stream()
            .findFirst();
    }

    private java.util.Optional<String> findLatestActiveValue(String key, LocalDateTime currentTime) {
        return jdbcTemplate.query(
                sqlSelectLatestActiveValue(),
                (rs, rowNum) -> rs.getString("kv_value"),
                key, currentTime)
            .stream()
            .findFirst();
    }

    private void replaceValue(String key, String value, LocalDateTime expireTime) {
        deleteByKey(key);
        insertValue(key, value, expireTime);
    }

    private void insertValue(String key, String value, LocalDateTime expireTime) {
        jdbcTemplate.update(sqlInsertValue(), nextId(), key, value, expireTime);
    }

    private int incrementExistingValue(String key, long delta, LocalDateTime expireTime, LocalDateTime currentTime) {
        return jdbcTemplate.update(sqlIncrementActiveValue(), delta, expireTime, key, currentTime);
    }

    private void deleteByKey(String key) {
        jdbcTemplate.update(sqlDeleteByKey(), key);
    }

    private long countActiveByKey(String key, LocalDateTime currentTime) {
        return jdbcTemplate.queryForObject(sqlCountActiveByKey(), Long.class, key, currentTime);
    }

    private long countActiveByPrefix(String keyPrefix, LocalDateTime currentTime) {
        return jdbcTemplate.queryForObject(sqlCountActiveByPrefix(), Long.class, likePrefix(keyPrefix), currentTime);
    }

    private Collection<String> findSortedSetMembersByScore(String keyPrefix,
                                                           double minScore,
                                                           double maxScore,
                                                           int limit,
                                                           LocalDateTime currentTime) {
        ScoreRangeSql scoreRangeSql = scoreRangeSql(minScore, maxScore);
        List<Object> args = new ArrayList<>();
        args.add(likePrefix(keyPrefix));
        args.add(currentTime);
        args.addAll(scoreRangeSql.args());
        return jdbcTemplate.query(
            sqlSelectSortedSetMembersByScore(scoreRangeSql.whereSql(), limit),
            (rs, rowNum) -> sortedSetMemberFromKey(keyPrefix, rs.getString("kv_key")),
            args.toArray());
    }

    private long deleteSortedSetMembersByScore(String keyPrefix,
                                               double minScore,
                                               double maxScore,
                                               LocalDateTime currentTime) {
        ScoreRangeSql scoreRangeSql = scoreRangeSql(minScore, maxScore);
        List<Object> args = new ArrayList<>();
        args.add(likePrefix(keyPrefix));
        args.add(currentTime);
        args.addAll(scoreRangeSql.args());
        return jdbcTemplate.update(
            sqlDeleteSortedSetMembersByScore(scoreRangeSql.whereSql()),
            args.toArray());
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private String likePrefix(String keyPrefix) {
        return keyPrefix + "%";
    }

    private String sqlSelectActiveValue() {
        return "SELECT kv_value FROM " + tableName + SQL_ACTIVE_WHERE;
    }

    private String sqlSelectLatestActiveValue() {
        return sqlSelectActiveValue() + " ORDER BY create_time DESC LIMIT 1";
    }

    private String sqlInsertValue() {
        return "INSERT INTO " + tableName + " (id, kv_key, kv_value, expire_time) VALUES (?, ?, ?, ?)";
    }

    private String sqlDeleteByKey() {
        return "DELETE FROM " + tableName + " WHERE kv_key = ?";
    }

    private String sqlIncrementActiveValue() {
        return "UPDATE " + tableName + " SET kv_value = " + SQL_SIGNED_VALUE + " + ?, expire_time = ?"
            + SQL_ACTIVE_WHERE;
    }

    private String sqlCountActiveByKey() {
        return "SELECT COUNT(*) FROM " + tableName + SQL_ACTIVE_WHERE;
    }

    private String sqlCountActiveByPrefix() {
        return "SELECT COUNT(*) FROM " + tableName + SQL_ACTIVE_PREFIX_WHERE;
    }

    private String sqlSelectSortedSetMembersByScore(String scoreWhereSql, int limit) {
        String limitSql = limit > 0 ? " LIMIT " + limit : "";
        return "SELECT kv_key FROM " + tableName
            + SQL_ACTIVE_PREFIX_WHERE
            + scoreWhereSql
            + " ORDER BY " + SQL_DECIMAL_SCORE + " ASC, kv_key ASC"
            + limitSql;
    }

    private String sqlDeleteSortedSetMembersByScore(String scoreWhereSql) {
        return "DELETE FROM " + tableName
            + SQL_ACTIVE_PREFIX_WHERE
            + scoreWhereSql;
    }

    private ScoreRangeSql scoreRangeSql(double minScore, double maxScore) {
        Require.isTrue(!Double.isNaN(minScore), "minScore cannot be NaN");
        Require.isTrue(!Double.isNaN(maxScore), "maxScore cannot be NaN");
        Require.isTrue(minScore <= maxScore, "minScore cannot be greater than maxScore");
        StringBuilder whereSql = new StringBuilder();
        List<Object> args = new ArrayList<>();
        if (minScore != Double.NEGATIVE_INFINITY) {
            whereSql.append(" AND ").append(SQL_DECIMAL_SCORE).append(" >= ?");
            args.add(minScore);
        }
        if (maxScore != Double.POSITIVE_INFINITY) {
            whereSql.append(" AND ").append(SQL_DECIMAL_SCORE).append(" <= ?");
            args.add(maxScore);
        }
        return new ScoreRangeSql(whereSql.toString(), args);
    }

    private long nextId() {
        return ID_GENERATOR.nextId();
    }

    private String normalizeTableName(String configuredTableName) {
        String candidate = DEFAULT_TABLE_NAME;
        if (configuredTableName != null && !configuredTableName.isBlank()) {
            candidate = configuredTableName.trim();
        }
        Require.isTrue(TABLE_NAME_PATTERN.matcher(candidate).matches(),
                "tableName must match [A-Za-z_][A-Za-z0-9_]*");
        return candidate;
    }

    private record ScoreRangeSql(String whereSql, List<Object> args) {
    }
}
