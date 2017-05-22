package voss.core.server.naming.inventory;

import junit.framework.TestCase;
import naef.dto.CustomerInfoDto;
import voss.core.server.builder.BuildResult;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.core.server.database.CoreConnector;
import voss.core.server.database.ShellConnector;

public class CustomerInfoTest extends TestCase {

    public void test() {
        try {
            CoreConnector conn = CoreConnector.getInstance();
            CustomerInfoDto dto = conn.getCustomerInfoByName("foo:baz2");
            assertTrue(dto == null);

            CustomerInfoCommandBuilder builder = new CustomerInfoCommandBuilder("hoge");
            builder.setIDType("foo");
            builder.setID("baz2");
            BuildResult result = builder.buildCommand();
            assertTrue(result == BuildResult.SUCCESS);

            ShellConnector shell = ShellConnector.getInstance();
            shell.execute(builder);

            CustomerInfoDto dto2 = conn.getCustomerInfoByName("foo:baz2");
            assertTrue(dto2 != null);

            CustomerInfoCommandBuilder builder2 = new CustomerInfoCommandBuilder(dto2, "hoge");
            BuildResult r2 = builder2.buildDeleteCommand();
            assertTrue(r2 == BuildResult.SUCCESS);

            shell.execute(builder2);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}