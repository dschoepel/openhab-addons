/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.persistence.jdbc.internal.db;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.knowm.yank.Yank;
import org.knowm.yank.exceptions.YankSQLException;
import org.openhab.core.items.Item;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.persistence.FilterCriteria.Ordering;
import org.openhab.core.types.State;
import org.openhab.persistence.jdbc.internal.dto.ItemVO;
import org.openhab.persistence.jdbc.internal.dto.ItemsVO;
import org.openhab.persistence.jdbc.internal.exceptions.JdbcSQLException;
import org.openhab.persistence.jdbc.internal.utils.StringUtilsExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oracle.sql.TIMESTAMP;

/**
 * Extended Database Configuration class for Oracle Database. Class represents
 * the extended database-specific configuration. Overrides and supplements the
 * default settings from JdbcBaseDAO. Enter only the differences to JdbcBaseDAO here.
 *
 * @author Helmut Lehmeyer - Initial contribution
 * @author Mark Herwege - Implemented for Oracle DB
 */
@NonNullByDefault
public class JdbcOracleDAO extends JdbcBaseDAO {
    @SuppressWarnings("unused")
    private static final String DRIVER_CLASS_NAME = oracle.jdbc.driver.OracleDriver.class.getName();
    private static final String DATA_SOURCE_CLASS_NAME = oracle.jdbc.datasource.impl.OracleDataSource.class.getName();

    private final Logger logger = LoggerFactory.getLogger(JdbcOracleDAO.class);

    protected String sqlGetItemTableID = "SELECT itemId FROM #itemsManageTable# WHERE #colname# = ?";

    /********
     * INIT *
     ********/

    public JdbcOracleDAO() {
        initSqlTypes();
        initDbProps();
        initSqlQueries();
    }

    private void initSqlQueries() {
        logger.debug("JDBC::initSqlQueries: '{}'", this.getClass().getSimpleName());

        sqlPingDB = "SELECT 1 FROM DUAL";
        sqlGetDB = "SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL"; // Not needed, just query schema that
                                                                                // will be used
        sqlIfTableExists = "SELECT * FROM USER_TABLES WHERE TABLE_NAME = UPPER('#searchTable#')";
        sqlCreateNewEntryInItemsTable = "INSERT INTO #itemsManageTable# (ItemId, #colname#) VALUES (DEFAULT, ?)";
        sqlCreateItemsTableIfNot = """
                DECLARE
                  table_exists NUMBER;
                BEGIN
                  SELECT COUNT(*) INTO table_exists FROM USER_TABLES WHERE TABLE_NAME = UPPER('#itemsManageTable#');
                  IF table_exists = 0 THEN
                    EXECUTE IMMEDIATE 'CREATE TABLE #itemsManageTable#
                       ( ItemId NUMBER GENERATED BY DEFAULT ON NULL AS IDENTITY NOT NULL
                       , #colname# #coltype# NOT NULL
                       , CONSTRAINT #itemsManageTable#_PKEY PRIMARY KEY (ItemId)
                       )';
                  END IF;
                END;""";
        sqlDropItemsTableIfExists = """
                DECLARE
                  table_exists NUMBER;
                BEGIN
                  SELECT COUNT(*) INTO table_exists FROM USER_TABLES WHERE TABLE_NAME = UPPER('#itemsManageTable#');
                  IF table_exists = 0 THEN
                    EXECUTE IMMEDIATE 'DROP TABLE #itemsManageTable#';
                  END IF;
                END;""";
        sqlGetItemTables = "SELECT TABLE_NAME FROM USER_TABLES WHERE TABLE_NAME != UPPER('#itemsManageTable#')";
        sqlGetTableColumnTypes = "SELECT COLUMN_NAME, DATA_TYPE, NULLABLE FROM USER_TAB_COLUMNS WHERE TABLE_NAME = UPPER('#tableName#')";
        sqlCreateItemTable = """
                DECLARE
                  table_exists NUMBER;
                BEGIN
                  SELECT COUNT(*) INTO table_exists FROM USER_TABLES WHERE TABLE_NAME = UPPER('#tableName#');
                  IF table_exists = 0 THEN
                    EXECUTE IMMEDIATE 'CREATE TABLE #tableName#
                      ( time #tablePrimaryKey# NOT NULL
                      , value #dbType#
                      , CONSTRAINT #tableName#_PKEY PRIMARY KEY (time)
                      )';
                  END IF;
                END;""";
        sqlAlterTableColumn = "ALTER TABLE #tableName# MODIFY (#columnName# #columnType#)";
        sqlInsertItemValue = """
                MERGE INTO #tableName# tgt
                  USING (SELECT CAST(? AS TIMESTAMP) AS TIME, CAST(? AS #dbType#) AS VALUE FROM DUAL) src ON (tgt.TIME = src.TIME)
                  WHEN MATCHED THEN
                      UPDATE SET tgt.VALUE = src.VALUE
                  WHEN NOT MATCHED THEN
                      INSERT (TIME, VALUE) VALUES (src.TIME, src.VALUE)""";
    }

