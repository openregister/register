package functional.csv;

import functional.ApplicationTests;
import org.junit.Test;
import play.libs.ws.WSResponse;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class CSVSantyTest extends ApplicationTests {
    public static final String TEST_JSON_R1 = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
    public static final String TEST_JSON_R_WITH_COMMA = "{\"test-register\":\"testregisterkey\",\"name\":\"The, Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
    public static final String TEST_JSON_R_WITH_COMMA_IM_ARRAY = "{\"test-register\":\"testregisterkey\",\"name\":\"The, Entry\",\"key1\": \"value1\",\"key2\": [\"A,A1\",\"B\"]}";
    public static final String TEST_JSON_R2 = "{\"test-register\":\"testregisterkey2\",\"name\":\"The Entry2\",\"key1\": \"value2\",\"key2\": [\"C\",\"D\"]}";
    public static final String EXPECTED_HASH = "4686f89b9c983f331c7deef476fda719148de4fb";

    public static final String EXPECTED_HEADER = "hash,test-register,name,key1,key2,last-updated";
    public static final String EXPECTED_CSV_R1 = "4686f89b9c983f331c7deef476fda719148de4fb,testregisterkey,The Entry,value1,A;B,20";
    public static final String EXPECTED_CSV_R2 = "b0c762fd934019b14a3ec88d775c6a037a09a74e,testregisterkey2,The Entry2,value2,C;D,20";
    public static final String EXPECTED_CSV_R_WITH_COMMA = "afaa651dfcb7688de8b643f4c966f710687d9459,testregisterkey,\"The, Entry\",value1,A;B,20";
    public static final String EXPECTED_CSV_R_WITH_COMMA_IN_ARRAY = "d8dbf7e5d98df7e95d27d6cf1682417f0a8b000d,testregisterkey,\"The, Entry\",value1,\"A,A1;B\",20";
    public static final String EXPECTED_CSV_MEDIA_TYPE = "text/csv; charset=utf-8";
    public static final String EXPECTED_TSV_MEDIA_TYPE = "text/tab-separated-values; charset=utf-8";

    @Test
    public void testFindOneByKey() throws Exception {
        postJson("/create", TEST_JSON_R1);

        WSResponse response = getByKV("key1", "value1", "csv");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(EXPECTED_CSV_MEDIA_TYPE);
        assertThat(response.getBody()).contains(EXPECTED_CSV_R1);
    }

    @Test
    public void testTSVMediaType() throws Exception {
        postJson("/create", TEST_JSON_R1);

        WSResponse response = getByKV("key1", "value1", "tsv");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(EXPECTED_TSV_MEDIA_TYPE);
    }

    @Test
     public void testCSVHasHeader() throws Exception {
        postJson("/create", TEST_JSON_R1);

        WSResponse response = getByKV("key1", "value1", "csv");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody()).startsWith(EXPECTED_HEADER);
    }

    @Test
    public void testValueWithCommaIsEncoded() throws Exception {
        postJson("/create", TEST_JSON_R_WITH_COMMA);

        WSResponse response = getByKV("key1", "value1", "csv");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody()).contains(EXPECTED_CSV_R_WITH_COMMA);
    }

    @Test
    public void testValueWithCommaIsEncodedInArray() throws Exception {
        postJson("/create", TEST_JSON_R_WITH_COMMA_IM_ARRAY);

        WSResponse response = getByKV("key1", "value1", "csv");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getBody()).contains(EXPECTED_CSV_R_WITH_COMMA_IN_ARRAY);
    }

    @Test
    public void testFindOneByHash() throws Exception {
        postJson("/create", TEST_JSON_R1);

        WSResponse response = getByHash(EXPECTED_HASH, "csv");

        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(EXPECTED_CSV_MEDIA_TYPE);
        assertThat(response.getBody()).contains(EXPECTED_CSV_R1);
    }

    @Test
    public void testSearchAndRenderListOfResults() throws Exception {
        postJson("/create", TEST_JSON_R1);
        postJson("/create", TEST_JSON_R2);

        WSResponse response = get("/search?_query=&_representation=csv");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(EXPECTED_CSV_MEDIA_TYPE);
        assertThat(response.getBody()).contains(EXPECTED_CSV_R1);
        assertThat(response.getBody()).contains(EXPECTED_CSV_R2);
    }
}
