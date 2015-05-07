package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import controllers.conf.Register;
import play.Routes;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;
import uk.gov.openregister.config.ApplicationConf;
import uk.gov.openregister.domain.Record;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static uk.gov.openregister.config.ApplicationConf.registerPrimaryKey;

public class ImportData extends Controller {


    public static Result loadWithProgress() {
        return ok(views.html.load.render(ApplicationConf.getString("register.name"), "Data import"));
    }

    public static WebSocket<JsonNode> progress() {
        return WebSocket.whenReady((in, out) -> new Thread(() -> {
            in.onMessage(urlj -> {
                JsonNode url = urlj.get("url");
                if (url == null || url.asText().isEmpty()) notifyProgress("Failed. Invalid url provided", true, true, 0,  out);
                else readAndSaveToDb(url.asText(), out);
            });

        }).start());
    }

    private static void readAndSaveToDb(String url, WebSocket.Out<JsonNode> out) {

        new Thread(() -> {

            try {
                char cs = url.endsWith(".tsv") ? '\t' : ',';

                notifyProgress("Downloading raw data", false, false, 0, out);

                CsvMapper mapper = new CsvMapper();
                CsvSchema schema = CsvSchema.emptySchema().withColumnSeparator(cs).withHeader();
                MappingIterator<JsonNode> it = mapper.reader(JsonNode.class)
                        .with(schema)
                        .readValues(new URL(url));
                long counter = 0;

                notifyProgress("Dropping existing data", false, false, counter, out);
                Register.instance.store().deleteAll();
                while (it.hasNext()) {
                    JsonNode rowAsNode = it.next();
                    Register.instance.store().save(registerPrimaryKey, new Record(rowAsNode));
                    counter++;
                    if (counter % 1000 == 0) {
                        notifyProgress("Importing... (" + counter + " records)", false, false, counter, out);
                    }
                }

                notifyProgress("Imported successfully " + (int) counter + " records", true, true, counter, out);
            } catch (Exception e) {
                notifyProgress("Failed. " + e.getMessage(), true, false, 0, out);
                throw new RuntimeException(e);
            }
        }).run();

    }

    private static void notifyProgress(String message, boolean done, boolean success, long count,  WebSocket.Out<JsonNode> out) {

        Map<String, Object> result = new HashMap<>();
        result.put("text", message);
        result.put("count", count);
        result.put("done", done);
        result.put("success", success);

        out.write(Json.toJson(result));
    }

    public static Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
                Routes.javascriptRouter("jsRoutes",
                        // Routes
                        controllers.api.routes.javascript.ImportData.progress()
                )
        );
    }
}
