package controllers.html;

import controllers.api.Pager;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.Optional;

public class Pagination {
    private URIBuilder uriBuilder;
    private int page;
    private int pageSize;
    private long total;

    public Pagination(String uri, int page, int pageSize, long total) throws URISyntaxException {
        this.uriBuilder = new URIBuilder(uri);
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public Optional<String> linkToPreviousPage() throws URISyntaxException {
        return Optional.ofNullable(page > 1 ? uriBuilder.setParameter(Pager.PAGE_PARAM, "" + (page - 1)).build().toString() : null);
    }

    public Optional<String> linkToNextPage() throws URISyntaxException {
        return Optional.ofNullable(getTotalPages() > page ? uriBuilder.setParameter(Pager.PAGE_PARAM, "" + (page + 1)).build().toString() : null);
    }

    public int currentPage() {
        return page;
    }

    public long getTotalPages() {
        if (total % pageSize != 0) return (total / pageSize) + 1;
        else return total / pageSize;
    }

    public boolean pageDoesNotExist() {
        return page > getTotalPages();
    }

}
