package controllers.api;

import play.libs.F;
import play.mvc.QueryStringBindable;

import java.util.Map;

import static play.libs.F.Option;


public class Pager implements QueryStringBindable<Pager> {
    public final static Pager defaultPager = new Pager(0,30);

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
        String _pageSize = params.containsKey("_pageSize") ? params.get("_pageSize")[0] : "30";

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
        return "_page=" + page + "&" + "_pageSize=" + pageSize;
    }

    @Override
    public String javascriptUnbind() {
        return null;
    }
}
