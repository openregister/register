package uk.gov.openregister.store;

public interface SearchSpec {
    interface SearchHelper {
        String sortBy();
        boolean isHistoric();
    }

    SearchHelper getDefault();
    SearchHelper getLastUpdate();
}