    /**
     * INFO: http://www.java2s.com/Code/Java/Database-SQL-JDBC/StandardSQLDataTypeswithTheirJavaEquivalents.htm
     */
    private void initSqlTypes() {
        sqlTypes.put("CALLITEM", "VARCHAR2(200 CHAR)");
        sqlTypes.put("COLORITEM", "VARCHAR2(70)");
        sqlTypes.put("CONTACTITEM", "VARCHAR2(6)");
        sqlTypes.put("DATETIMEITEM", "TIMESTAMP");
        sqlTypes.put("DIMMERITEM", "NUMBER(3)");
        sqlTypes.put("IMAGEITEM", "CLOB");
        sqlTypes.put("LOCATIONITEM", "VARCHAR2(50)");
        sqlTypes.put("NUMBERITEM", "FLOAT");
        sqlTypes.put("PLAYERITEM", "VARCHAR2(20)");
        sqlTypes.put("ROLLERSHUTTERITEM", "NUMBER(3)");
        // VARCHAR2 max length 32767 bytes for MAX_STRING_SIZE=EXTENDED, only 4000 bytes when MAX_STRING_SIZE=STANDARD
        // (EXTENDED is default for ADB). As default character set for ADB is AL32UTF8, it takes between 1 and 4 bytes
        // per character, where most typical characters will only take one. Therefore use a maximum of 16000 characters.
        sqlTypes.put("STRINGITEM", "VARCHAR2(16000 CHAR)");
        sqlTypes.put("SWITCHITEM", "VARCHAR2(6)");
        sqlTypes.put("tablePrimaryKey", "TIMESTAMP");
        sqlTypes.put("tablePrimaryValue", "CURRENT_TIMESTAMP");
        logger.debug("JDBC::initSqlTypes: Initialized the type array sqlTypes={}", sqlTypes.values());
    }

    /**
     * INFO: https://github.com/brettwooldridge/HikariCP
     */
    private void initDbProps() {
        // Tuning for performance and draining connection on ADB
        // See https://blogs.oracle.com/developers/post/hikaricp-best-practices-for-oracle-database-and-spring-boot
        System.setProperty("com.zaxxer.hikari.aliveBypassWindowMs", "-1");
        // Setting as system property because HikariCP as instantiated through Yank does not pass on these connection
        // properties from dataSource properties to the connection
        System.setProperty("oracle.jdbc.defaultConnectionValidation", "LOCAL");
        System.setProperty("oracle.jdbc.defaultRowPrefetch", "20");

        // Properties for HikariCP
        databaseProps.setProperty("dataSourceClassName", DATA_SOURCE_CLASS_NAME);
        databaseProps.setProperty("maximumPoolSize", "3");
        databaseProps.setProperty("minimumIdle", "2");
    }

    /**************
     * ITEMS DAOs *
     **************/

