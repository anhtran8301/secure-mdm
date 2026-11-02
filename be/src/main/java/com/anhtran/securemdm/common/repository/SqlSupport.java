package com.anhtran.securemdm.common.repository;

import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Base for custom repository fragments that need hand-written SQL (row locks, upserts, joins). Rows
 * are mapped with Spring Data's converter, so snake_case columns bind to camelCase fields.
 */
public abstract class SqlSupport {
  private final DatabaseClient client;
  private final R2dbcConverter converter;

  protected SqlSupport(R2dbcEntityTemplate template) {
    this.client = template.getDatabaseClient();
    this.converter = template.getConverter();
  }

  private DatabaseClient.GenericExecuteSpec sql(String sql, Object... args) {
    var spec = client.sql(sql);
    for (int i = 0; i < args.length; i++) {
      spec = args[i] == null ? spec.bindNull(i, String.class) : spec.bind(i, args[i]);
    }
    return spec;
  }

  protected Mono<Long> execute(String sql, Object... args) {
    return sql(sql, args).fetch().rowsUpdated();
  }

  protected <T> Flux<T> all(Class<T> type, String sql, Object... args) {
    return sql(sql, args).map((row, metadata) -> converter.read(type, row, metadata)).all();
  }

  protected <T> Mono<T> one(Class<T> type, String sql, Object... args) {
    return all(type, sql, args).singleOrEmpty();
  }

  protected Mono<Long> count(String sql, Object... args) {
    return sql(sql, args).map(row -> row.get(0, Long.class)).one();
  }
}
