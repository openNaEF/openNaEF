package voss.discovery.agent.vmware.test;

import voss.discovery.agent.vmware.collector.CollectorService;

public class GuestInfoTest {
    public static void main(String[] args) throws Exception {
        String serverName = "example.com";
        String userName = "hoge";
        String password = "fuga";

        String macAddr = "00:50:56:00:00:00";

        CollectorService service = new CollectorService(serverName, userName, password);
        System.out.println(service.getIpAddress(macAddr));
    }
}