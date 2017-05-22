package voss.core.server.util;

import naef.dto.*;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.PseudowireDto;
import naef.dto.mpls.RsvpLspDto;
import naef.dto.vlan.VlanDto;
import naef.dto.vpls.VplsDto;
import naef.dto.vrf.VrfDto;
import tef.skelton.NamedModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class DtoComparator implements Comparator<NaefDto> {
    private final NodeElementComparator nodeElementComparator = new NodeElementComparator();
    private final TypeBasedPortComparator portComparator = new TypeBasedPortComparator();
    private final Map<Class<? extends NaefDto>, Integer> levelCache =
            new HashMap<Class<? extends NaefDto>, Integer>();
    private final Map<NaefDto, String> nameCache = new HashMap<NaefDto, String>();

    @Override
    public int compare(NaefDto o1, NaefDto o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return 1;
        } else if (o2 == null) {
            return -1;
        }
        int c1 = getClass(o1);
        int c2 = getClass(o2);
        if (c1 != c2) {
            return c1 - c2;
        }
        int diff = 0;
        switch (c1) {
            case 1:
                diff = compareAnonymous(o1, o2);
                break;
            case 2:
                diff = getClassName(o1).compareTo(getClassName(o2));
                if (diff == 0) {
                    diff = compareNetwork((NetworkDto) o1, (NetworkDto) o2);
                }
                break;
            case 3:
                diff = getName(o1).compareTo(getName(o2));
                break;
            case 4:
                diff = portComparator.compare((PortDto) o1, (PortDto) o2);
                break;
            case 5:
                diff = nodeElementComparator.compare((NodeElementDto) o1, (NodeElementDto) o2);
                break;
            default:
                diff = compareAnonymous(o1, o2);
        }
        return diff;
    }

    private int getClass(NaefDto dto) {
        if (dto == null) {
            return 1000;
        }
        Integer cls = levelCache.get(dto.getClass());
        if (cls != null) {
            return cls.intValue();
        }
        if (dto instanceof IdPoolDto<?, ?, ?>) {
            cls = 1;
        } else if (dto instanceof NetworkDto) {
            cls = 2;
        } else if (dto instanceof NodeDto) {
            cls = 3;
        } else if (dto instanceof PortDto) {
            cls = 4;
        } else if (dto instanceof NodeElementDto) {
            cls = 5;
        } else {
            cls = 100;
        }
        levelCache.put(dto.getClass(), cls);
        return cls;
    }

    private String getClassName(NaefDto dto) {
        return dto.getClass().getName();
    }

    private String getName(NaefDto dto) {
        String name = nameCache.get(dto);
        if (name == null) {
            if (dto instanceof NamedModel) {
                name = ((NamedModel) dto).getName();
            } else {
                name = dto.getAbsoluteName();
            }
            nameCache.put(dto, name);
        }
        return name;
    }

    private int compareAnonymous(NaefDto o1, NaefDto o2) {
        int diff = getClassName(o1).compareTo(getClassName(o2));
        if (diff == 0) {
            diff = getName(o1).compareTo(getName(o2));
        }
        return diff;
    }

    private int compareNetwork(NetworkDto n1, NetworkDto n2) {
        if (n1 instanceof PseudowireDto) {
            PseudowireDto p1 = (PseudowireDto) n1;
            PseudowireDto p2 = (PseudowireDto) n2;
            if (p1.getLongId() != null && p2.getLongId() != null) {
                if (p1.getLongId().longValue() > p2.getLongId().longValue()) {
                    return 1;
                } else if (p1.getLongId().longValue() == p2.getLongId().longValue()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (p1.getStringId() != null && p2.getStringId() != null) {
                return p1.getStringId().compareTo(p2.getStringId());
            } else {
                if (p1.getLongId() != null) {
                    return -1;
                } else {
                    return 1;
                }
            }
        } else if (n1 instanceof VlanDto) {
            VlanDto v1 = (VlanDto) n1;
            VlanDto v2 = (VlanDto) n2;
            return v1.getVlanId().intValue() - v2.getVlanId().intValue();
        } else if (n1 instanceof VplsDto) {
            VplsDto v1 = (VplsDto) n1;
            VplsDto v2 = (VplsDto) n2;
            if (v1.getIntegerId() != null && v2.getIntegerId() != null) {
                return v1.getIntegerId().intValue() - v2.getIntegerId().intValue();
            } else if (v1.getStringId() != null && v2.getStringId() != null) {
                return v1.getStringId().compareTo(v2.getStringId());
            } else {
                String v1ID = DtoUtil.getStringID(v1);
                String v2ID = DtoUtil.getStringID(v2);
                return v1ID.compareTo(v2ID);
            }
        } else if (n1 instanceof VrfDto) {
            VrfDto v1 = (VrfDto) n1;
            VrfDto v2 = (VrfDto) n2;
            if (v1.getIntegerId() != null && v2.getIntegerId() != null) {
                return v1.getIntegerId().intValue() - v2.getIntegerId().intValue();
            } else if (v1.getStringId() != null && v2.getStringId() != null) {
                return v1.getStringId().compareTo(v2.getStringId());
            } else {
                String v1ID = DtoUtil.getStringID(v1);
                String v2ID = DtoUtil.getStringID(v2);
                return v1ID.compareTo(v2ID);
            }
        } else if (n1 instanceof RsvpLspDto) {
            RsvpLspDto lsp1 = (RsvpLspDto) n1;
            RsvpLspDto lsp2 = (RsvpLspDto) n2;
            return lsp1.getName().compareTo(lsp2.getName());
        } else if (n1 instanceof IpSubnetDto) {
            IpSubnetDto ip1 = (IpSubnetDto) n1;
            IpSubnetDto ip2 = (IpSubnetDto) n2;
            return ip1.getSubnetName().compareTo(ip2.getSubnetName());
        } else {
            throw new IllegalArgumentException("not supported network-dto type: " + getClassName(n1));
        }
    }
}