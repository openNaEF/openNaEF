package pasaran;


import naef.mvo.Node;
import naef.mvo.eth.EthLink;
import naef.mvo.eth.EthPort;
import tef.TransactionContext;
import tef.TransactionId;
import voss.mplsnms.MplsnmsNaefService;

import java.util.Objects;


/**
 * Naefを起動後、Jettyが立ち上がる
 * web フォルダがドキュメントルートとして扱われる
 */
public class Debug {
    public static void main(String[] args) throws Exception {
        startNaef();

        TransactionId.W tx = TransactionContext.beginWriteTransaction();
        try {
            TransactionContext.setTargetTime(0);
            Node node1 = new Node("node1");
            node1.putValue("initial-time", TransactionContext.getTargetTime());
            EthPort port1 = new EthPort();
            port1.putValue("initial-time", TransactionContext.getTargetTime());
            port1.setName("eth-0");
            port1.setOwner(node1);

            TransactionContext.setTargetTime(1456671600000L);
            Node node2 = new Node("node2");
            node2.putValue("initial-time", TransactionContext.getTargetTime());
            EthPort port2 = new EthPort();
            port2.putValue("initial-time", TransactionContext.getTargetTime());
            port2.setName("eth-0");
            port2.setOwner(node2);

            EthLink link12 = new EthLink(port1, port2);
            link12.putValue("initial-time", TransactionContext.getTargetTime());

            TransactionContext.setTargetTime(1456758000000L);
            Node node3 = new Node("node3");
            node3.putValue("initial-time", TransactionContext.getTargetTime());
            EthPort port3 = new EthPort();
            port3.putValue("initial-time", TransactionContext.getTargetTime());
            port3.setName("eth-0");
            port3.setOwner(node3);

            TransactionContext.setTargetTime(1456844400000L);
            Node node4 = new Node("node4");
            node4.putValue("initial-time", TransactionContext.getTargetTime());
            EthPort port4 = new EthPort();
            port4.putValue("initial-time", TransactionContext.getTargetTime());
            port4.setName("eth-0");
            port4.setOwner(node4);

            EthLink link34 = new EthLink(port3, port4);
            link34.putValue("initial-time", TransactionContext.getTargetTime());

            TransactionContext.commit();
        } finally {
            TransactionContext.close();
        }

//        TransactionId.R rtx = TransactionContext.beginReadTransaction();
//        try {
//            TransactionContext.setTargetTime(2);
//            // node1, node2 だけ取得したい
//
//            TransactionId.W targetVersion = TransactionContext.getTargetVersion();
//            long targetTime = TransactionContext.getTargetTime();
//            Node.home.list()
//                    .stream()
//                    .filter(node -> node.getInitialVersion().compareTo(targetVersion) <= 0)
//                    .filter(node -> (long) node.getValue("initial-time") <= targetTime)
//                    .sorted(Comparator.comparing((Node::getName)))
//                    .forEach(node -> System.out.println(node.getName()));
//        } finally {
//            TransactionContext.close();
//        }


    }

    public static void startNaef() throws Exception {
        // Naef 設定
        System.setProperty("voss.mplsnms.rmi-service-name", "mplsnms");
        System.setProperty("running_mode", "console");
        System.setProperty("tef-working-directory", Objects.toString(System.getProperty("tef-working-directory"), "C:\\Users\\TOSHIBA\\Desktop\\NAEFService_pasaran"));

        // Naef 起動
        new MplsnmsNaefService().start();

    }
}
