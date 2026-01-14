/*
 * Copyright (C) 2023-2025 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.lamprism.lampray.system.database;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Liquibase initialization configuration for database migration management.
 *
 * @author RollW
 */
@Configuration
public class LiquibaseInitializeConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(LiquibaseInitializeConfiguration.class);

    private static final String CHANGELOG_PATH = "classpath:db/changelog.yaml";

    private static final String BASELINE_CONTEXT = "baseline";
    private static final String INCREMENTAL_CONTEXT = "incremental";

    /**
     * Business tables to check for legacy migration detection.
     * These tables indicate an existing database that needs migration.
     */
    private static final List<String> BUSINESS_TABLES = List.of("user");

    @Bean
    public SpringLiquibase springLiquibase(DataSource dataSource) throws Exception {
        logger.info("Initializing Liquibase database migration...");

        DatabaseStatus status = detectDatabaseStatus(dataSource);
        logger.info("Detected database status: {}", status);

        // Handle legacy migration before creating SpringLiquibase
        if (status == DatabaseStatus.LEGACY_MIGRATION) {
            handleLegacyMigrationSync(dataSource);
            // TODO: auto migration to be implemented in future versions
            throw new IllegalStateException(
                    "Legacy database detected without Liquibase tracking. Auto marked all changeSets as executed. " +
                            "You need to manually check the database schemas and ensure everything is correct, then restart the application."
            );
        }

        SpringLiquibase liquibase = createSpringLiquibase(dataSource);
        configureLiquibaseByStatus(liquibase, status);

        switch (status) {
            case NEW_INSTALL -> handleNewInstallationSync(dataSource, liquibase);
            case NORMAL_UPGRADE -> handleNormalUpgradeSync(dataSource, liquibase);
        }

        return liquibase;
    }

    /**
     * Create and configure basic SpringLiquibase instance.
     *
     * @param dataSource DataSource for database operations
     * @return Configured SpringLiquibase instance
     */
    private SpringLiquibase createSpringLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(CHANGELOG_PATH);
        liquibase.setShouldRun(true);
        return liquibase;
    }

    /**
     * Configure Liquibase contexts based on database status.
     *
     * @param liquibase SpringLiquibase instance to configure
     * @param status Database status
     */
    private void configureLiquibaseByStatus(SpringLiquibase liquibase, DatabaseStatus status) {
        switch (status) {
            case NEW_INSTALL -> configureForNewInstallation(liquibase);
            case NORMAL_UPGRADE -> configureForNormalUpgrade(liquibase);
        }
    }

    /**
     * Configure Liquibase for new installation scenario.
     * Uses the baseline context to execute only the latest complete schema.
     */
    private void configureForNewInstallation(SpringLiquibase liquibase) {
        logger.info("Configuring for NEW INSTALLATION: will execute baseline schema only.");
        liquibase.setContexts(BASELINE_CONTEXT);
    }

    /**
     * Configure Liquibase for normal upgrade scenario.
     * Applies all incremental changesets without baseline.
     */
    private void configureForNormalUpgrade(SpringLiquibase liquibase) {
        logger.info("Configuring for NORMAL UPGRADE: will execute incremental changesets.");
        liquibase.setContexts(INCREMENTAL_CONTEXT);
    }

    /**
     * Handle new installation sync after baseline execution.
     */
    private void handleNewInstallationSync(DataSource dataSource, SpringLiquibase springLiquibase) throws Exception {
        logger.info("New installation detected: syncing incremental changeSets after baseline execution...");

        // Trigger baseline execution first
        springLiquibase.afterPropertiesSet();

        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(
                    CHANGELOG_PATH.replace("classpath:", ""),
                    new ClassLoaderResourceAccessor(),
                    database
            );

            syncChangeSetsByContext(liquibase, INCREMENTAL_CONTEXT);

            logger.info("New installation sync completed: incremental changeSets marked as executed, " +
                    "only future incremental changeSets will be applied on upgrades.");
        }
    }


    private void handleNormalUpgradeSync(DataSource dataSource, SpringLiquibase springLiquibase) throws  Exception {
        logger.info("Normal upgrade detected: syncing baseline changeSets...");

        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(
                    CHANGELOG_PATH.replace("classpath:", ""),
                    new ClassLoaderResourceAccessor(),
                    database
            );
            syncChangeSetsByContext(liquibase, BASELINE_CONTEXT);
            logger.info("Normal upgrade sync completed: baseline changeSets marked as executed, " +
                    "future incremental changeSets will be applied on upgrades.");
        }
    }

    private void handleLegacyMigrationSync(DataSource dataSource) throws Exception {
        logger.info("Legacy migration detected: syncing baseline changeSets...");

        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));

            Liquibase liquibase = new Liquibase(
                    CHANGELOG_PATH.replace("classpath:", ""),
                    new ClassLoaderResourceAccessor(),
                    database
            );

            syncChangeSetsByContext(liquibase, BASELINE_CONTEXT);
            syncChangeSetsByContext(liquibase, INCREMENTAL_CONTEXT);

            logger.info("Legacy migration sync completed: baseline changeSets marked as executed, " +
                    "future incremental changeSets will be applied on upgrades.");
        }
    }

    /**
     * Sync changeSets by context using changeLogSync.
     *
     * @param liquibase Liquibase instance
     * @param context Context name
     * @throws Exception if sync fails
     */
    private void syncChangeSetsByContext(Liquibase liquibase, String context) throws Exception {
        Contexts contexts = new Contexts(context);
        LabelExpression labelExpression = new LabelExpression();
        liquibase.changeLogSync(contexts, labelExpression);
        logger.debug("Synced changeSets for context: {}", context);
    }


    /**
     * Detect the current database status to determine the appropriate migration strategy.
     *
     * @param dataSource DataSource to check
     * @return DatabaseStatus enum value
     * @throws SQLException if database access fails
     */
    private DatabaseStatus detectDatabaseStatus(DataSource dataSource) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            boolean hasLiquibaseTable = tableExists(conn, "DATABASECHANGELOG");
            boolean hasBusinessTables = hasAnyBusinessTable(conn);

            if (!hasLiquibaseTable && !hasBusinessTables) {
                return DatabaseStatus.NEW_INSTALL;
            }
            if (hasBusinessTables && !hasLiquibaseTable) {
                return DatabaseStatus.LEGACY_MIGRATION;
            }
            return DatabaseStatus.NORMAL_UPGRADE;
        }
    }

    /**
     * Check if a table exists in the database.
     *
     * @param tableName Table name to check (case-insensitive)
     * @return true if table exists, false otherwise
     * @throws SQLException if database access fails
     */
    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        try (ResultSet rs = metaData.getTables(catalog, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * Check if any business tables exist in the database.
     */
    private boolean hasAnyBusinessTable(Connection connection) throws SQLException {
        for (String table : BUSINESS_TABLES) {
            if (tableExists(connection, table)) {
                logger.debug("Found existing business table: {}", table);
                return true;
            }
        }
        return false;
    }

    /**
     * Database deployment status.
     */
    enum DatabaseStatus {
        /**
         * Fresh installation with no existing tables.
         * Will execute baseline schema only.
         */
        NEW_INSTALL,

        /**
         * Existing database with Liquibase tracking.
         * Will execute incremental changesets.
         */
        NORMAL_UPGRADE,

        /**
         * Existing database without Liquibase tracking.
         * Will sync baseline changesets and enable future migrations.
         */
        LEGACY_MIGRATION
    }
}

