package functional.jsonp;

import functional.ApplicationTests;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import play.libs.ws.WSResponse;
import uk.gov.openregister.domain.Record;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class JsonpRepresentationTest extends ApplicationTests{
    @Test
    public void jsonPRepresentationForFindByHash() throws JSONException {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        Record record = new Record(json);
        postJson("/create", json);

        WSResponse response = get("/hash/" + record.getHash() + ".json?_callback=foo");
        assertThat(response.getStatus()).isEqualTo(OK);

        String body = response.getBody();

        assertThat(body).startsWith("foo(");
        JSONAssert.assertEquals(record.getEntry().toString(), json, true);
        assertThat(body).endsWith(")");
    }

    @Test
    public void jsonPRepresentationForFindByKeyValue() throws JSONException {
        String json = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
        Record record = new Record(json);
        postJson("/create", json);

        WSResponse response = get("/test-register/testregisterkey.json?_callback=foo");
        assertThat(response.getStatus()).isEqualTo(OK);

        String body = response.getBody();

        assertThat(body).startsWith("foo(");
        JSONAssert.assertEquals(record.getEntry().toString(), json, true);
        assertThat(body).endsWith(")");
    }
}
