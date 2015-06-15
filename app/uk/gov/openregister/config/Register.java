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
import uk.gov.openregister.store.elasticsearch.ESInfo;
import uk.gov.openregister.store.elasticsearch.ElasticSearchImporter;
import uk.gov.openregister.store.postgresql.DBInfo;
import uk.gov.openregister.store.postgresql.PostgresqlStore;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


public abstract class Register {

    public static final int TIMEOUT = 30000;
    private static final BasicDataSource dataSource;
    private Store store;
    private ElasticSearchImporter elasticSearchImporter;

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
    private String copyright;
    private String registry;
    private String registryName;
    private String crestName;
    private String officialColour;

    public final Store store() {
        if (store == null) {
            store = new PostgresqlStore(new DBInfo(name(), name().toLowerCase(), fieldNames()), dataSource);
        }

        if (elasticSearchImporter == null) {
            elasticSearchImporter = new ElasticSearchImporter(store, new ESInfo(ApplicationConf.getString("es.url"), name().toLowerCase()));
        }

        return store;
    }

    public synchronized String officialText() {
        if(officialText == null) {
            getOfficialTextForRegister();
        }
        return officialText;
    }

    public synchronized String copyright() {
        if(copyright == null) {
            getOfficialTextForRegister();
        }
        return copyright;
    }

    public synchronized String registry() {
        if(registry == null) {
            getOfficialTextForRegister();
        }
        return registry;
    }

    public synchronized String registryName() {
        if(registryName == null) {
            getRegistryDetails();
        }
        return registryName;
    }

    public synchronized String crestName() {
        if(crestName == null) {
            getRegistryDetails();
        }
        return crestName;
    }

    public synchronized String officialColour() {
        if(officialColour == null) {
            getRegistryDetails();
        }
        return officialColour;
    }

    private void getOfficialTextForRegister() {
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
        String rrUrl = curieResolver.resolve(new Curie("register", name())) + ".json";
        WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

        if (rr.getStatus() == 200) {
            JsonNode rEntry = rr.asJson().get("entry");
            if (rEntry.get("text") != null) {
                final String markdownText = rEntry.get("text").textValue();
                officialText = new MarkdownProcessor().markdown(markdownText);
            }
            if (rEntry.get("copyright") != null) {
                final String markdownText = rEntry.get("copyright").textValue();
                copyright = new MarkdownProcessor().markdown(markdownText);
            }
            if (rEntry.get("registry") != null) {
                registry = rEntry.get("registry").textValue();
            }
        }
    }

    private void getRegistryDetails() {
        CurieResolver curieResolver = new CurieResolver(ApplicationConf.getRegisterServiceTemplateUrl());
        String rrUrl = curieResolver.resolve(new Curie("public-body", registry())) + "?_representation=json";
        WSResponse rr = WS.client().url(rrUrl).execute().get(TIMEOUT);

        if (rr.getStatus() == 200) {
            JsonNode rEntry = rr.asJson().get("entry");
            if (rEntry.get("name") != null) {
                registryName = rEntry.get("name").textValue();
            }
            if (rEntry.get("crest") != null) {
                crestName = rEntry.get("crest").textValue();
            }
            if (rEntry.get("official-colour") != null) {
                officialColour = rEntry.get("official-colour").textValue();
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
