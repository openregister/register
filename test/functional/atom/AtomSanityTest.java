package functional.atom;

import functional.ApplicationTests;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import play.libs.ws.WSResponse;
import uk.gov.openregister.StreamUtils;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static org.fest.assertions.Assertions.assertThat;
import static play.mvc.Http.Status.OK;

public class AtomSanityTest extends ApplicationTests {
    public static final String TEST_JSON = "{\"test-register\":\"testregisterkey\",\"name\":\"The Entry\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}";
    public static final String EXPECTED_HASH = "4686f89b9c983f331c7deef476fda719148de4fb";

    public static final String EXPECTED_ATOM = "<feed xmlns:f=\"http://fields.openregister.org/field/\"\n" +
            " xmlns:dt=\"http://fields.openregister.org/datatype/\"\n" +
            " xmlns=\"http://www.w3.org/2005/Atom\">\n" +
            " <title>Test register register updates</title>\n" +
            " <id>http://localhost:8888/test-register/latest.atom</id>\n" +
            "<link rel=\"self\" href=\"http://localhost:8888/test-register/\" />\n" +
            "<updated>2015-06-02T17:14:09+01:00</updated>\n" +
            "<author><name>openregister.org</name></author>\n" +
            "<entry>\n" +
            "<id>urn:hash:4686f89b9c983f331c7deef476fda719148de4fb</id>\n" +
            "<title>http://localhost:8888/hash/4686f89b9c983f331c7deef476fda719148de4fb</title>\n" +
            "<updated>2015-06-02T17:14:09+01:00</updated>\n" +
            "<author><name>openregister.org</name></author>\n" +
            "<link href=\"http://localhost:8888/hash/4686f89b9c983f331c7deef476fda719148de4fb\"></link><content type=\"application/xml\">\n" +
            "<f:test-register>http://localhost:8888/test-register/testregisterkey</f:test-register>\n" +
            "<f:name>The Entry</f:name>\n" +
            "<f:key1>value1</f:key1>\n" +
            "<f:key2>A, B</f:key2>\n" +
            "</content>\n" +
            "</entry>\n" +
            "</feed>";
    public static final String APPLICATION_ATOM = "application/atom+xml; charset=utf-8";
    private static Element expectedRootElement;
    private static SAXBuilder jdomBuilder;

    @BeforeClass
    public static void setup() throws JDOMException, IOException {
        jdomBuilder = new SAXBuilder();
        Document expectedDocument = jdomBuilder.build(new StringReader(EXPECTED_ATOM));
        expectedRootElement = expectedDocument.getRootElement();
    }


    @Test
    public void testFindOneByKey() throws Exception {
        postJson("/create", TEST_JSON);

        WSResponse response = getByKV("key1", "value1", "atom");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(APPLICATION_ATOM);
        String actualResponse = response.getBody();
        assertThatDocumentsAreSame(expectedRootElement, actualResponse);
    }


    @Test
    public void testFindOneByHash() throws Exception {
        postJson("/create", TEST_JSON);

        WSResponse response = getByHash(EXPECTED_HASH, "atom");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(APPLICATION_ATOM);
        String actualResponse = response.getBody();
        assertThatDocumentsAreSame(expectedRootElement, actualResponse);
    }

