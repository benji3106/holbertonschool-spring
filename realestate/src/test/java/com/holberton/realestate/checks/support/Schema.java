package com.holberton.realestate.checks.support;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

/**
 * Reads the database through JDBC: the tables and the columns Hibernate created from your
 * entities, and the rows your API saved. Table and column names are compared without their
 * case and without their underscores, so ZIP_CODE and zipCode are the same column.
 */
public final class Schema {

    private final DataSource dataSource;

    Schema(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** One column of a table: is it nullable, and which SQL type is it. */
    public record Column(String name, boolean nullable, String type) {
    }

    public boolean hasTable(String table) {
        return tables().contains(key(table));
    }

    public Set<String> tables() {
        Set<String> names = new LinkedHashSet<>();
        run(metaData -> {
            try (ResultSet result = metaData.getTables(null, null, "%", new String[] {"TABLE"})) {
                while (result.next()) {
                    names.add(key(result.getString("TABLE_NAME")));
                }
            }
        });
        return names;
    }

    /** The columns of a table, by normalized name. Fails if the table does not exist. */
    public Map<String, Column> columns(String table) {
        Map<String, Column> columns = new LinkedHashMap<>();
        run(metaData -> {
            try (ResultSet result = metaData.getColumns(null, null, null, null)) {
                while (result.next()) {
                    if (!key(table).equals(key(result.getString("TABLE_NAME")))) {
                        continue;
                    }
                    String name = result.getString("COLUMN_NAME");
                    boolean nullable = result.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                    columns.put(key(name), new Column(name, nullable, result.getString("TYPE_NAME")));
                }
            }
        });
        if (columns.isEmpty()) {
            Expect.fail("expected a table " + table.toUpperCase() + " in the database but found "
                    + tables());
        }
        return columns;
    }

    public Column column(String table, String column) {
        Map<String, Column> columns = columns(table);
        Column found = columns.get(key(column));
        if (found == null) {
            Expect.fail("expected a column " + column.toUpperCase() + " in the table " + table.toUpperCase()
                    + " but found " + columns.keySet());
        }
        return found;
    }

    public boolean hasColumn(String table, String column) {
        return columns(table).containsKey(key(column));
    }

    /** The columns covered by a unique index or a unique constraint, one entry per column. */
    public Set<String> uniqueColumns(String table) {
        Set<String> unique = new LinkedHashSet<>();
        run(metaData -> {
            try (ResultSet result = metaData.getIndexInfo(null, null, table.toUpperCase(), true, false)) {
                while (result.next()) {
                    String column = result.getString("COLUMN_NAME");
                    if (column != null) {
                        unique.add(key(column));
                    }
                }
            }
        });
        return unique;
    }

    public Set<String> primaryKeyColumns(String table) {
        Set<String> keys = new LinkedHashSet<>();
        run(metaData -> {
            try (ResultSet result = metaData.getPrimaryKeys(null, null, table.toUpperCase())) {
                while (result.next()) {
                    keys.add(key(result.getString("COLUMN_NAME")));
                }
            }
        });
        return keys;
    }

    /** The foreign keys of a table, as "column -> other table". */
    public List<String> foreignKeys(String table) {
        List<String> links = new ArrayList<>();
        run(metaData -> {
            try (ResultSet result = metaData.getImportedKeys(null, null, table.toUpperCase())) {
                while (result.next()) {
                    links.add(key(result.getString("FKCOLUMN_NAME")) + " -> "
                            + key(result.getString("PKTABLE_NAME")));
                }
            }
        });
        return links;
    }

    public long count(String table) {
        return number("select count(*) from " + table);
    }

    public long countWhere(String table, String column, String value) {
        return number("select count(*) from " + table + " where " + column + " = '" + value + "'");
    }

    public long number(String sql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getLong(1) : 0L;
        } catch (SQLException exception) {
            throw new AssertionError("the query [" + sql + "] failed: " + exception.getMessage());
        }
    }

    private interface MetaDataReader {
        void read(DatabaseMetaData metaData) throws SQLException;
    }

    private void run(MetaDataReader reader) {
        try (Connection connection = dataSource.getConnection()) {
            reader.read(connection.getMetaData());
        } catch (SQLException exception) {
            throw new AssertionError("could not read the database: " + exception.getMessage());
        }
    }

    private static String key(String name) {
        return name == null ? "" : name.toUpperCase().replace("_", "");
    }
}
