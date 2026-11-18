package qasystem.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Owns the connection to the database and the schema (the tables).
 *
 * WHERE the data lives is decided here, in one place:
 *   - by default: a local H2 database file in the user's home folder
 *   - if the environment variable QA_DB_URL is set: that database instead
 *     (e.g. a shared online PostgreSQL server - nothing else in the app changes)
 */
public final class Database implements AutoCloseable {

    /**
     * H2 settings that make it behave like PostgreSQL, so the same SQL
     * works today (local H2) and later (online PostgreSQL).
     */
    private static final String H2_OPTIONS =
            ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    /** Folder in the user's home directory, e.g. C:\Users\Hadi\.qasystem */
    private static final Path DATA_FOLDER = Path.of(System.getProperty("user.home"), ".qasystem");

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;
    private boolean inTransaction;

    private Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        createTables();
    }

    // =====================================================================
    // Opening
    // =====================================================================

    /**
     * The app's database. "name" picks the file, so dev mode can use its own
     * database ("qasystem-dev") without touching the real one.
     */
    public static Database open(String name) {
        String serverUrl = System.getenv("QA_DB_URL");
        if (serverUrl != null && !serverUrl.isBlank()) {
            return new Database(serverUrl, System.getenv("QA_DB_USER"), System.getenv("QA_DB_PASSWORD"));
        }
        return openFile(DATA_FOLDER.resolve(name));
    }

    /** A local H2 database stored in this file (H2 adds ".mv.db" to the name). */
    public static Database openFile(Path file) {
        // H2 wants forward slashes, even on Windows.
        String path = file.toAbsolutePath().toString().replace('\\', '/');
        return new Database("jdbc:h2:file:" + path + H2_OPTIONS, "sa", "");
    }

    /** A fresh, empty database that lives only in memory. Used by the JUnit tests. */
    public static Database inMemory() {
        return new Database("jdbc:h2:mem:test-" + UUID.randomUUID() + H2_OPTIONS, "sa", "");
    }

    /** Human-readable location, shown in error messages. */
    public String describeLocation() {
        return url.startsWith("jdbc:h2:file:") ? url.substring("jdbc:h2:file:".length(), url.indexOf(';')) : url;
    }

    // =====================================================================
    // Running SQL
    // =====================================================================

    /** A piece of database work that receives the connection. */
    @FunctionalInterface
    public interface Work<T> {
        T run(Connection connection) throws SQLException;
    }

    /** Runs one piece of database work (auto-committed on its own). */
    public <T> T query(Work<T> work) {
        try {
            return work.run(connection());
        } catch (SQLException e) {
            throw new DataAccessException("Database error", e);
        }
    }

    /**
     * Runs several steps as ONE unit: either all of them are saved, or none.
     * If anything inside throws, everything is rolled back.
     * Calls nested inside an existing transaction simply join it.
     */
    public <T> T transaction(Work<T> work) {
        if (inTransaction) {
            return query(work); // already inside a transaction: join it
        }
        Connection c = connection();
        try {
            c.setAutoCommit(false);
            inTransaction = true;
            T result = work.run(c);
            c.commit();
            return result;
        } catch (SQLException e) {
            rollbackQuietly(c);
            throw new DataAccessException("Database error", e);
        } catch (RuntimeException e) {
            rollbackQuietly(c);
            throw e;
        } finally {
            inTransaction = false;
            try {
                c.setAutoCommit(true);
            } catch (SQLException ignored) {
                // the connection is broken; connection() will reopen it next time
            }
        }
    }

    /** "Unique key already exists", e.g. a username that is taken. Same code in H2 and PostgreSQL. */
    public static boolean isDuplicateKey(SQLException e) {
        return "23505".equals(e.getSQLState());
    }

    // =====================================================================
    // Connection and schema
    // =====================================================================

    private Connection connection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(url, user, password);
            }
            return connection;
        } catch (SQLException e) {
            throw new DataAccessException("Could not open the database at " + describeLocation(), e);
        }
    }

    /** Runs schema.sql. Every table uses CREATE TABLE IF NOT EXISTS, so this is safe on every start. */
    private void createTables() {
        String schema = readSchema();
        query(c -> {
            try (Statement statement = c.createStatement()) {
                for (String sql : schema.split(";")) {
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
            return null;
        });
    }

    private static String readSchema() {
        try (InputStream in = Database.class.getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql is missing from resources/qasystem/data");
            }
            StringBuilder sql = new StringBuilder();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.strip().startsWith("--")) { // drop comment lines
                    sql.append(line).append('\n');
                }
            }
            return sql.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read schema.sql", e);
        }
    }

    private static void rollbackQuietly(Connection c) {
        try {
            c.rollback();
        } catch (SQLException ignored) {
            // nothing more we can do
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // closing anyway
        }
    }
}
