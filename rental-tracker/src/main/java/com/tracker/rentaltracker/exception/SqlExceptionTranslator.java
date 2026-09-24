package com.tracker.rentaltracker.exception;

import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.sql.SQLException;

/**
 * Converts low-level JDBC/SQLite exceptions into the app's typed exceptions,
 * so nothing above the repository layer ever needs to know a SQLite error
 * code from a hole in the ground.
 */
public final class SqlExceptionTranslator {

    private SqlExceptionTranslator() {}

    public static DatabaseException translate(SQLException e, String context) {
        if (e instanceof SQLiteException se) {
            SQLiteErrorCode code = se.getResultCode();
            return switch (code) {
                case SQLITE_CONSTRAINT_UNIQUE ->
                    new ConstraintViolationException(ConstraintViolationException.ConstraintType.UNIQUE,
                        "Unique constraint violated (" + context + "): " + e.getMessage(), e);
                case SQLITE_CONSTRAINT_NOTNULL ->
                    new ConstraintViolationException(ConstraintViolationException.ConstraintType.NOT_NULL,
                        "Not-null constraint violated (" + context + "): " + e.getMessage(), e);
                case SQLITE_CONSTRAINT_CHECK ->
                    new ConstraintViolationException(ConstraintViolationException.ConstraintType.CHECK,
                        "Check constraint violated (" + context + "): " + e.getMessage(), e);
                case SQLITE_CONSTRAINT_FOREIGNKEY ->
                    new ConstraintViolationException(ConstraintViolationException.ConstraintType.FOREIGN_KEY,
                        "Foreign key constraint violated (" + context + "): " + e.getMessage(), e);
                case SQLITE_CANTOPEN ->
                    new DatabaseConnectionException("Cannot open database (" + context + "): " + e.getMessage(), e);
                default ->
                    new ConstraintViolationException(ConstraintViolationException.ConstraintType.UNKNOWN,
                        "Database constraint violated (" + context + "): " + e.getMessage(), e);
            };
        }
        return new DatabaseConnectionException("Unexpected database error (" + context + "): " + e.getMessage(), e);
    }
}
