package voss.discovery.agent.atmbridge;

import voss.discovery.iolib.AbortedException;
import voss.discovery.iolib.console.ConsoleAccess;
import voss.discovery.iolib.console.ConsoleException;
import voss.discovery.iolib.snmp.SnmpAccess;
import voss.model.AtmQosType;
import voss.model.EAConverter;
import voss.model.EthernetPort;
import voss.model.value.PortSpeedValue;

import java.io.IOException;

public interface EAConverterTelnetUtil {

    String getModelName(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String getOSTypeName(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String getOSVersion(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String getBasePhysicalAddress(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String getGatewayAddress(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String[] getTrapReceiverAddresses(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String[] getSyslogServerAddresses(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String getSysIpAddress(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    String getSysIpSubnetMask(ConsoleAccess telnet, String ipAddress) throws IOException, ConsoleException, AbortedException;


    int[] getChassisEthernetPortIndexs(ConsoleAccess telnet) throws IOException, ConsoleException, AbortedException;

    int[] getModuleEthernetPortIndexs(ConsoleAccess telnet, String slotName) throws IOException, ConsoleException, AbortedException;


    EthernetPort.Duplex getEthernetPortDuplex(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    boolean hasAggregationNameAndID(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    int getAggregationID(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    String getAggregationName(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    String getPhysicalPortType(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    String getPhysicalPortName(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    PortSpeedValue.Admin getPortAdminSpeed(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    String getPortAdminStatus(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    String getPortIfDescr(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    String getPortOperationalStatus(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    PortSpeedValue.Oper getPortSpeed(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    String getPortStatus(ConsoleAccess telnet, String ifName) throws IOException, ConsoleException, AbortedException;

    int[] getAtmPhysicalPortIndexs(ConsoleAccess telnet, String slotName) throws IOException, ConsoleException, AbortedException;

    String[] getAtmPVCs(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    int getVpi(String pvc) throws IOException, ConsoleException, AbortedException;

    int getVci(String pvc) throws IOException, ConsoleException, AbortedException;

    String getAtmPVCOperStatus(ConsoleAccess telnet, String pvc, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPVCAdminStatus(ConsoleAccess telnet, String pvc, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    AtmQosType getQos(ConsoleAccess telnet, String pvc, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    long getPcr(ConsoleAccess telnet, String pvc, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    long getMcr(ConsoleAccess telnet, String pvc, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    int getBridgePortNumber(ConsoleAccess telnet, String pvc, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getBridgeOperStatus(ConsoleAccess telnet, int bridgePortNumber) throws IOException, ConsoleException, AbortedException;

    String getBridgeAdminStatus(ConsoleAccess telnet, int bridgePortNumber) throws IOException, ConsoleException, AbortedException;

    String getBridgePortName(ConsoleAccess telnet, int bridgePortNumber) throws IOException, ConsoleException, AbortedException;


    String[] getVlanIfName(ConsoleAccess telnet, int bridgePortNumber) throws IOException, ConsoleException, AbortedException;

    int getVlanEncapsIfTag(ConsoleAccess telnet, String vlanIfName) throws IOException, ConsoleException, AbortedException;

    String[] getEthernetIfNames(ConsoleAccess telnet, String vlanIfName) throws IOException, ConsoleException, AbortedException;


    String getAtmPhysicalPortType(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPhysicalPortName(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPhysicalPortAdminSpeed(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPhysicalPortAdminStatus(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPhysicalPortIfDescr(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPhysicalPortOperationalStatus(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPhysicalPortSpeed(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    String getAtmPhysicalPortStatus(ConsoleAccess telnet, String atmPhysicalPortIfName) throws IOException, ConsoleException, AbortedException;

    int getBridgePortNumber(ConsoleAccess telnet, String ethernetIfName) throws IOException, ConsoleException, AbortedException;

    boolean isTaggedVlan(ConsoleAccess telnet, String vlanIfName, int bridgePortIndex) throws IOException, ConsoleException, AbortedException;

    String getVlanIfDescr(ConsoleAccess telnet, String vlanIfName) throws IOException, ConsoleException, AbortedException;

    void supplementInterfaceAttributes(SnmpAccess snmp, EAConverter ea) throws IOException, ConsoleException, AbortedException;
}
