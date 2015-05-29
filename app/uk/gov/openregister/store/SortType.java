package uk.gov.openregister.store;

public interface SortType {
    interface SortBy {
        String sortBy();
    }

    SortBy getDefault();
    SortBy getLastUpdate();
}