    @Override
    public Long doCreateNewEntryInItemsTable(ItemsVO vo) throws JdbcSQLException {
        String sql = StringUtilsExt.replaceArrayMerge(sqlCreateNewEntryInItemsTable,
                new String[] { "#itemsManageTable#", "#colname#" },
                new String[] { vo.getItemsManageTable(), vo.getColname() });
        Object[] params = { vo.getItemName() };
        logger.debug("JDBC::doCreateNewEntryInItemsTable sql={} item={}", sql, vo.getItemName());
        try {
            Yank.execute(sql, params);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
        // We need to return the itemId, but Yank.insert does not retrieve the value from Oracle. So do an explicit
        // query for it.
        sql = StringUtilsExt.replaceArrayMerge(sqlGetItemTableID, new String[] { "#itemsManageTable#", "#colname#" },
                new String[] { vo.getItemsManageTable(), vo.getColname() });
        logger.debug("JDBC::doGetEntryIdInItemsTable sql={}", sql);
        try {
            return Yank.queryScalar(sql, Long.class, params);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
    }

    @Override
    public ItemsVO doCreateItemsTableIfNot(ItemsVO vo) throws JdbcSQLException {
        String sql = StringUtilsExt.replaceArrayMerge(sqlCreateItemsTableIfNot,
                new String[] { "#itemsManageTable#", "#colname#", "#coltype#" },
                new String[] { vo.getItemsManageTable(), vo.getColname(), "VARCHAR2(500)" });
        logger.debug("JDBC::doCreateItemsTableIfNot sql={}", sql);
        try {
            Yank.execute(sql, null);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
        return vo;
    }

    /*************
     * ITEM DAOs *
     *************/

    @Override
    public void doUpdateItemTableNames(List<ItemVO> vol) throws JdbcSQLException {
        logger.debug("JDBC::doUpdateItemTableNames vol.size = {}", vol.size());
        for (ItemVO itemTable : vol) {
            String sql = "RENAME " + itemTable.getTableName() + "  TO " + itemTable.getNewTableName();
            logger.debug("JDBC::updateTableName sql={} oldValue='{}' newValue='{}'", sql, itemTable.getTableName(),
                    itemTable.getNewTableName());
            try {
                Yank.execute(sql, null);
            } catch (YankSQLException e) {
                throw new JdbcSQLException(e);
            }
        }
    }

    @Override
    public void doStoreItemValue(Item item, State itemState, ItemVO vo) throws JdbcSQLException {
        doStoreItemValue(item, itemState, vo, ZonedDateTime.now());
    }

    @Override
    public void doStoreItemValue(Item item, State itemState, ItemVO vo, ZonedDateTime date) throws JdbcSQLException {
        ItemVO storedVO = storeItemValueProvider(item, itemState, vo);
        String sql = StringUtilsExt.replaceArrayMerge(sqlInsertItemValue, new String[] { "#tableName#", "#dbType#" },
                new String[] { storedVO.getTableName(), storedVO.getDbType() });
        java.sql.Timestamp timestamp = new java.sql.Timestamp(date.toInstant().toEpochMilli());
        Object[] params = { timestamp, storedVO.getValue() };
        logger.debug("JDBC::doStoreItemValue sql={} value='{}'", sql, storedVO.getValue());
        try {
            Yank.execute(sql, params);
        } catch (YankSQLException e) {
            throw new JdbcSQLException(e);
        }
    }

    /****************************
     * SQL generation Providers *
     ****************************/

    @Override
    protected String histItemFilterQueryProvider(FilterCriteria filter, int numberDecimalcount, String table,
            String simpleName, ZoneId timeZone) {
        logger.debug(
                "JDBC::getHistItemFilterQueryProvider filter = {}, numberDecimalcount = {}, table = {}, simpleName = {}",
                filter, numberDecimalcount, table, simpleName);

        String filterString = resolveTimeFilter(filter, timeZone);
        filterString += (filter.getOrdering() == Ordering.ASCENDING) ? " ORDER BY time ASC" : " ORDER BY time DESC";
        if (filter.getPageSize() != Integer.MAX_VALUE) {
            filterString += " OFFSET " + filter.getPageNumber() * filter.getPageSize() + " ROWS FETCH NEXT "
                    + filter.getPageSize() + " ROWS ONLY";
        }
        // SELECT time, ROUND(value,3) FROM number_item_0114 ORDER BY time DESC OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
        // rounding HALF UP
        String queryString = "NUMBERITEM".equalsIgnoreCase(simpleName) && numberDecimalcount > -1
                ? "SELECT time, ROUND(value," + numberDecimalcount + ") FROM " + table
                : "SELECT time, value FROM " + table;
        if (!filterString.isEmpty()) {
            queryString += filterString;
        }
        logger.debug("JDBC::query queryString = {}", queryString);
        return queryString;
    }

    @Override
    protected String resolveTimeFilter(FilterCriteria filter, ZoneId timeZone) {
        String filterString = "";
        ZonedDateTime beginDate = filter.getBeginDate();
        if (beginDate != null) {
            filterString += filterString.isEmpty() ? " WHERE" : " AND";
            filterString += " TIME>=TO_TIMESTAMP('" + JDBC_DATE_FORMAT.format(beginDate.withZoneSameInstant(timeZone))
                    + "', 'YYYY-MM-dd HH24:MI:SS')";
        }
        ZonedDateTime endDate = filter.getEndDate();
        if (endDate != null) {
            filterString += filterString.isEmpty() ? " WHERE" : " AND";
            filterString += " TIME<=TO_TIMESTAMP('" + JDBC_DATE_FORMAT.format(endDate.withZoneSameInstant(timeZone))
                    + "', 'YYYY-MM-dd HH24:MI:SS')";
        }
        return filterString;
    }

    @Override
    protected Instant objectAsInstant(Object v) {
        if (v instanceof TIMESTAMP objectAsOracleTimestamp) {
            try {
                return objectAsOracleTimestamp.timestampValue().toInstant();
            } catch (SQLException e) {
                throw new UnsupportedOperationException("Date of type '" + v.getClass().getName()
                        + "', no Timestamp representation exists for '" + objectAsOracleTimestamp.toString() + "'");
            }
        } else {
            return super.objectAsInstant(v);
        }
    }
}
