package controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import controllers.BaseController;
import play.Routes;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.WebSocket;
import uk.gov.openregister.domain.Record;
import uk.gov.openregister.model.Cardinality;
import uk.gov.openregister.model.Field;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ImportData extends BaseController {

    public static final int BATCH_SIZE = 20000;

    public Result loadWithProgress() {
        return ok(views.html.load.render(register, "Data import"));
    }

    public WebSocket<JsonNode> progress() {
        return WebSocket.whenReady((in, out) -> new Thread(() -> {
            in.onMessage(urlj -> {
                JsonNode url = urlj.get("url");
                JsonNode overwriteDataNode = urlj.get("overwriteData");
                if (url == null || url.asText().isEmpty())
                    notifyProgress("Failed. Invalid url provided", true, true, 0, out);
                else {
                    boolean overwriteData = !(overwriteDataNode == null || !overwriteDataNode.asBoolean());
                    readAndSaveToDb(url.asText(), out, overwriteData);
                }
            });

        }).start());
    }

    private void readAndSaveToDb(String url, WebSocket.Out<JsonNode> out, boolean overwriteData) {
        try {

            if (url.endsWith(".zip")) {
                ZipInputStream zipInputStream = new ZipInputStream(new URL(url).openStream());
                ZipEntry entry;

                while ((entry = zipInputStream.getNextEntry()) != null) {
                    if (!entry.isDirectory()) {
                        notifyProgress("Found '" + entry.getName() + "' in zip file, importing....", false, false, 0, out);
                        importStream(entry.getName(), zipInputStream, out, overwriteData);
                    }
                }
            } else {
                importStream(url, new URL(url).openStream(), out, overwriteData);
            }

        } catch (Exception e) {
            notifyProgress("Failed. " + e.getMessage(), true, false, 0, out);
            throw new RuntimeException(e);
        }
    }

    private void importStream(String url, InputStream inputStream, WebSocket.Out<JsonNode> out, boolean overwriteData) throws java.io.IOException {

        long startTime = System.currentTimeMillis();

        notifyProgress("Downloading raw data", false, false, 0, out);

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        CsvSchema schema = getSchema(url.endsWith(".tsv"));
        MappingIterator<JsonNode> it = new CsvMapper().reader(JsonNode.class)
                .with(schema)
                .readValues(br);
        long counter = 0;

        if(overwriteData) {
            notifyProgress("Dropping existing data", false, false, counter, out);
            store.deleteAll();
        }
        List<Record> records = new ArrayList<>();

        while (it.hasNext()) {
            JsonNode rowAsNode = it.next();
            records.add(new Record(rowAsNode));
            counter++;
            if (counter % BATCH_SIZE == 0) {
                store.fastImport(records);
                records.clear();
                notifyProgress("Importing... (" + counter + " records)", false, false, counter, out);
            }
        }
        store.fastImport(records);
        long timeTaken = System.currentTimeMillis() - startTime;
        notifyProgress("Imported successfully " + (int) counter + " records, time taken: " + timeTaken + " millis" , true, true, counter, out);
    }

    private CsvSchema getSchema(boolean isTsv) {
        CsvSchema.Builder builder = CsvSchema.builder().setColumnSeparator(isTsv ? '\t' : ',').setUseHeader(true);
        if (isTsv) {
            builder = builder.disableQuoteChar();
        }
        for (Field field : register.fields()) {
            if (field.getCardinality() == Cardinality.MANY) {
                builder = builder.addArrayColumn(field.getName(), ';');
            } else {
                builder = builder.addColumn(field.getName());
            }
        }
        return builder.build();
    }

    private static void notifyProgress(String message, boolean done, boolean success, long count, WebSocket.Out<JsonNode> out) {

        Map<String, Object> result = new HashMap<>();
        result.put("text", message);
        result.put("count", count);
        result.put("done", done);
        result.put("success", success);

        out.write(Json.toJson(result));
    }

    public Result javascriptRoutes() {
        response().setContentType("text/javascript");
        return ok(
                Routes.javascriptRouter("jsRoutes",
                        // Routes
                        controllers.api.routes.javascript.ImportData.progress()
                )
        );
    }
}
