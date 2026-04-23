package com.kidbank.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;

/**
 * Converts Railway's postgresql://user:pass@host:port/db URL to JDBC format.
 * Falls back to individual PG* vars (local dev) if DATABASE_URL is not set.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource(
            @Value("${DATABASE_URL:}") String databaseUrl,
            @Value("${PGHOST:localhost}") String pgHost,
            @Value("${PGPORT:5432}") int pgPort,
            @Value("${PGDATABASE:kidbank}") String pgDatabase,
            @Value("${PGUSER:kidbank}") String pgUser,
            @Value("${PGPASSWORD:kidbank}") String pgPassword) throws Exception {

        if (!databaseUrl.isBlank()) {
            // Railway gives postgresql://user:pass@host:port/db — convert to JDBC
            URI uri = URI.create(databaseUrl);
            String[] userInfo = uri.getUserInfo().split(":", 2);
            String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
            return DataSourceBuilder.create()
                    .url(jdbcUrl)
                    .username(userInfo[0])
                    .password(userInfo.length > 1 ? userInfo[1] : "")
                    .driverClassName("org.postgresql.Driver")
                    .build();
        }

        // Local dev fallback
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDatabase)
                .username(pgUser)
                .password(pgPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}
