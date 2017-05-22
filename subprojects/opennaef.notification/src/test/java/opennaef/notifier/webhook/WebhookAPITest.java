package opennaef.notifier.webhook;

import com.sun.net.httpserver.HttpServer;
import opennaef.notifier.MockServer;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.*;
import org.junit.runners.MethodSorters;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Webhook RESTful API test
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebhookAPITest extends JerseyTest {
    private static final int MOCK_SERVER_PORT = 1234;
    private static HttpServer MOCK_SERVER;
    private static final String CALLBACK_URL = "http://localhost:" + MOCK_SERVER_PORT;

    @Override
    protected Application configure() {
        return new WebhookApp();
    }

    @BeforeClass
    public static void beforeClass() {
        try {
            MOCK_SERVER = MockServer.start(MOCK_SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void afterClass() {
        MOCK_SERVER.stop(0);
    }

    @Test
    public void _1_create() throws Exception {
        String body = "{" +
                "\"callback_url\": \"" + CALLBACK_URL + "\"," +
                "\"active\": false" +
                "}";
        Response res = target("").request()
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(201, res.getStatus());
        assertEquals(
                "{\"active\":false,\"callback_url\":\"" + CALLBACK_URL + "\",\"failed\":false,\"filter\":{\"exists\":[],\"for_all\":[]},\"id\":1,\"message\":null}",
                res.readEntity(String.class));
    }

    @Test
    @Ignore
    public void _2_create_ping_failed() throws Exception {
        String body = "{" +
                "\"callback_url\": \"http://ping_failed/\"," +
                "\"active\": true" +
                "}";
        Response res = target("").request()
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(400, res.getStatus());
        assertEquals(
                "{\"message\":\"ping failed\"}",
                res.readEntity(String.class));
    }

    @Test
    public void _3_get1() throws Exception {
        Response res = target("").path("1").request()
                .get();
        assertEquals(200, res.getStatus());
        assertEquals(
                "{\"active\":false,\"callback_url\":\"" + CALLBACK_URL + "\",\"failed\":false,\"filter\":{\"exists\":[],\"for_all\":[]},\"id\":1,\"message\":null}",
                res.readEntity(String.class));
    }

    @Test
    public void _4_update() throws Exception {
        String body = "{" +
                "\"callback_url\": \"" + CALLBACK_URL + "/update\"," +
                "\"filter\": {\"exists\": [\"port\"]}" +
                "}";

        Response res = target("").path("1").request()
                .header("X-HTTP-Method-Override", "PATCH")
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, res.getStatus());
        assertEquals(
                "{\"active\":false,\"callback_url\":\"" + CALLBACK_URL + "/update\",\"failed\":false,\"filter\":{\"exists\":[\"port\"],\"for_all\":[]},\"id\":1,\"message\":null}",
                res.readEntity(String.class));
    }

    @Test
    @Ignore
    public void _5_update_change_callbackURL_ping_failed() throws Exception {
        String body = "{" +
                "\"callback_url\": \"http://ping_failed/\"," +
                "\"active\": false" +
                "}";

        Response res = target("").path("1").request()
                .header("X-HTTP-Method-Override", "PATCH")
                .post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(400, res.getStatus());
        assertEquals(
                "{\"message\":\"ping failed\"}",
                res.readEntity(String.class));
    }

    @Test
    public void _6_ping() throws Exception {
        Response res = target("").path("1").path("pings").request()
                .post(Entity.entity("{}", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(204, res.getStatus());
    }

    @Test
    public void _7_delete() throws Exception {
        Response res = target("").path("1").request()
                .delete();
        assertEquals(204, res.getStatus());
    }

    @Test
    public void _8_get_not_found() throws Exception {
        Response res = target("").path("1").request()
                .get();
        assertEquals(404, res.getStatus());
    }

    @Test
    public void _9_getAll() throws Exception {
        String body2 = "{\"callback_url\": \"" + CALLBACK_URL + "/2\"}";
        String body3 = "{\"callback_url\": \"" + CALLBACK_URL + "/3\"}";
        String body4 = "{\"callback_url\": \"" + CALLBACK_URL + "/4\"}";

        target("").request().post(Entity.entity(body2, MediaType.APPLICATION_JSON_TYPE));
        target("").request().post(Entity.entity(body3, MediaType.APPLICATION_JSON_TYPE));
        target("").request().post(Entity.entity(body4, MediaType.APPLICATION_JSON_TYPE));

        Response res = target("").request().get();
        assertEquals(200, res.getStatus());
        assertEquals("[" +
                        "{\"active\":true,\"callback_url\":\"" + CALLBACK_URL + "/2\",\"failed\":false,\"filter\":{\"exists\":[],\"for_all\":[]},\"id\":2,\"message\":null}," +
                        "{\"active\":true,\"callback_url\":\"" + CALLBACK_URL + "/3\",\"failed\":false,\"filter\":{\"exists\":[],\"for_all\":[]},\"id\":3,\"message\":null}," +
                        "{\"active\":true,\"callback_url\":\"" + CALLBACK_URL + "/4\",\"failed\":false,\"filter\":{\"exists\":[],\"for_all\":[]},\"id\":4,\"message\":null}" +
                        "]",
                res.readEntity(String.class));
    }
}