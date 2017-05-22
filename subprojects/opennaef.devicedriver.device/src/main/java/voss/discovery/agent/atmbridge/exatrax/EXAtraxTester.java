package voss.discovery.agent.atmbridge.exatrax;

import java.util.LinkedList;

class EXAtraxTester {
    public static void main(String args[]) throws Exception {
        (new ShowVersion()).test();

        for (int slot = 1; slot <= 2; slot++) {
            for (int port = 1; port <= 2; port++) {
                (new ShowAtmPvc(slot, port)).test();
            }
        }

        for (int slot = 1; slot <= 2; slot++) {
            for (int port = 1; port <= 2; port++) {
                (new ShowAtmVp(slot, port)).test();
            }
        }

        for (int port = 1; port <= 30; port++) {
            (new ShowPortSummary(port)).test();
        }

        for (int port = 1; port <= 30; port++) {
            (new ShowEther(port)).test();
        }

        LinkedList<Integer> pvcPortList = new LinkedList<Integer>();

        for (int slot = 1; slot <= 2; slot++) {
            for (int port = 1; port <= 2; port++) {
                ShowAtmPvc sap = new ShowAtmPvc(slot, port);
                LinkedList<Pvc> pvcList = sap.getPvcList();

                for (int i = 0; i < pvcList.size(); i++) {
                    Pvc pvc = (Pvc) pvcList.get(i);
                    ShowPvc sp = new ShowPvc(slot, port, pvc.vpi, pvc.vci);
                    sp.test();
                    int associatePort = sp.getAssociatePortNumber();
                    if (associatePort > 0) {
                        pvcPortList.add(associatePort);
                    }
                }
            }
        }

        for (int i = 0; i < pvcPortList.size(); i++) {
            (new ShowPortSummary(pvcPortList.get(i).intValue())).test();
        }

        for (int slot = 1; slot <= 2; slot++) {
            for (int port = 1; port <= 2; port++) {
                (new ShowAtmPhy(slot, port)).test();
            }
        }

        (new ShowVlanSummary("nag660")).test();
        (new ShowVlanSummary("nag661")).test();
        (new ShowVlanSummary("nag662")).test();

        (new ShowLagAll()).test();
        (new ShowIpRoute()).test();

        (new ShowIpAll()).test();

        (new ShowConfigRunning()).test();
    }
}