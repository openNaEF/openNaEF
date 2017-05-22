package opennaef.rest.api.resource;

import opennaef.rest.api.App;
import opennaef.rest.api.NaefRestApi;
import net.arnx.jsonic.JSON;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class ConfigurableMvoApiTest extends JerseyTest {
    private static final List<String> mvoLinkKeys = Arrays.asList("id", "object_type_name", "name", "if_name", "href", "rel");
    private static final Pattern uriPattern = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");

    private static final String NODE_A = "nodeA";
    private static final String NODE_B = "nodeB";

    private static final String PORT_1 = "eth1";
    private static final String PORT_2 = "eth2";

    @BeforeClass
    public static void BeforeClass() {
        System.setProperty("_debug", "DEAD_BEEF");

        String naefPath = "./src/test/resources/naef";
        System.setProperty("tef-working-directory", naefPath);

        Path naefLog = Paths.get(naefPath, "logs");
        try {
            Files.walkFileTree(naefLog, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
        App.startNaef();
    }

    private static void deleteDir(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDir(child);
            }
        }
        file.delete();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() {
        return new GrizzlyWebTestContainerFactory();
    }

    @Override
    protected DeploymentContext configureDeployment() {
        ResourceConfig config = new NaefRestApi();
        return ServletDeploymentContext.forServlet(new ServletContainer(config)).build();
    }

    @Test
    public void 新規作成系() throws Exception {
        // node を 2つ 新規作成する
        // node に port を 2つ 新規作成する
        // port にリンクをはる

        // nodeA 新規作成 + port を 2つ 新規作成
        Map<String, Object> nodeA = createNode(NODE_A);
        Map<String, Object> nodeA_eth1 = createPort(nodeA, PORT_1);
        Map<String, Object> nodeA_eth2 = createPort(nodeA, PORT_2);

        // nodeB 新規作成 + port を 2つ 新規作成
        Map<String, Object> nodeB = createNode(NODE_B);
        Map<String, Object> nodeB_eth1 = createPort(nodeB, PORT_1);
        Map<String, Object> nodeB_eth2 = createPort(nodeB, PORT_2);

        // link を2つ作成
        Map<String, Object> link1 = createLink("link1", nodeA_eth1, nodeB_eth1);
        Map<String, Object> link2 = createLink("link2", nodeA_eth2, nodeB_eth2);
    }

    public Map<String, Object> createNode(String nodeName) {
        final Response createNode = target("nodes").request().post(Entity.json(JSON.encode(nodeAttrs(nodeName))));
        assertEquals(201, createNode.getStatus());
        String location = createNode.getHeaderString("Location");
        assertTrue(uriPattern.matcher(location).matches());
        String nodeId = location.substring(location.lastIndexOf("/") + 1);

        final Response node = target("nodes/" + nodeId).request().get();
        assertEquals(200, node.getStatus());
        return assertNode(node, nodeName);
    }

    public static Map<String, Object> assertNode(Response res, String nodeName) {
        Map<String, Object> actual = JSON.decode(res.readEntity(String.class));

        Map<String, Object> expected = nodeAttrs(nodeName);
        expected.entrySet()
                .forEach(e -> {
                    String key = e.getKey();
                    assertTrue(actual.containsKey(key));
                    assertEquals(e.getValue(), actual.get(key));
                });
        return actual;
    }

    public static Map<String, Object> nodeAttrs(String nodeName) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("name", nodeName);
        attrs.put("vendor", "vendor");
        attrs.put("node_type", "node-type");
        attrs.put("os_type", "os");
        attrs.put("os_version", "v1.0.0");
        attrs.put("management_ip_address", "127.0.0.1");
        attrs.put("snmp_mode", "SNMP v1");
        attrs.put("snmp_comunity", "public");
        attrs.put("login_user", "user");
        attrs.put("login_passward", "*****");
        attrs.put("admin_user", "admin");
        attrs.put("admin_passward", "*****");
        attrs.put("cli_mode", "SSH");
        attrs.put("virtualized_hosting_enable", "true");
        attrs.put("purpose", "purpose");
        attrs.put("note", "note");
        return attrs;
    }

    public Map<String, Object> createPort(Map<String, Object> node, String ifName) {
        List<Map<String, String>> chassis = (List<Map<String, String>>) node.get("chassis");
        String chassisId = chassis.get(0).get("id");
        final Response createPort = target("ports").request().post(Entity.json(JSON.encode(portAttrs(chassisId, ifName))));
        System.out.println(createPort.readEntity(String.class));
        assertEquals(201, createPort.getStatus());
        String location = createPort.getHeaderString("Location");
        assertTrue(uriPattern.matcher(location).matches());
        String portId = location.substring(location.lastIndexOf("/") + 1);

        final Response port = target("ports/" + portId).request().get();
        assertEquals(200, port.getStatus());
        return assertPort(port, chassisId, ifName);
    }

    public static Map<String, Object> portAttrs(String chassisId, String ifName) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("name", ifName);
        attrs.put("if_name", ifName);
        attrs.put("owner", chassisId);
        attrs.put("port_type", "Ethernet");
        attrs.put("port_mode", "IP");
        return attrs;
    }

    public static Map<String, Object> assertPort(Response res, String chassisId, String ifName) {
        Map<String, Object> actual = JSON.decode(res.readEntity(String.class));

        Map<String, Object> expected = portAttrs(chassisId, ifName);
        expected.entrySet().stream()
                .filter(e -> {
                    String key = e.getKey();
                    return !key.equals("name") && !key.equals("owner");
                })
                .forEach(e -> {
                    String key = e.getKey();
                    assertTrue(actual.containsKey(key));
                    assertEquals(e.getValue(), actual.get(key));
                });
        return actual;
    }

    public Map<String, Object> createLink(String name, Map<String, Object> port1, Map<String, Object> port2) {
        String port1id = (String) port1.get("id");
        String port2id = (String) port2.get("id");
        final Response createLink = target("eth-links").request().post(Entity.json(JSON.encode(linkAttr(name, port1id, port2id))));
        assertEquals(201, createLink.getStatus());
        String location = createLink.getHeaderString("Location");
        assertTrue(uriPattern.matcher(location).matches());
        String linkId = location.substring(location.lastIndexOf("/") + 1);

        final Response link = target("eth-links/" + linkId).request().get();
        assertEquals(200, link.getStatus());
        return assertLink(link, name, port1id, port2id);
    }

    public static Map<String, Object> linkAttr(String linkName, String port1, String port2) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("name", linkName);
        List<String> memberPorts = new ArrayList<>();
        memberPorts.add(port1);
        memberPorts.add(port2);
        attrs.put("member_ports", memberPorts);
        return attrs;
    }

    public static Map<String, Object> assertLink(Response res, String name, String port1, String port2) {
        Map<String, Object> actual = JSON.decode(res.readEntity(String.class));

        assertEquals(name, actual.get("name"));

        List<Map<String, String>> memberPorts = (List<Map<String, String>>) actual.get("member_ports");
        long ports = memberPorts.stream().filter(p -> {
            String id = p.get("id");
            return id.equals(port1) || id.equals(port2);
        }).count();
        assertEquals(2, memberPorts.size());
        assertEquals(2, ports);
        return actual;
    }

//    @Test
//    public void node全件取得() throws Exception {
//        final Response response = target("nodes").request().get();
//        assertEquals(response.getStatus(), 200);
//
//        final String resEntity = response.readEntity(String.class);
//        List<Map<String, String>> entity = JSON.decode(resEntity);
//        entity.parallelStream()
//                .forEach(ConfigurableMvoApiTest::mvoLinkCheck);
//    }
//
//    @Test
//    public void port全件取得() throws Exception {
//        final Response response = target("ports").request().get();
//        assertEquals(response.getStatus(), 200);
//
//        final String resEntity = response.readEntity(String.class);
//        List<Map<String, String>> entity = JSON.decode(resEntity);
//        entity.parallelStream()
//                .forEach(ConfigurableMvoApiTest::mvoLinkCheck);
//    }
//
//    public static void mvoLinkCheck(Map<String, String> mvoLink) {
//        mvoLink.keySet().parallelStream().forEach(key -> assertTrue(mvoLinkKeys.contains(key)));
//        try {
//            new URI(mvoLink.get("href"));
//        } catch (URISyntaxException e) {
//            fail("uri 生成失敗");
//        }
//    }
}