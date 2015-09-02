package controllers.html;

import play.mvc.Controller;
import play.mvc.Result;

public class Robots extends Controller {
    public Result robots() {
        return ok("User-agent: *\n" +
                "Disallow: /\n");
    }
}