    public static final String EXPECTED_ATOM_LIST = "<feed xmlns:f=\"http://fields.openregister.org/field/\"\n" +
            " xmlns:dt=\"http://fields.openregister.org/datatype/\"\n" +
            " xmlns=\"http://www.w3.org/2005/Atom\">\n" +
            " <title>TODO</title>\n" +
            " <id>http://localhost:8888/test-register/latest.atom</id>\n" +
            "<link rel=\"self\" href=\"http://localhost:8888/test-register/\" />\n" +
            "<updated>2015-06-02T17:18:16+01:00</updated>\n" +
            "<author><name>openregister.org</name></author>\n" +
            "<entry>\n" +
            "<id>urn:hash:39837068f586ab19bcb2b5f2408b024438e75c43</id>\n" +
            "<title>http://localhost:8888/hash/39837068f586ab19bcb2b5f2408b024438e75c43</title>\n" +
            "<updated>2015-06-02T17:18:16+01:00</updated>\n" +
            "<author><name>openregister.org</name></author>\n" +
            "<link href=\"http://localhost:8888/hash/39837068f586ab19bcb2b5f2408b024438e75c43\"></link><content type=\"application/xml\">\n" +
            "<f:test-register>http://localhost:8888/test-register/testregisterkey1</f:test-register>\n" +
            "<f:name>The Entry1</f:name>\n" +
            "<f:key1>value1</f:key1>\n" +
            "<f:key2>A, B</f:key2>\n" +
            "</content>\n" +
            "</entry>\n" +
            "<entry>\n" +
            "<id>urn:hash:b0c762fd934019b14a3ec88d775c6a037a09a74e</id>\n" +
            "<title>http://localhost:8888/hash/b0c762fd934019b14a3ec88d775c6a037a09a74e</title>\n" +
            "<updated>2015-06-02T17:18:16+01:00</updated>\n" +
            "<author><name>openregister.org</name></author>\n" +
            "<link href=\"http://localhost:8888/hash/b0c762fd934019b14a3ec88d775c6a037a09a74e\"></link><content type=\"application/xml\">\n" +
            "<f:test-register>http://localhost:8888/test-register/testregisterkey2</f:test-register>\n" +
            "<f:name>The Entry2</f:name>\n" +
            "<f:key1>value2</f:key1>\n" +
            "<f:key2>C, D</f:key2>\n" +
            "</content>\n" +
            "</entry>\n" +
            "</feed>";

    @Test
    public void testSearchAndRenderListOfResults() throws Exception {
        postJson("/create", "{\"test-register\":\"testregisterkey1\",\"name\":\"The Entry1\",\"key1\": \"value1\",\"key2\": [\"A\",\"B\"]}");
        postJson("/create", "{\"test-register\":\"testregisterkey2\",\"name\":\"The Entry2\",\"key1\": \"value2\",\"key2\": [\"C\",\"D\"]}");

        WSResponse response = get("/search?_query=&_representation=atom");
        assertThat(response.getStatus()).isEqualTo(OK);
        assertThat(response.getHeader("Content-type")).isEqualTo(APPLICATION_ATOM);
        String actualResponse = response.getBody();
        assertThatDocumentsAreSame(expectedRootElement, actualResponse);
    }

    private void assertThatDocumentsAreSame(Element expectedRootElement, String actualResponse) throws JDOMException, IOException {
        Document actualDocument = jdomBuilder.build(new StringReader(actualResponse));
        Element actualRootElement = actualDocument.getRootElement();

        assertEquals(expectedRootElement, actualRootElement, "title");
        assertEquals(expectedRootElement, actualRootElement, "id");
        assertEquals(expectedRootElement, actualRootElement, "link");
        assertEquals(expectedRootElement, actualRootElement, "author");

        List expectedEntries = expectedRootElement.getChildren("entry");
        List actualEntries = actualRootElement.getChildren("entry");

        assertThat(expectedEntries.size()).isEqualTo(actualEntries.size());
        for(int i = 0; i < expectedEntries.size(); i++) {
            assertEquals((Element)expectedEntries.get(i), (Element)actualEntries.get(i), "id");
            assertEquals((Element)expectedEntries.get(i), (Element)actualEntries.get(i), "title");
            assertEquals((Element)expectedEntries.get(i), (Element)actualEntries.get(i), "author");
            assertEquals((Element)expectedEntries.get(i), (Element)actualEntries.get(i), "link");
            assertEquals((Element)expectedEntries.get(i), (Element)actualEntries.get(i), "content");
        }
    }

    private void assertEquals(Element expectedRootElement, Element actualRootElement, String elementName) {
        Optional<Element> firstMatchingExpectedElement = StreamUtils.asStream(expectedRootElement.getDescendants())
                .filter(e -> e instanceof Element)
                .filter(e -> ((Element) e).getName().equals(elementName))
                .findFirst();

        Optional<Element> firstMatchingActualElement = StreamUtils.asStream(actualRootElement.getDescendants())
                .filter(e -> e instanceof Element)
                .filter(e -> ((Element) e).getName().equals(elementName))
                .findFirst();

        assertThat(firstMatchingExpectedElement.get().getText()).isEqualTo(firstMatchingActualElement.get().getText());
    }
}
