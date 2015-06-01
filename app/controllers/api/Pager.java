package controllers.api;

import play.libs.F;
import play.mvc.QueryStringBindable;

import java.util.Map;

import static play.libs.F.Option;


public class Pager implements QueryStringBindable<Pager> {
    public static final int DEFAULT_PAGE_SIZE = 30;
    public static final Pager DEFAULT_PAGER = new Pager(0,DEFAULT_PAGE_SIZE);
    public static final String PAGE_PARAM = "_page";
    public static final String PAGE_SIZE_PARAM = "_pageSize";

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
        String _page = params.containsKey(PAGE_PARAM) ? params.get(PAGE_PARAM)[0] : "0";
        String _pageSize = params.containsKey(PAGE_SIZE_PARAM) ? params.get(PAGE_SIZE_PARAM)[0] : String.valueOf(DEFAULT_PAGE_SIZE);

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
            unbound += PAGE_PARAM + "=" + page;
            separator = "&";
        }

        if(pageSize != DEFAULT_PAGE_SIZE) {
            unbound += separator + PAGE_SIZE_PARAM + "=" + pageSize;
        }
        return unbound;
    }

    @Override
    public String javascriptUnbind() {
        return null;
    }
}
