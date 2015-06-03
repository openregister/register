package controllers.html;

import controllers.api.Pager;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.util.Optional;

public class Pagination {
    private URIBuilder uriBuilder;
    int page;
    int total;
    int pageSize;

    public Pagination(URIBuilder uriBuilder, int page, int total, int pageSize) {
        this.uriBuilder = uriBuilder;
        this.page = page;
        this.total = total;
        this.pageSize = pageSize;
    }

    public Optional<String> linkToPreviousPage() throws URISyntaxException {
        return Optional.ofNullable(page > 0 ? uriBuilder.setParameter(Pager.PAGE_PARAM, "" + (page - 1)).build().toString() : null);
    }

    public Optional<String> linkToNextPage() throws URISyntaxException {
        return Optional.ofNullable((total / pageSize) > page ? uriBuilder.setParameter(Pager.PAGE_PARAM, "" + (page + 1)).build().toString() : null);
    }

    public int getPage() {
        return page;
    }

    public int getTotal() {
        return total;
    }

    public int getTotalPages() {
        if (total % pageSize != 0) return (total / pageSize) + 1;
        else return total / pageSize;
    }

    public boolean pageDoesNotExist() {
        return page >= getTotalPages();
    }

}
