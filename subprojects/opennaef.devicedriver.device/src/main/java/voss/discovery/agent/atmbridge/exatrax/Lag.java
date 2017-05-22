package voss.discovery.agent.atmbridge.exatrax;

import java.util.LinkedList;

public class Lag {
    public String name = null;
    int key = -1;
    int operState = -1;
    int adminState = -1;
    public int aggregatorPort = -1;
    public LinkedList<Integer> ethernetPortList = null;

    Lag(String name) {
        this.name = name;
    }
}