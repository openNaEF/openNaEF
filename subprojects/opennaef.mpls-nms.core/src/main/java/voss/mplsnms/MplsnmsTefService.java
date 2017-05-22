package voss.mplsnms;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import naef.NaefTefService;
import naef.dto.LocationDto;
import naef.dto.NodeDto;
import naef.dto.ip.IpIfDto;
import naef.dto.ip.IpSubnetDto;
import naef.dto.mpls.RsvpLspDto;
import naef.mvo.CustomerInfo;
import naef.mvo.Location;
import naef.mvo.Node;
import naef.mvo.ip.IpIf;
import naef.mvo.ip.IpSubnet;
import naef.mvo.mpls.RsvpLsp;
import tef.MvoHome;
import tef.TefService;
import tef.skelton.Attribute;
import tef.skelton.AuthenticationException;
import tef.skelton.dto.DtoAttrTranscript;
import tef.skelton.dto.DtoAttrTranscript.EvalStrategy;
import tef.skelton.dto.EntityDto;
import voss.mplsnms.rmc.AllocateVlanIpsubnetaddr;

public class MplsnmsTefService extends NaefTefService {

    public static final String PROPERTYNAME_RMISERVICENAME = "voss.mplsnms.rmi-service-name";

    @Override protected void serviceStarted() {
        super.serviceStarted();

        String rmiServiceName = System.getProperty(PROPERTYNAME_RMISERVICENAME);
        if (rmiServiceName == null) {
            throw new IllegalStateException("system property '" + PROPERTYNAME_RMISERVICENAME+ "' を設定してください.");
        }
        try {
            TefService.instance().getRmiRegistry().bind(rmiServiceName, new MplsnmsRmiServiceAccessPoint.Impl());
        } catch (RemoteException re) {
            throw new RuntimeException(re);
        } catch (AlreadyBoundException abe) {
            throw new RuntimeException(abe);
        }

        NaefTefService.instance().getRmcServerService()
            .registerMethod(AllocateVlanIpsubnetaddr.Call.class, new AllocateVlanIpsubnetaddr.Exec());
    }

