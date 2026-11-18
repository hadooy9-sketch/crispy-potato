package qasystem.data;

import java.sql.SQLException;

/**
 * A database problem (connection lost, bad SQL, disk full...).
 * Unchecked, so services and controllers don't need try/catch for SQLException everywhere.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, SQLException cause) {
        super(message + ": " + cause.getMessage(), cause);
    }

    /** The database's own error code, e.g. to recognize "database already in use". */
    public int getErrorCode() {
        return ((SQLException) getCause()).getErrorCode();
    }
}
