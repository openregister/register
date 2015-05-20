package uk.gov.openregister.store;

public class DatabaseConflictException extends DatabaseException {
    public DatabaseConflictException(String msg) {
        super(msg);
    }
}