    @Override protected void installTypes() {
        super.installTypes();

        installAttributes(naef.mvo.CustomerInfo.class, MplsnmsAttrs.CustomerInfoAttr.class);

        installAttributes(naef.mvo.Location.class, MplsnmsAttrs.LocationAttr.class);
        installDtoAttrTranscript(new DtoAttrTranscript.MapKeyRefAttr<IpSubnetDto, Double, Location>(
            LocationDto.class,
            MplsnmsAttrs.LocationDtoAttr.USER_TRAFFIC_BUNPAI_HIRITSU_JIDOU,
            MplsnmsAttrs.LocationAttr.USER_TRAFFIC_BUNPAI_HIRITSU_JIDOU,
            EvalStrategy.LAZY)
        {
            @Override public Map<EntityDto.Desc<IpSubnetDto>, Double> get(Location mvo) {
                return removeNullMapping(super.get(mvo));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.MapKeyRefAttr<IpSubnetDto, Double, Location>(
            LocationDto.class,
            MplsnmsAttrs.LocationDtoAttr.USER_TRAFFIC_BUNPAI_HIRITSU_SHUDOU,
            MplsnmsAttrs.LocationAttr.USER_TRAFFIC_BUNPAI_HIRITSU_SHUDOU,
            EvalStrategy.LAZY)
        {
            @Override public Map<EntityDto.Desc<IpSubnetDto>, Double> get(Location mvo) {
                return removeNullMapping(super.get(mvo));
            }
        });

        installAttributes(naef.mvo.Node.class, MplsnmsAttrs.NodeAttr.class);
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRefAttr<LocationDto, Node>(
            NodeDto.class,
            MplsnmsAttrs.NodeDtoAttr.SHUUYOU_FLOOR,
            MplsnmsAttrs.NodeAttr.SHUUYOU_FLOOR,
            EvalStrategy.LAZY));

        installAttributes(naef.mvo.NodeGroup.class, MplsnmsAttrs.NodeGroupAttr.class);

        installAttributes(naef.mvo.ip.IpSubnetAddress.class, MplsnmsAttrs.IpSubnetAddressAttr.class);

        installAttributes(naef.mvo.ip.IpIf.class, MplsnmsAttrs.IpIfAttr.class);

        installAttributes(naef.mvo.mpls.RsvpLsp.class, MplsnmsAttrs.RsvpLspAttr.class);
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRefAttr<RsvpLspDto, RsvpLsp>(
            RsvpLspDto.class,
            MplsnmsAttrs.RsvpLspDtoAttr.COUPLING_PAIR,
            MplsnmsAttrs.RsvpLspAttr.COUPLING_PAIR,
            EvalStrategy.LAZY));

        final MvoHome<naef.mvo.ip.IpIf> ipifHome = new MvoHome<naef.mvo.ip.IpIf>(naef.mvo.ip.IpIf.class);
        naef.mvo.ip.IpSubnet.Attr.NAMESPACE.addPostProcessor(
            new Attribute.SingleAttr.PostProcessor<naef.mvo.ip.IpSubnetNamespace, naef.mvo.ip.IpSubnet>() {

                @Override public void set(
                    naef.mvo.ip.IpSubnet model,
                    naef.mvo.ip.IpSubnetNamespace oldValue,
                    naef.mvo.ip.IpSubnetNamespace newValue)
                {
                    if (newValue == null) {
                        removeFromIpIfAttributes(model);
                        removeFromLocationAttributes(model);
                    }
                }

                private void removeFromIpIfAttributes(naef.mvo.ip.IpSubnet subnet) {
                    for (naef.mvo.ip.IpIf ipif : ipifHome.list()) {
                        if (MplsnmsAttrs.IpIfAttr.UKAIRO.containsKey(ipif, subnet)) {
                            MplsnmsAttrs.IpIfAttr.UKAIRO.remove(ipif, subnet);
                        }

                        if (MplsnmsAttrs.IpIfAttr.UKAIRO_HOUKOU.containsKey(ipif, subnet)) {
                            MplsnmsAttrs.IpIfAttr.UKAIRO_HOUKOU.remove(ipif, subnet);
                        }
                    }
                }

                private void removeFromLocationAttributes(naef.mvo.ip.IpSubnet subnet) {
                    for (naef.mvo.Location location : naef.mvo.Location.home.list()) {
                        if (MplsnmsAttrs.LocationAttr
                            .USER_TRAFFIC_BUNPAI_HIRITSU_JIDOU.containsKey(location, subnet))
                        {
                            MplsnmsAttrs.LocationAttr.USER_TRAFFIC_BUNPAI_HIRITSU_JIDOU.remove(location, subnet);
                        }

                        if (MplsnmsAttrs.LocationAttr
                            .USER_TRAFFIC_BUNPAI_HIRITSU_SHUDOU.containsKey(location, subnet))
                        {
                            MplsnmsAttrs.LocationAttr.USER_TRAFFIC_BUNPAI_HIRITSU_SHUDOU.remove(location, subnet);
                        }
                    }
                }
            });
        installDtoAttrTranscript(new DtoAttrTranscript.MapKeyRefAttr<IpSubnetDto, Double, IpIf>(
            IpIfDto.class,
            MplsnmsAttrs.IpIfDtoAttr.UKAIRO,
            MplsnmsAttrs.IpIfAttr.UKAIRO,
            EvalStrategy.LAZY)
        {
            @Override public Map<EntityDto.Desc<IpSubnetDto>, Double> get(IpIf mvo) {
                return removeNullMapping(super.get(mvo));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.MapKeyRefAttr<IpSubnetDto, Boolean, IpIf>(
            IpIfDto.class,
            MplsnmsAttrs.IpIfDtoAttr.UKAIRO_HOUKOU,
            MplsnmsAttrs.IpIfAttr.UKAIRO_HOUKOU,
            EvalStrategy.LAZY)
        {
            @Override public Map<EntityDto.Desc<IpSubnetDto>, Boolean> get(IpIf mvo) {
                return removeNullMapping(super.get(mvo));
            }
        });
    }

    @Override public String authenticate(String clientHost) throws AuthenticationException {
        return null;
    }
}
