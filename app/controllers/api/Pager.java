package controllers.api;

import play.libs.F;
import play.mvc.QueryStringBindable;

import java.util.Map;

import static play.libs.F.Option;


public class Pager implements QueryStringBindable<Pager> {
    public final static Pager DEFAULT_PAGER = new Pager(0,30);
    public static final int DEFAULT_PAGE_SIZE = 30;

    public int page;
    public int pageSize;

    public Pager() {
    }

    public Pager(int p, int ps) {
        page = p;
        pageSize = ps;
    }

    @Override
    public F.Option<Pager> bind(String key, Map<String, String[]> params) {
        String _page = params.containsKey("_page") ? params.get("_page")[0] : "0";
        String _pageSize = params.containsKey("_pageSize") ? params.get("_pageSize")[0] : String.valueOf(DEFAULT_PAGE_SIZE);

        try {
            page = Integer.parseInt(_page);
            pageSize = Integer.parseInt(_pageSize);
            return Option.<Pager>Some(this);
        } catch (NumberFormatException e) {
            return Option.<Pager>None();
        }
    }

    @Override
    public String unbind(String key) {
        String separator = "";
        String unbound = "";
        if (page > 0) {
            unbound += "_page=" + page;
            separator = "&";
        }

        if(pageSize != 30) {
            unbound += separator + "_pageSize=" + pageSize;
        }
        return unbound;
    }

    @Override
    public String javascriptUnbind() {
        return null;
    }
}
