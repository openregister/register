package uk.gov.openregister.store.elasticsearch;

public class ESInfo {
    private final String url;
    private final String key;

    public ESInfo(String url, String key) {
        this.url = url;
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }
}
