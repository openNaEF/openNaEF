package voss.nms.inventory.util;

import naef.dto.NodeDto;
import naef.dto.PortDto;
import naef.dto.pos.PosApsIfDto;
import naef.dto.pos.PosPortDto;
import naef.dto.serial.SerialPortDto;
import naef.dto.serial.TdmSerialIfDto;
import voss.core.server.util.NodeUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelPortUtil {
    public static final String KEY_CHANNEL_PORT = "serial";

    public static List<TdmSerialIfDto> getChannelPorts(PortDto port) {
        List<TdmSerialIfDto> result = new ArrayList<TdmSerialIfDto>();
        if (!isChannelEnablePort(port)) {
            return result;
        }
        for (PortDto p : NodeUtil.getPorts(port)) {
            if (p instanceof TdmSerialIfDto) {
                result.add((TdmSerialIfDto) p);
            }
        }
        return result;
    }

    public static List<PortDto> getChannelEnabledPortsOn(NodeDto node) {
        List<PortDto> result = new ArrayList<PortDto>();
        for (PortDto p : node.getPorts()) {
            if (isChannelEnablePort(p)) {
                result.add(p);
            }
        }
        return result;
    }

    public static boolean isChannelEnablePort(PortDto port) {
        if (port == null) {
            return false;
        } else if (port instanceof SerialPortDto) {
            return true;
        } else if (port instanceof PosPortDto) {
            return true;
        } else if (port instanceof PosApsIfDto) {
            return true;
        }
        return false;
    }

    public static boolean isIntersectTimeslot(String ts1, String ts2) {
        List<Integer> tsList1 = getTimeSlotList(ts1);
        List<Integer> tsList2 = getTimeSlotList(ts2);
        tsList1.retainAll(tsList2);
        return tsList1.size() > 0;
    }

    public static List<Integer> getTimeSlotList(String ts) {
        String[] arr = ts.split(",");
        List<Integer> result = new ArrayList<Integer>();
        for (String s : arr) {
            if (s.indexOf('-') > -1) {
                String[] arr2 = s.split("-");
                if (arr2.length != 2) {
                    throw new IllegalArgumentException("illegal timeslot format. " + ts);
                }
                int begin = Integer.parseInt(arr2[0]);
                int end = Integer.parseInt(arr2[1]);
                for (int i = begin; i <= end; i++) {
                    result.add(Integer.valueOf(i));
                }
            } else {
                Integer i = Integer.valueOf(s);
                result.add(i);
            }
        }
        Collections.sort(result);
        return result;
    }
}