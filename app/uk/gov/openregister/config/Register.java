package uk.gov.openregister.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.dbcp2.BasicDataSource;
import org.markdownj.MarkdownProcessor;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import uk.gov.openregister.linking.Curie;
import uk.gov.openregister.linking.CurieResolver;
import uk.gov.openregister.model.Field;
import uk.gov.openregister.store.Store;
import uk.gov.openregister.store.postgresql.DBInfo;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


public abstract class Register {

    public static final int TIMEOUT = 30000;
    private static final BasicDataSource dataSource;
    private Store store;

    static {
        try {
            URI dbUri = new URI(ApplicationConf.getString("db.default.url"));
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();
            dataSource = new BasicDataSource();

            if (dbUri.getUserInfo() != null) {
                dataSource.setUsername(dbUri.getUserInfo().split(":")[0]);
                dataSource.setPassword(dbUri.getUserInfo().split(":")[1]);
            }
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setUrl(dbUrl);
            dataSource.setInitialSize(1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String officialText;


    public final Store store() {
        if (store == null) {
            store = new PostgresqlStore(new DBInfo(name(), name().toLowerCase(), fieldNames()), dataSource);
        }
        return store;
    }

    public synchronized String officialText() {
        if(officialText == null) {
            getOfficialTextForRegister();
        }
        return officialText;
    }

    private void getOfficialTextForRegister() {
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
        String rrUrl = curieResolver.resolve(new Curie("register", name())) + "?_representation=json";
        WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

        if (rr.getStatus() == 200) {
            JsonNode rEntry = rr.asJson().get("entry");
            if (rEntry.get("text") != null) {
                final String markdownText = rEntry.get("text").textValue();
                officialText = new MarkdownProcessor().markdown(markdownText);
            }
        }
    }

    public abstract String friendlyName();

    public abstract String name();

    public abstract List<Field> fields();

    public List<String> fieldNames() {
        return fields().stream().map(Field::getName).collect(Collectors.toList());
    }
}
