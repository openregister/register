package controllers.api;

import play.libs.F;
import play.mvc.QueryStringBindable;

import java.util.Map;


public class Pager implements QueryStringBindable<Pager> {
    public int page;
    public int pageSize;

    public Pager(int p, int ps) {
        page = p;
        pageSize = ps;
    }


    @Override
    public F.Option<Pager> bind(String key, Map<String, String[]> params) {
        if (params.containsKey(key + "_page") && params.containsKey(key + "_pageSize")) {
            try {
                page = Integer.parseInt(params.get(key + "_page")[0]);
                pageSize = Integer.parseInt(params.get(key + "_pageSize")[0]);
                return F.Option.Some(this);
            } catch (NumberFormatException e) {
                return F.Option.None();
            }
        } else {
            return F.Option.None();
        }
    }

    @Override
    public String unbind(String key) {
        return key + "_page=" + page + "&" + key + "_pageSize=" + pageSize;
    }

    @Override
    public String javascriptUnbind() {
        return "function(k,v) {\n" +
                "    return encodeURIComponent(k+'_page')+'='+v.page+'&'+encodeURIComponent(k+'_pageSize')+'='+v.pageSize;\n" +
                "}";
    }
}
