package naef;

import lib38k.logger.Logger;
import lib38k.net.httpd.HttpConnection;
import lib38k.net.httpd.HttpRequest;
import lib38k.net.httpd.HttpServer;
import naef.mvo.NaefMvoUtils;
import naef.mvo.NetworkUtils;
import naef.mvo.vxlan.Vxlan;
import tef.MVO;
import tef.TefService;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.Attribute;
import tef.skelton.AuthenticationException;
import tef.skelton.FormatException;
import tef.skelton.IdPool;
import tef.skelton.Model;
import tef.skelton.ObjectQueryExpression;
import tef.skelton.ResolveException;
import tef.skelton.Resolver;
import tef.skelton.SkeltonTefService;
import tef.skelton.UiTypeName;
import tef.skelton.UniquelyNamedModelResolver;
import tef.skelton.ValueResolver;
import tef.skelton.dto.Dto2Mvo;
import tef.skelton.dto.DtoAttrTranscript;
import tef.skelton.dto.DtoAttrTranscript.EvalStrategy;
import tef.skelton.dto.DtoInitializer;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;
import tef.skelton.dto.MvoDtoFactory;
import tef.skelton.dto.TaskSynthesizedDtoBuilder;
import tef.skelton.shell.DumpCommand;
import tef.ui.ObjectRenderer;
import tef.ui.http.DumpObjectResponse;
import tef.ui.http.TefHttpConnection;
import tef.ui.http.TefHttpServer;
import tef.ui.shell.ShellConnection;
import tef.ui.shell.ShellServer;
import tef.ui.shell.ShellSession;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NaefTefService extends SkeltonTefService {

    private final boolean rmiClientAuthenticationEnabled_;

    public static NaefTefService instance() {
        return (NaefTefService) TefService.instance();
    }

    public NaefTefService() {
        logMessage("naef service, build:" + readBuildVersion("naef"));

        addExtraObjectCoder(naef.mvo.ip.Ipv4Address.EXTRA_OBJECT_CODER);
        addExtraObjectCoder(naef.mvo.ip.Ipv6Address.EXTRA_OBJECT_CODER);
        addExtraObjectCoder(naef.mvo.ospf.OspfAreaId.EXTRA_OBJECT_CODER);

        String rmiClientAuthenticationEnabledStr = System.getProperty("naef.rmi-client-authentication-enabled");
        rmiClientAuthenticationEnabled_
            = ! (rmiClientAuthenticationEnabledStr != null && rmiClientAuthenticationEnabledStr.equals("false"));
        logMessage("naef.rmi-client-authentication-enabled: " + rmiClientAuthenticationEnabled_);

        ObjectRenderer.addObjectRenderer(new ObjectRenderer.Renderer() {

                @Override public Class<?> getTargetClass() {
                    return naef.mvo.ip.IpAddress.class;
                }

                @Override public String render(Object obj) {
                    return ((naef.mvo.ip.IpAddress) obj).toString();
                }
            });

        ObjectRenderer.addObjectRenderer(new ObjectRenderer.Renderer() {

                @Override public Class<?> getTargetClass() {
                    return naef.mvo.ip.IpAddressRange.class;
                }

                @Override public String render(Object obj) {
                    naef.mvo.ip.IpAddressRange range = (naef.mvo.ip.IpAddressRange) obj;
                    return (range.getLowerBound() != null
                            && range.getUpperBound() != null
                            && range.computeSubnetMaskLength() != null)
                        ? range.getLowerBound() + "/" + range.computeSubnetMaskLength()
                        : range.getRangeStr();
                }
            });
        ObjectRenderer.addObjectRenderer(new ObjectRenderer.Renderer() {

                @Override public Class getTargetClass() {
                    return Model.class;
                }

                @Override public String render(Object obj) {
                    return SkeltonTefService.instance()
                        .uiTypeNames().getName((Class<? extends Model>) obj.getClass())
                        + ":"
                        + (obj instanceof MVO
                            ? ((MVO) obj).getMvoId().getLocalStringExpression()
                            : obj.toString());
                }
            });
        ObjectRenderer.addObjectRenderer(new ObjectRenderer.Renderer() {

                @Override public Class getTargetClass() {
                    return naef.mvo.NodeElement.class;
                }

                @Override public String render(Object obj) {
                    return ((naef.mvo.NodeElement) obj).getFqn();
                }
            });
        DumpObjectResponse.addMethodInvoker(new DumpObjectResponse.MethodInvoker() {

                @Override public boolean isAdaptive(MVO obj) {
                    return obj instanceof naef.mvo.NodeElement;
                }

                @Override public String getName() {
                    return "sub-elements";
                }

                @Override public Object invokeMethod(MVO obj) {
                    return ((naef.mvo.NodeElement) obj).getCurrentSubElements();
                }
            });
        DumpObjectResponse.addMethodInvoker(new DumpObjectResponse.MethodInvoker() {

                @Override public boolean isAdaptive(MVO mvo) {
                    return mvo instanceof naef.mvo.NodeElement;
                }

                @Override public String getName() {
                    return "absolute name";
                }

                @Override public Object invokeMethod(MVO mvo) {
                    return ((naef.mvo.NodeElement) mvo).getFqn();
                }
            });
        DumpObjectResponse.addMethodInvoker(new DumpObjectResponse.MethodInvoker() {

                @Override public boolean isAdaptive(MVO mvo) {
                    return mvo instanceof naef.mvo.InterconnectionIf;
                }

                @Override public String getName() {
                    return "current attached ports";
                }

                @Override public Object invokeMethod(MVO mvo) {
                    return ((naef.mvo.InterconnectionIf) mvo).getCurrentAttachedPorts();
                }
            });

        DumpCommand.setRenderer(new DumpCommand.Renderer() {

                @Override protected String renderEntityDto(EntityDto o) {
                    if (o instanceof naef.dto.LinkDto) {
                        List<EntityDto.Desc<naef.dto.PortDto>> ports
                            = new ArrayList<EntityDto.Desc<naef.dto.PortDto>>(
                                o.get(naef.dto.LinkDto.ExtAttr.MEMBER_PORTS));
                        DumpCommand.sort(ports);
                        StringBuilder result = new StringBuilder();
                        for (EntityDto.Desc<naef.dto.PortDto> port : ports) {
                            result.append(result.length() == 0 ? "" : ",");
                            result.append(render(port));
                        }
                        return getTypeName(o) + "{" + result.toString() + "}";
                    }
                    if (o instanceof naef.dto.NaefDto) {
                        String absName = ((naef.dto.NaefDto) o).getAbsoluteName();
                        if (absName != null) {
                            return absName;
                        }
                    }
                    if (o instanceof naef.dto.PathHopDto) {
                        naef.dto.PathHopDto pathhop = (naef.dto.PathHopDto) o;
                        return getTypeName(o)
                            + "{" + render(pathhop.get(naef.dto.PathHopDto.Attr.SRC_PORT))
                            + "," + render(pathhop.get(naef.dto.PathHopDto.Attr.DST_PORT))
                            + "}";
                    }
                    return super.renderEntityDto(o);
                }

                @Override protected String renderOther(Object o) {
                    if (o instanceof naef.dto.IdRange<?>) {
                        return ((naef.dto.IdRange<?>) o).lowerBound.toString()
                            + "-"
                            + ((naef.dto.IdRange<?>) o).upperBound.toString();
                    }
                    return super.renderOther(o);
                }

                private String getTypeName(EntityDto o) {
                    return SkeltonTefService.instance().uiTypeNames().getName(Dto2Mvo.toMvo(o).getClass());
                }
            });
    }

    @Override protected ShellServer createShellService() {
        return new ShellServer() {

                @Override protected ShellSession createSession(ShellConnection connection, Logger logger) {
                    return new naef.shell.NaefShellSession(connection, logger);
                }
            };
    }

    @Override protected HttpServer createHttpService(HttpServer.Config config, Logger logger) {
        return new TefHttpServer(config, logger) {

                @Override protected ServerSocket createServerSocket(int port) throws IOException {
                    return "ssl".equals(System.getProperty("http-socket-type"))
                        ? SSLServerSocketFactory.getDefault().createServerSocket(port)
                        : super.createServerSocket(port);
                }

                @Override protected HttpConnection newConnection(Socket socket) {
                    return new TefHttpConnection(this, socket) {

                            @Override protected boolean authenticate(HttpRequest request) {
                                return authenticateHttpRequest(request);
                            }
                        };
                }
            };
    }

    @Override protected TaskSynthesizedDtoBuilder newTaskSynthesizedDtoBuilder() {
        return new TaskSynthesizedDtoBuilder() {

                @Override protected Object convertToDtoAdaptiveValue(
                    Model mvo, EntityDto dto, Attribute<?, ?> attribute, Object value)
                    throws SynthesizeException
                {
                    return (mvo instanceof naef.mvo.mpls.RsvpLsp)
                        && (attribute == naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_1
                            || attribute == naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_2
                            || attribute == naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_3
                            || attribute == naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_4
                            || attribute == naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_5
                            || attribute == naef.mvo.mpls.RsvpLsp.Attr.ACTIVE_HOP_SERIES)
                        ? MvoDtoDesc.build1((Model) value)
                        : super.convertToDtoAdaptiveValue(mvo, dto, attribute, value);
                }
            };
    }

    @Override protected void installTypes() {
        super.installTypes();

        MvoDtoFactory factory = getMvoDtoFactory();

        installDtoInitializer(new DtoInitializer<MVO, naef.dto.NaefDto>(naef.dto.NaefDto.class) {

            @Override public void initialize(MVO mvo, naef.dto.NaefDto dto) {
                Resolver<?> resolver = getResolver((Class<? extends Model>) mvo.getClass());
                dto.set(
                    naef.dto.NaefDtoAttrs.SHELL_CLASS_ID,
                    resolver == null ? null : resolver.getName());
                dto.set(
                    naef.dto.NaefDtoAttrs.OBJECT_TYPE_NAME,
                    uiTypeNames().getName(mvo.getClass()));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.CustomerInfoDto, Model>(
            naef.dto.NaefDto.class,
            naef.dto.NaefDtoAttrs.CUSTOMER_INFOS,
            naef.mvo.NaefAttributes.CUSTOMER_INFOS,
            EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.Chassis.class, "chassis");
        installMvoDtoMapping(naef.mvo.Chassis.class, naef.dto.ChassisDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.SlotDto, naef.mvo.Chassis>(
            naef.dto.ChassisDto.class,
            naef.dto.ChassisDto.ExtAttr.SLOTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Slot> getValues(naef.mvo.Chassis mvo) {
                return NaefMvoUtils.getCurrentSubElements(mvo, naef.mvo.Slot.class, false);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.JackDto, naef.mvo.Chassis>(
            naef.dto.ChassisDto.class,
            naef.dto.ChassisDto.ExtAttr.JACKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Jack> getValues(naef.mvo.Chassis mvo) {
                return NaefMvoUtils.getCurrentSubElements(mvo, naef.mvo.Jack.class, false);
            }
        });

        installUiTypeName(naef.mvo.CrossConnection.class, "x-conn");

        installUiTypeName(naef.mvo.CustomerInfo.class, "customer-info");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.CustomerInfo>(naef.mvo.CustomerInfo.home));
        installAttributes(naef.mvo.CustomerInfo.class, naef.mvo.CustomerInfo.Attr.class);
        installMvoDtoMapping(naef.mvo.CustomerInfo.class, naef.dto.CustomerInfoDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.CustomerInfo, naef.dto.CustomerInfoDto>(
            naef.dto.CustomerInfoDto.class)
        {
            @Override public void initialize(naef.mvo.CustomerInfo mvo, naef.dto.CustomerInfoDto dto) {
                dto.set(naef.dto.NaefDtoAttrs.ABSOLUTE_NAME, mvo.getFqn());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.NaefDto, naef.mvo.CustomerInfo>(
            naef.dto.CustomerInfoDto.class,
            naef.dto.CustomerInfoDto.ExtAttr.REFERENCES,
            naef.mvo.CustomerInfo.Attr.REFERENCES,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRefAttr<naef.dto.SystemUserDto, naef.mvo.CustomerInfo>(
            naef.dto.CustomerInfoDto.class,
            naef.dto.CustomerInfoDto.ExtAttr.SYSTEM_USER,
            naef.mvo.CustomerInfo.Attr.SYSTEM_USER,
            EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.Hardware.class, "hardware");
        installAttributes(naef.mvo.Hardware.class, naef.mvo.AbstractHardware.Attr.class);

        installUiTypeName(naef.mvo.HardwareType.class, "hardware-type");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.HardwareType>(naef.mvo.HardwareType.home));
        installAttributes(naef.mvo.HardwareType.class, naef.mvo.HardwareType.Attr.class);
        installMvoDtoMapping(naef.mvo.HardwareType.class, naef.dto.HardwareTypeDto.class);

        installUiTypeName(tef.skelton.IdPool.class, "id-pool");
        installDtoInitializer(new DtoInitializer<IdPool.SingleMap<?, ?, ?>, naef.dto.IdPoolDto<?, ?, ?>>(
            naef.dto.IdPoolDto.class)
        {
            @Override public void initialize(IdPool.SingleMap<?, ?, ?> mvo, naef.dto.IdPoolDto<?, ?, ?> dto) {
                dto.set(naef.dto.NaefDtoAttrs.ABSOLUTE_NAME, mvo.getFqn());

                Set<naef.dto.IdRange<?>> idRanges = new HashSet<naef.dto.IdRange<?>>();
                for (tef.skelton.Range<?> range : mvo.getMasterRanges()) {
                    idRanges.add(dto.newIdRange(range));
                }
                dto.set(naef.dto.IdPoolDto.ExtAttr.ID_RANGES, idRanges);

                dto.set(
                    naef.dto.IdPoolDto.ExtAttr.PARENT,
                    MvoDtoDesc.<naef.dto.IdPoolDto<?, ?, ?>>build1(mvo.getParent()));
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRef<naef.dto.IdPoolDto<?, ?, ?>, IdPool.SingleMap<?, ?, ?>>(
                naef.dto.IdPoolDto.class,
                naef.dto.IdPoolDto.ExtAttr.ROOT,
                EvalStrategy.LAZY)
            {
                @Override protected IdPool<?, ?, ?> getValue(IdPool.SingleMap<?, ?, ?> mvo) {
                    return mvo == null ? null : mvo.getRoot();
                }
            });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SetRef<naef.dto.IdPoolDto<?, ?, ?>, IdPool.SingleMap<?, ?, ?>>(
                naef.dto.IdPoolDto.class,
                naef.dto.IdPoolDto.ExtAttr.CHILDREN,
                EvalStrategy.LAZY)
            {
                @Override protected Set<? extends IdPool<?, ?, ?>> getValues(IdPool.SingleMap<?, ?, ?> mvo) {
                    return mvo.getChildren();
                }
            });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SetRef<EntityDto, IdPool.SingleMap<?, ?, ?>>(
                naef.dto.IdPoolDto.class,
                naef.dto.IdPoolDto.ExtAttr.USERS,
                EvalStrategy.LAZY)
            {
                @Override protected Set<? extends Model> getValues(IdPool.SingleMap<?, ?, ?> mvo) {
                    return (Set<? extends Model>) mvo.getUsers();
                }
            });

        installUiTypeName(naef.mvo.InterconnectionIf.class, "i13n-if");
        installMvoDtoMapping(naef.mvo.InterconnectionIf.class, naef.dto.InterconnectionIfDto.class);
        installDtoAttrTranscript(
            new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.InterconnectionIf>(
                naef.dto.InterconnectionIfDto.class,
                naef.dto.InterconnectionIfDto.ExtAttr.ATTACHED_PORTS,
                EvalStrategy.LAZY)
            {
                @Override protected Set<naef.mvo.Port> getValues(naef.mvo.InterconnectionIf mvo) {
                    return mvo.getCurrentAttachedPorts();
                }
            });

        installUiTypeName(naef.mvo.Jack.class, "jack");
        installMvoDtoMapping(naef.mvo.Jack.class, naef.dto.JackDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.Jack, naef.dto.JackDto>(naef.dto.JackDto.class) {

            @Override public void initialize(naef.mvo.Jack mvo, naef.dto.JackDto dto) {
                dto.set(naef.dto.JackDto.ExtAttr.PORT, MvoDtoDesc.<naef.dto.PortDto>build1(mvo.getPort()));
            }
        });

        installUiTypeName(naef.mvo.L1Link.class, "l1-link");

        installMvoDtoMapping(naef.mvo.L2Link.class, naef.dto.L2LinkDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.L2Link<?>, naef.dto.L2LinkDto>(naef.dto.L2LinkDto.class) {

            @Override public void initialize(naef.mvo.L2Link<?> mvo, naef.dto.L2LinkDto dto) {
                dto.set(
                    naef.dto.L2LinkDto.ExtAttr.NESTING_CONTAINER,
                    MvoDtoDesc.<naef.dto.L2LinkDto>build1(mvo.getNestingContainer()));
                dto.set(
                    naef.dto.L2LinkDto.ExtAttr.IS_NESTING_OUTERMOST,
                    Boolean.valueOf(mvo.isNestingOutermost()));
                dto.set(
                    naef.dto.L2LinkDto.ExtAttr.IS_NESTED_INNERMOST,
                    Boolean.valueOf(mvo.isNestedInnermost()));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.L2LinkDto, naef.mvo.L2Link>(
            naef.dto.L2LinkDto.class,
            naef.dto.L2LinkDto.ExtAttr.NESTED_LINKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.L2Link> getValues(naef.mvo.L2Link mvo) {
                return mvo.getNestedLinks();
            }
        });

        installUiTypeName(naef.mvo.Location.class, "location");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.Location>(naef.mvo.Location.home));
        installMvoDtoMapping(naef.mvo.Location.class, naef.dto.LocationDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.Location, naef.dto.LocationDto>(naef.dto.LocationDto.class) {

            @Override public void initialize(naef.mvo.Location mvo, naef.dto.LocationDto dto) {
                dto.set(naef.dto.NaefDtoAttrs.ABSOLUTE_NAME, mvo.getFqn());
                dto.set(naef.dto.LocationDto.ExtAttr.PARENT, MvoDtoDesc.<naef.dto.LocationDto>build1(mvo.getParent()));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.LocationDto, naef.mvo.Location>(
            naef.dto.LocationDto.class,
            naef.dto.LocationDto.ExtAttr.CHILDREN,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Location> getValues(naef.mvo.Location mvo) {
                return mvo.getChildren();
            }
        });

        installUiTypeName(naef.mvo.Module.class, "module");
        installMvoDtoMapping(naef.mvo.Module.class, naef.dto.ModuleDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.JackDto, naef.mvo.Module>(
            naef.dto.ModuleDto.class,
            naef.dto.ModuleDto.ExtAttr.JACKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Jack> getValues(naef.mvo.Module mvo) {
                return NaefMvoUtils.getCurrentSubElements(mvo, naef.mvo.Jack.class, false);
            }
        });

        installUiTypeName(naef.mvo.NaefObjectType.class, "naef-object-type");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.NaefObjectType>(naef.mvo.NaefObjectType.home));
        installAttributes(naef.mvo.NaefObjectType.class, naef.mvo.NaefObjectType.Attr.class);
        installMvoDtoMapping(naef.mvo.NaefObjectType.class, naef.dto.NaefObjectTypeDto.class);

        installUiTypeName(naef.mvo.Network.class, "network");
        installAttributes(naef.mvo.AbstractNetwork.class, naef.mvo.NaefAttributes.class);
        installMvoDtoMapping(naef.mvo.AbstractNetwork.class, naef.dto.NetworkDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.Network>(
            naef.dto.NetworkDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.LAZY)
        {
            @Override public String get(naef.mvo.Network mvo) {
                List<naef.mvo.Port> ports = new ArrayList<naef.mvo.Port>(mvo.getCurrentMemberPorts());
                Collections.sort(ports, new Comparator<naef.mvo.Port>() {

                    @Override public int compare(naef.mvo.Port p1, naef.mvo.Port p2) {
                        return p1.getFqn().compareTo(p2.getFqn());
                    }
                });

                StringBuilder portsFqn = new StringBuilder();
                for (naef.mvo.Port port : ports) {
                    portsFqn.append(portsFqn.length() == 0  ? "" : getFqnTertiaryDelimiter());
                    portsFqn.append(port.getFqn());
                }

                return uiTypeNames().getName(mvo.getClass())
                    + naef.dto.NaefDto.getFqnPrimaryDelimiter()
                    + naef.dto.NaefDto.getFqnLeftBracket()
                    + portsFqn.toString()
                    + naef.dto.NaefDto.getFqnRightBracket();
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.Network>(
            naef.dto.NetworkDto.class,
            naef.dto.NetworkDto.ExtAttr.MEMBER_PORTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Port> getValues(naef.mvo.Network mvo) {
                return new HashSet<naef.mvo.Port>(mvo.getCurrentMemberPorts());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.NetworkDto, naef.mvo.Network>(
            naef.dto.NetworkDto.class,
            naef.dto.NetworkDto.ExtAttr.UPPER_LAYERS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Network> getValues(naef.mvo.Network mvo) {
                return mvo instanceof naef.mvo.Network.LowerStackable
                    ? ((naef.mvo.Network.LowerStackable) mvo).getCurrentUpperLayers(false)
                    : Collections.<naef.mvo.Network>emptySet();
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.NetworkDto, naef.mvo.Network>(
            naef.dto.NetworkDto.class,
            naef.dto.NetworkDto.ExtAttr.LOWER_LAYER_LINKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Network> getValues(naef.mvo.Network mvo) {
                return mvo instanceof naef.mvo.Network.UpperStackable
                    ? ((naef.mvo.Network.UpperStackable) mvo).getCurrentLowerLayers(false)
                    : Collections.<naef.mvo.Network>emptySet();
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRef<naef.dto.NetworkDto, naef.mvo.Network>(
            naef.dto.NetworkDto.class,
            naef.dto.NetworkDto.ExtAttr.OWNER,
            EvalStrategy.LAZY)
        {
            @Override protected naef.mvo.Network getValue(naef.mvo.Network mvo) {
                if (mvo instanceof naef.mvo.Network.Containee) {
                    naef.mvo.Network container = ((naef.mvo.Network.Containee) mvo).getCurrentContainer();
                    if (container != null
                        && naef.dto.NetworkDto.class
                            .isAssignableFrom(getMvoDtoMapping().getDtoClass(container.getClass())))
                    {
                        return container;
                    }
                }
                return null;
            }
        });

        installUiTypeName(naef.mvo.NetworkType.class, "network-type");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.NetworkType>(naef.mvo.NetworkType.home));
        installAttributes(naef.mvo.NetworkType.class, naef.mvo.NetworkType.Attr.class);
        installMvoDtoMapping(naef.mvo.NetworkType.class, naef.dto.NetworkTypeDto.class);

        installUiTypeName(naef.mvo.Node.class, "node");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.Node>(naef.mvo.Node.home));
        installAttributes(naef.mvo.Node.class, naef.mvo.Node.Attr.class);
        installAttributes(
            naef.mvo.Node.class,
            naef.mvo.vlan.VlanAttrs.ENABLED_NETWORKING_FUNCTION_VLAN,
            naef.mvo.mpls.Pseudowire.Attr.PSEUDOWIRE_ENABLED,
            naef.mvo.mpls.RsvpLsp.Attr.RSVP_LSP_ENABLED,
            naef.mvo.vpls.VplsIf.Attr.VPLS_ENABLED,
            naef.mvo.vrf.VrfIf.Attr.VRF_ENABLED);
        installMvoDtoMapping(naef.mvo.Node.class, naef.dto.NodeDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.Node, naef.dto.NodeDto>(naef.dto.NodeDto.class) {

            @Override public void initialize(naef.mvo.Node mvo, naef.dto.NodeDto dto) {
                dto.set(naef.dto.NodeDto.ExtAttr.NODE_NAME, mvo.getName());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRefAttr<naef.dto.NodeTypeDto, naef.mvo.Node>(
            naef.dto.NodeDto.class,
            naef.dto.NodeDto.ExtAttr.OBJECT_TYPE,
            naef.mvo.Node.Attr.OBJECT_TYPE,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.ChassisDto, naef.mvo.Node>(
            naef.dto.NodeDto.class,
            naef.dto.NodeDto.ExtAttr.CHASSISES,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Chassis> getValues(naef.mvo.Node mvo) {
                return NaefMvoUtils.getCurrentSubElements(mvo, naef.mvo.Chassis.class, false);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.JackDto, naef.mvo.Node>(
            naef.dto.NodeDto.class,
            naef.dto.NodeDto.ExtAttr.JACKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Jack> getValues(naef.mvo.Node mvo) {
                return NaefMvoUtils.getCurrentSubElements(mvo, naef.mvo.Jack.class, true);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.Node>(
            naef.dto.NodeDto.class,
            naef.dto.NodeDto.ExtAttr.PORTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Port> getValues(naef.mvo.Node mvo) {
                return NaefMvoUtils.getCurrentSubElements(mvo, naef.mvo.Port.class, true);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.NodeGroupDto, naef.mvo.Node>(
            naef.dto.NodeDto.class,
            naef.dto.NodeDto.ExtAttr.NODE_GROUPS,
            naef.mvo.Node.Attr.NODE_GROUPS,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.NodeDto, naef.mvo.Node>(
            naef.dto.NodeDto.class,
            naef.dto.NodeDto.ExtAttr.VIRTUALIZATION_HOST_NODES,
            naef.mvo.Node.Attr.VIRTUALIZATION_HOST_NODES,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.NodeDto, naef.mvo.Node>(
            naef.dto.NodeDto.class,
            naef.dto.NodeDto.ExtAttr.VIRTUALIZATION_GUEST_NODES,
            naef.mvo.Node.Attr.VIRTUALIZATION_GUEST_NODES,
            EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.NodeElement.class, "node-element");
        installAttributes(
            naef.mvo.NodeElement.class,
            naef.mvo.NaefAttributes.class,
            naef.mvo.AbstractNodeElement.Attr.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.NodeElement, naef.dto.NodeElementDto>(
            naef.dto.NodeElementDto.class)
        {
            @Override public void initialize(naef.mvo.NodeElement mvo, naef.dto.NodeElementDto dto) {
                dto.set(naef.dto.NaefDtoAttrs.ABSOLUTE_NAME, mvo.getFqn());
                dto.set(naef.dto.NodeElementDto.ExtAttr.SQN, NaefMvoUtils.getSqn(mvo));
                dto.set(naef.dto.NodeElementDto.ExtAttr.NODE_LOCAL_NAME, NaefMvoUtils.getNodeLocalName(mvo));
                dto.set(naef.dto.NodeElementDto.ExtAttr.NODE, MvoDtoDesc.<naef.dto.NodeDto>build1(mvo.getNode()));
                dto.set(
                    naef.dto.NodeElementDto.ExtAttr.OWNER,
                    MvoDtoDesc.<naef.dto.NodeElementDto>build1(mvo.getOwner()));
                }
            });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.NodeElementDto, naef.mvo.NodeElement>(
            naef.dto.NodeElementDto.class,
            naef.dto.NodeElementDto.ExtAttr.SUBELEMENTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.NodeElement> getValues(naef.mvo.NodeElement mvo) {
                return mvo.getCurrentSubElements();
            }
        });

        installUiTypeName(naef.mvo.NodeElementType.class, "node-element-type");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.NodeElementType>(naef.mvo.NodeElementType.home));
        installAttributes(naef.mvo.NodeElementType.class, naef.mvo.NodeElementType.Attr.class);
        installMvoDtoMapping(naef.mvo.NodeElementType.class, naef.dto.NodeElementTypeDto.class);

        installUiTypeName(naef.mvo.NodeGroup.class, "node-group");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.NodeGroup>(naef.mvo.NodeGroup.home));
        installAttributes(naef.mvo.NodeGroup.class, naef.mvo.NodeGroup.Attr.class);
        installMvoDtoMapping(naef.mvo.NodeGroup.class, naef.dto.NodeGroupDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.NodeGroup, naef.dto.NodeGroupDto>(
            naef.dto.NodeGroupDto.class)
        {
            @Override public void initialize(naef.mvo.NodeGroup mvo, naef.dto.NodeGroupDto dto) {
                dto.set(naef.dto.NaefDtoAttrs.ABSOLUTE_NAME, mvo.getFqn());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.NodeDto, naef.mvo.NodeGroup>(
            naef.dto.NodeGroupDto.class,
            naef.dto.NodeGroupDto.ExtAttr.MEMBERS,
            naef.mvo.NodeGroup.Attr.MEMBERS,
            EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.NodeType.class, "node-type");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.NodeType>(naef.mvo.NodeType.home));
        installAttributes(naef.mvo.NodeType.class, naef.mvo.NodeType.Attr.class);
        installMvoDtoMapping(naef.mvo.NodeType.class, naef.dto.NodeTypeDto.class);

        installMvoDtoMapping(naef.mvo.P2pLink.class, naef.dto.LinkDto.class);

        installDtoInitializer(new DtoInitializer<naef.mvo.PathHop<?, ?, ?>, naef.dto.PathHopDto>(
            naef.dto.PathHopDto.class)
        {
            @Override public void initialize(naef.mvo.PathHop<?, ?, ?> mvo, naef.dto.PathHopDto dto) {
                dto.set(naef.dto.PathHopDto.Attr.SRC_PORT, MvoDtoDesc.<naef.dto.PortDto>build1(mvo.getSrcPort()));
                dto.set(naef.dto.PathHopDto.Attr.DST_PORT, MvoDtoDesc.<naef.dto.PortDto>build1(mvo.getDstPort()));
            }
        });

        installUiTypeName(naef.mvo.Port.class, "port");
        installSubResolver(new Resolver.SingleNameResolver<naef.mvo.Port, naef.mvo.Node>(
            naef.mvo.Port.class,
            "if-name",
            naef.mvo.Node.class)
        {
            @Override protected naef.mvo.Port resolveImpl(naef.mvo.Node context, String arg) throws ResolveException {
                String ifname = arg;
                naef.mvo.Port port = naef.mvo.Node.Attr.IFNAME_MAP.get(context, ifname);
                if (port == null) {
                    throw new ResolveException("オブジェクトが見つかりません.");
                }
                return port;
            }

            @Override public String getName(naef.mvo.Port obj) {
                return obj.getFqn();
            }
        });
        installSubResolver(new Resolver.SingleNameResolver<naef.mvo.Port, naef.mvo.Node>(
            naef.mvo.Port.class,
            "if-index",
            naef.mvo.Node.class)
        {
            @Override protected naef.mvo.Port resolveImpl(naef.mvo.Node context, String arg) throws ResolveException {
                Long ifindex;
                try {
                    ifindex = ValueResolver.parseLong(arg, false);
                } catch (FormatException fe) {
                    throw new ResolveException("if-index には数値を指定してください.");
                }

                naef.mvo.Port port = naef.mvo.Node.Attr.IFINDEX_MAP.get(context, ifindex);
                if (port == null) {
                    throw new ResolveException("オブジェクトが見つかりません.");
                }
                return port;
            }

            @Override public String getName(naef.mvo.Port obj) {
                return obj.getFqn();
            }
        });
        installAttributes(
            naef.mvo.Port.class, 
            naef.mvo.AbstractPort.Attr.class);
        installMvoDtoMapping(naef.mvo.Port.class, naef.dto.PortDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.Port, naef.dto.PortDto>(naef.dto.PortDto.class) {

            @Override public void initialize(naef.mvo.Port mvo, naef.dto.PortDto dto) {
                dto.set(
                    naef.dto.PortDto.ExtAttr.PRIMARY_IPIF,
                    MvoDtoDesc.<naef.dto.ip.IpIfDto>build1(naef.mvo.AbstractPort.Attr.PRIMARY_IPIF.get(mvo)));
                dto.set(
                    naef.dto.PortDto.ExtAttr.SECONDARY_IPIF,
                    MvoDtoDesc.<naef.dto.ip.IpIfDto>build1(naef.mvo.AbstractPort.Attr.SECONDARY_IPIF.get(mvo)));
                dto.set(
                    naef.dto.PortDto.ExtAttr.CONTAINER,
                    MvoDtoDesc.<naef.dto.PortDto>build1(mvo.getContainer()));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.PARTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Port> getValues(naef.mvo.Port mvo) {
                return asSet(mvo.getParts());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.UPPER_LAYERS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Port> getValues(naef.mvo.Port mvo) {
                return asSet(mvo.getUpperLayerPorts());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.LOWER_LAYERS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Port> getValues(naef.mvo.Port mvo) {
                return asSet(mvo.getLowerLayerPorts());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.CROSS_CONNECTIONS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Port> getValues(naef.mvo.Port mvo) {
                return asSet(mvo.getCurrentCrossConnectedPorts());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.NetworkDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.NETWORKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Network> getValues(naef.mvo.Port mvo) {
                return naef.dto.NaefDtoUtils.getPortNetworks(mvo);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.MapKeyValueRefAttr<naef.dto.PortDto, naef.dto.CustomerInfoDto, naef.mvo.Port>(
                naef.dto.PortDto.class,
                naef.dto.PortDto.ExtAttr.PORT_CUSTOMERINFOS,
                naef.mvo.AbstractPort.Attr.PORT_CUSTOMERINFOS,
                EvalStrategy.LAZY)
            {
                @Override public
                    Map<EntityDto.Desc<naef.dto.PortDto>, EntityDto.Desc<naef.dto.CustomerInfoDto>>
                    get(naef.mvo.Port mvo)
                {
                    return removeNullMapping(super.get(mvo));
                }
            });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.ALIASES,
            naef.mvo.AbstractPort.Attr.ALIASES,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRefAttr<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.ALIAS_SOURCE,
            naef.mvo.AbstractPort.Attr.ALIAS_SOURCE,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRef<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.ALIAS_ROOT_SOURCE,
            EvalStrategy.LAZY)
        {
            @Override protected naef.mvo.Port getValue(naef.mvo.Port mvo) {
                return getAliasRootSource(mvo);
            }

            private naef.mvo.Port getAliasRootSource(naef.mvo.Port mvo) {
                naef.mvo.Port aliasSource = naef.mvo.AbstractPort.Attr.ALIAS_SOURCE.get(mvo);
                return aliasSource == null
                    ? mvo
                    : getAliasRootSource(aliasSource);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.MapKeyRefAttr<naef.dto.ip.IpIfDto, Integer, naef.mvo.Port>(
            naef.dto.PortDto.class,
            naef.dto.PortDto.ExtAttr.BOUND_IPIFS,
            naef.mvo.AbstractPort.Attr.BOUND_IPIFS,
            EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.PortType.class, "port-type");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.PortType>(naef.mvo.PortType.home));
        installAttributes(naef.mvo.PortType.class, naef.mvo.PortType.Attr.class);
        installMvoDtoMapping(naef.mvo.PortType.class, naef.dto.PortTypeDto.class);

        installUiTypeName(naef.mvo.Slot.class, "slot");
        installMvoDtoMapping(naef.mvo.Slot.class, naef.dto.SlotDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.Slot, naef.dto.SlotDto>(naef.dto.SlotDto.class) {

            @Override public void initialize(naef.mvo.Slot mvo, naef.dto.SlotDto dto) {
                dto.set(naef.dto.SlotDto.ExtAttr.MODULE, MvoDtoDesc.<naef.dto.ModuleDto>build1(mvo.getCurrentModule()));
            }
        });

        installUiTypeName(naef.mvo.SystemUser.class, "system-user");
        installMainResolver(new UniquelyNamedModelResolver<naef.mvo.SystemUser>(naef.mvo.SystemUser.home));
        installAttributes(naef.mvo.SystemUser.class, naef.mvo.SystemUser.Attr.class);
        installMvoDtoMapping(naef.mvo.SystemUser.class, naef.dto.SystemUserDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.SystemUser, naef.dto.SystemUserDto>(
            naef.dto.SystemUserDto.class)
        {
            @Override public void initialize(naef.mvo.SystemUser mvo, naef.dto.SystemUserDto dto) {
                dto.set(naef.dto.NaefDtoAttrs.ABSOLUTE_NAME, mvo.getFqn());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.CustomerInfoDto, naef.mvo.SystemUser>(
            naef.dto.SystemUserDto.class,
            naef.dto.SystemUserDto.ExtAttr.CUSTOMER_INFOS,
            naef.mvo.SystemUser.Attr.CUSTOMER_INFOS,
            EvalStrategy.LAZY));

        installDtoInitializer(new DtoInitializer<naef.mvo.Port, naef.dto.HardPortDto>(naef.dto.HardPortDto.class) {

            @Override public void initialize(naef.mvo.Port mvo, naef.dto.HardPortDto dto) {
                naef.mvo.L1Link l1Link = NetworkUtils.getExclusiveLink(naef.mvo.L1Link.class, mvo);
                dto.set(naef.dto.HardPortDto.ExtAttr.L1LINK, MvoDtoDesc.<naef.dto.LinkDto>build1(l1Link));
                dto.set(
                    naef.dto.HardPortDto.ExtAttr.L1NEIGHBOR,
                    l1Link == null
                        ? null
                        : MvoDtoDesc.<naef.dto.PortDto>build1(l1Link.getPeer(mvo)));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.LinkDto, naef.mvo.Port>(
            naef.dto.HardPortDto.class,
            naef.dto.HardPortDto.ExtAttr.L2LINKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.L2Link> getValues(naef.mvo.Port mvo) {
                return mvo.getCurrentNetworks(naef.mvo.L2Link.class);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.Port>(
            naef.dto.HardPortDto.class,
            naef.dto.HardPortDto.ExtAttr.L2NEIGHBORS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Port> getValues(naef.mvo.Port mvo) {
                Set<naef.mvo.Port> l2Neighbors = new HashSet<naef.mvo.Port>();
                for (naef.mvo.L2Link l2Link : mvo.getCurrentNetworks(naef.mvo.L2Link.class)) {
                    naef.mvo.Port l2Neighbor = l2Link.getPeer(mvo);
                    if (l2Neighbor != null) {
                        l2Neighbors.add(l2Neighbor);
                    }
                }
                return l2Neighbors;
            }
        });

        installUiTypeName(naef.mvo.atm.AtmApsIf.class, "atm-aps-if");
        installAttributes(naef.mvo.atm.AtmApsIf.class, naef.mvo.atm.AtmPort.Attr.ATM_ENABLED);
        installMvoDtoMapping(naef.mvo.atm.AtmApsIf.class, naef.dto.atm.AtmApsIfDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.atm.AtmPortDto, naef.mvo.atm.AtmApsIf>(
            naef.dto.atm.AtmApsIfDto.class,
            naef.dto.atm.AtmApsIfDto.ExtAttr.ATM_PORTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.atm.AtmPort> getValues(naef.mvo.atm.AtmApsIf mvo) {
                return (Set<naef.mvo.atm.AtmPort>) mvo.getParts();
            }
        });

        installUiTypeName(naef.mvo.atm.AtmApsLink.class, "atm-aps-link");

        installUiTypeName(naef.mvo.atm.AtmLink.class, "atm-link");

        installUiTypeName(naef.mvo.atm.AtmPort.class, "atm-port");
        installAttributes(naef.mvo.atm.AtmPort.class, naef.mvo.atm.AtmPort.Attr.class);
        installMvoDtoMapping(naef.mvo.atm.AtmPort.class, naef.dto.atm.AtmPortDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.atm.AtmPort, naef.dto.atm.AtmPortDto>(
            naef.dto.atm.AtmPortDto.class)
        {
            @Override public void initialize(naef.mvo.atm.AtmPort mvo, naef.dto.atm.AtmPortDto dto) {
                if (mvo.getContainer() instanceof naef.mvo.atm.AtmApsIf) {
                    dto.set(
                        naef.dto.atm.AtmPortDto.ExtAttr.ATM_APS_IF,
                        MvoDtoDesc.<naef.dto.atm.AtmApsIfDto>build1(mvo.getContainer()));
                }
            }
        });

        installUiTypeName(naef.mvo.atm.AtmPvc.class, "atm-pvc");

        installUiTypeName(naef.mvo.atm.AtmPvcIf.class, "atm-pvc-if");
        installAttributes(naef.mvo.atm.AtmPvcIf.class, naef.mvo.atm.AtmPvcIf.Attr.class);
        installMvoDtoMapping(naef.mvo.atm.AtmPvcIf.class, naef.dto.atm.AtmPvcIfDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.atm.AtmPvcIf, naef.dto.atm.AtmPvcIfDto>(
            naef.dto.atm.AtmPvcIfDto.class)
        {
            @Override public void initialize(naef.mvo.atm.AtmPvcIf mvo, naef.dto.atm.AtmPvcIfDto dto) {
                naef.mvo.atm.AtmPvc link = NetworkUtils.getExclusiveLink(naef.mvo.atm.AtmPvc.class, mvo);
                dto.set(
                    naef.dto.atm.AtmPvcIfDto.ExtAttr.NEIGHBOR_PVC,
                    link == null
                        ? null
                        : MvoDtoDesc.<naef.dto.atm.AtmPvcIfDto>build1(link.getPeer(mvo)));
            }
        });

        installUiTypeName(naef.mvo.atm.AtmPvp.class, "atm-pvp");

        installUiTypeName(naef.mvo.atm.AtmPvpIf.class, "atm-pvp-if");
        installAttributes(naef.mvo.atm.AtmPvpIf.class, naef.mvo.atm.AtmPvpIf.Attr.class);
        installMvoDtoMapping(naef.mvo.atm.AtmPvpIf.class, naef.dto.atm.AtmPvpIfDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.atm.AtmPvpIf, naef.dto.atm.AtmPvpIfDto>(
            naef.dto.atm.AtmPvpIfDto.class)
        {
            @Override public void initialize(naef.mvo.atm.AtmPvpIf mvo, naef.dto.atm.AtmPvpIfDto dto) {
                dto.set(
                    naef.dto.atm.AtmPvpIfDto.ExtAttr.BUTSURI_PORT,
                    MvoDtoDesc.<naef.dto.PortDto>build1((naef.mvo.Port) mvo.getOwner()));
            }
        });

        installUiTypeName(naef.mvo.bgp.BgpProcess.class, "bgp-process");
        installMvoDtoMapping(naef.mvo.bgp.BgpProcess.class, naef.dto.bgp.BgpProcessDto.class);

        installUiTypeName(naef.mvo.eth.EthLag.class, "eth-lag");

        installUiTypeName(naef.mvo.eth.EthLagIf.class, "eth-lag-if");
        installAttributes(
            naef.mvo.eth.EthLagIf.class,
            naef.mvo.vlan.VlanAttrs.ENABLED_NETWORKING_FUNCTION_VLAN,
            naef.mvo.vlan.VlanAttrs.SWITCH_PORT_MODE);
        installMvoDtoMapping(naef.mvo.eth.EthLagIf.class, naef.dto.eth.EthLagIfDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.eth.EthLagIf, naef.dto.eth.EthLagIfDto>(
            naef.dto.eth.EthLagIfDto.class)
        {
            @Override public void initialize(naef.mvo.eth.EthLagIf mvo, naef.dto.eth.EthLagIfDto dto) {
                dto.set(
                    naef.dto.eth.EthLagIfDto.ExtAttr.LINK,
                    MvoDtoDesc.<naef.dto.LinkDto>build1(NetworkUtils.getExclusiveLink(naef.mvo.eth.EthLag.class, mvo)));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.eth.EthPortDto, naef.mvo.eth.EthLagIf>(
            naef.dto.eth.EthLagIfDto.class,
            naef.dto.eth.EthLagIfDto.ExtAttr.BUNDLE_PORTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.eth.EthPort> getValues(naef.mvo.eth.EthLagIf mvo) {
                return mvo.getCurrentBundlePorts();
            }
        });

        installUiTypeName(naef.mvo.eth.EthLink.class, "eth-link");

        installUiTypeName(naef.mvo.eth.EthPort.class, "eth-port");
        installAttributes(
            naef.mvo.eth.EthPort.class,
            naef.mvo.vlan.VlanAttrs.ENABLED_NETWORKING_FUNCTION_VLAN,
            naef.mvo.vlan.VlanAttrs.SWITCH_PORT_MODE);
        installMvoDtoMapping(naef.mvo.eth.EthPort.class, naef.dto.eth.EthPortDto.class);

        installUiTypeName(naef.mvo.fr.FrPvc.class, "fr-pvc");

        installUiTypeName(naef.mvo.fr.FrPvcIf.class, "fr-pvc-if");
        installAttributes(naef.mvo.fr.FrPvcIf.class, naef.mvo.fr.FrPvcIf.Attr.class);
        installMvoDtoMapping(naef.mvo.fr.FrPvcIf.class, naef.dto.fr.FrPvcIfDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.fr.FrPvcIf, naef.dto.fr.FrPvcIfDto>(
            naef.dto.fr.FrPvcIfDto.class)
        {
            @Override public void initialize(naef.mvo.fr.FrPvcIf mvo, naef.dto.fr.FrPvcIfDto dto) {
                dto.set(
                    naef.dto.fr.FrPvcIfDto.ExtAttr.BUTSURI_PORT,
                    MvoDtoDesc.<naef.dto.PortDto>build1((naef.mvo.Port) mvo.getOwner()));

                naef.mvo.fr.FrPvc link = NetworkUtils.getExclusiveLink(naef.mvo.fr.FrPvc.class, mvo);
                dto.set(
                    naef.dto.fr.FrPvcIfDto.ExtAttr.NEIGHBOR_PVC,
                    link == null
                        ? null
                        : MvoDtoDesc.<naef.dto.fr.FrPvcIfDto>build1(link.getPeer(mvo)));
            }
        });

        installUiTypeName(naef.mvo.ip.IpAddress.class, "ip-address");

        installUiTypeName(naef.mvo.ip.IpIf.class, "ip-if");
        installAttributes(naef.mvo.ip.IpIf.class, naef.mvo.ip.IpIf.Attr.class);
        installMvoDtoMapping(naef.mvo.ip.IpIf.class, naef.dto.ip.IpIfDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.ip.IpIf, naef.dto.ip.IpIfDto>(naef.dto.ip.IpIfDto.class) {

            @Override public void initialize(naef.mvo.ip.IpIf mvo, naef.dto.ip.IpIfDto dto) {
                dto.set(
                    naef.dto.ip.IpIfDto.ExtAttr.IP_SUBNET,
                    MvoDtoDesc.<naef.dto.ip.IpSubnetDto>build1(mvo.getSubnet()));

                dto.set(
                    naef.dto.ip.IpIfDto.ExtAttr.IP_SUBNET_ADDRESS,
                    MvoDtoDesc.<naef.dto.ip.IpSubnetAddressDto>build1(
                        naef.mvo.ip.IpIf.Attr.IP_SUBNET_ADDRESS.get(mvo)));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRefAttr<naef.dto.PortDto, naef.mvo.ip.IpIf>(
            naef.dto.ip.IpIfDto.class,
            naef.dto.ip.IpIfDto.ExtAttr.ASSOCIATED_PORTS,
            naef.mvo.ip.IpIf.ASSOCIATED_PORTS,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.MapKeyRefAttr<naef.dto.PortDto, Integer, naef.mvo.ip.IpIf>(
            naef.dto.ip.IpIfDto.class,
            naef.dto.ip.IpIfDto.ExtAttr.BOUND_PORTS,
            naef.mvo.ip.IpIf.Attr.BOUND_PORTS,
            EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.ip.IpSubnet.class, "ip-subnet");
        installAttributes(
            naef.mvo.ip.IpSubnet.class,
            naef.mvo.AbstractNetwork.Attr.class,
            naef.mvo.ip.IpSubnet.Attr.class);
        installMvoDtoMapping(naef.mvo.ip.IpSubnet.class, naef.dto.ip.IpSubnetDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.ip.IpSubnet>(
            naef.dto.ip.IpSubnetDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.ip.IpSubnet mvo) {
                naef.mvo.ip.IpSubnetNamespace namespace = mvo.get(naef.mvo.ip.IpSubnet.Attr.NAMESPACE);
                String name = namespace == null ? null : namespace.getId(mvo);
                return getNetworkAbsoluteNameByIdPool(mvo, namespace, name);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.ip.IpSubnet>(
            naef.dto.ip.IpSubnetDto.class,
            naef.dto.ip.IpSubnetDto.ExtAttr.SUBNET_NAME,
            EvalStrategy.LAZY)
        {
            @Override public String get(naef.mvo.ip.IpSubnet mvo) {
                naef.mvo.ip.IpSubnetNamespace namespace = mvo.get(naef.mvo.ip.IpSubnet.Attr.NAMESPACE);
                String name = namespace == null ? null : namespace.getId(mvo);
                return name;
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.ip.IpSubnetNamespaceDto, naef.mvo.ip.IpSubnet>(
                naef.dto.ip.IpSubnetDto.class,
                naef.dto.ip.IpSubnetDto.ExtAttr.NAMESPACE,
                naef.mvo.ip.IpSubnet.Attr.NAMESPACE,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.ip.IpSubnetAddressDto, naef.mvo.ip.IpSubnet>(
                naef.dto.ip.IpSubnetDto.class,
                naef.dto.ip.IpSubnetDto.ExtAttr.SUBNET_ADDRESS,
                naef.mvo.ip.IpSubnet.Attr.SUBNET_ADDRESS,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.ip.IpSubnet>(
            naef.dto.ip.IpSubnetDto.class,
            naef.dto.ip.IpSubnetDto.ExtAttr.MEMBER_IP_IF,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Port> getValues(naef.mvo.ip.IpSubnet mvo) {
                return mvo.getCurrentMemberPorts();
            }
        });

        installUiTypeName(naef.mvo.ip.IpSubnetAddress.class, "ip-subnet-address");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.ip.IpSubnetAddress>(naef.mvo.ip.IpSubnetAddress.home));
        installAttributes(naef.mvo.ip.IpSubnetAddress.class, naef.mvo.ip.IpSubnetAddress.Attr.class);
        installMvoDtoMapping(naef.mvo.ip.IpSubnetAddress.class, naef.dto.ip.IpSubnetAddressDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.ip.IpSubnetAddress, naef.dto.ip.IpSubnetAddressDto>(
            naef.dto.ip.IpSubnetAddressDto.class)
        {
            @Override public void initialize(naef.mvo.ip.IpSubnetAddress mvo, naef.dto.ip.IpSubnetAddressDto dto) {
                dto.set(
                    naef.dto.ip.IpSubnetAddressDto.ExtAttr.IP_SUBNET,
                    MvoDtoDesc.<naef.dto.ip.IpSubnetDto>build1(naef.mvo.ip.IpSubnetAddress.Attr.IP_SUBNET.get(mvo)));
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.ip.IpSubnetNamespaceDto, naef.mvo.ip.IpSubnetAddress>(
                naef.dto.ip.IpSubnetAddressDto.class,
                naef.dto.ip.IpSubnetAddressDto.ExtAttr.IP_SUBNET_NAMESPACE,
                naef.mvo.ip.IpSubnetAddress.Attr.IP_SUBNET_NAMESPACE,
                EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.ip.IpSubnetNamespace.class, "ip.subnet-namespace");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.ip.IpSubnetNamespace>(naef.mvo.ip.IpSubnetNamespace.home));
        installAttributes(naef.mvo.ip.IpSubnetNamespace.class, naef.mvo.ip.IpSubnetNamespace.Attr.class);
        installMvoDtoMapping(naef.mvo.ip.IpSubnetNamespace.class, naef.dto.ip.IpSubnetNamespaceDto.class);
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.ip.IpSubnetAddressDto, naef.mvo.ip.IpSubnetNamespace>(
                naef.dto.ip.IpSubnetNamespaceDto.class,
                naef.dto.ip.IpSubnetNamespaceDto.ExtAttr.IP_SUBNET_ADDRESS,
                naef.mvo.ip.IpSubnetNamespace.Attr.IP_SUBNET_ADDRESS,
                EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.ip.Ipv4Address.class, "ipv4-address");

        installUiTypeName(naef.mvo.ip.Ipv6Address.class, "ipv6-address");

        installUiTypeName(naef.mvo.isdn.IsdnChannelIf.class, "isdn-channel-if");
        installMvoDtoMapping(naef.mvo.isdn.IsdnChannelIf.class, naef.dto.isdn.IsdnChannelIfDto.class);

        installUiTypeName(naef.mvo.isdn.IsdnLink.class, "isdn-link");

        installUiTypeName(naef.mvo.isdn.IsdnPort.class, "isdn-port");
        installMvoDtoMapping(naef.mvo.isdn.IsdnPort.class, naef.dto.isdn.IsdnPortDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.isdn.IsdnChannelIfDto, naef.mvo.isdn.IsdnPort>(
            naef.dto.isdn.IsdnPortDto.class,
            naef.dto.isdn.IsdnPortDto.ExtAttr.CHANNELS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.isdn.IsdnChannelIf> getValues(naef.mvo.isdn.IsdnPort mvo) {
                return NaefMvoUtils.getCurrentSubElements(mvo, naef.mvo.isdn.IsdnChannelIf.class, false);
            }
        });

        installUiTypeName(naef.mvo.mpls.Pseudowire.class, "pseudowire");
        installAttributes(naef.mvo.mpls.Pseudowire.class, naef.mvo.mpls.Pseudowire.Attr.class);
        installMvoDtoMapping(naef.mvo.mpls.Pseudowire.class, naef.dto.mpls.PseudowireDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.mpls.Pseudowire>(
            naef.dto.mpls.PseudowireDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.mpls.Pseudowire mvo) {
                naef.mvo.mpls.PseudowireIdPool idPool = mvo.get(naef.mvo.mpls.Pseudowire.Attr.ID_POOL);
                Long vcId = idPool == null ? null : idPool.getId(mvo);
                if (vcId != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, idPool, vcId);
                }

                naef.mvo.mpls.PseudowireIdPool longIdPool = mvo.get(naef.mvo.mpls.Pseudowire.Attr.LONG_IDPOOL);
                Long longId = longIdPool == null ? null : longIdPool.getId(mvo);
                if (longId != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, longIdPool, longId);
                }

                naef.mvo.mpls.PseudowireStringIdPool stringIdPool
                    = mvo.get(naef.mvo.mpls.Pseudowire.Attr.STRING_IDPOOL);
                String stringId = stringIdPool == null ? null : stringIdPool.getId(mvo);
                if (stringId != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, stringIdPool, stringId);
                }

                return null;
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.mpls.PseudowireIdPoolDto, naef.mvo.mpls.Pseudowire>(
                naef.dto.mpls.PseudowireDto.class,
                naef.dto.mpls.PseudowireDto.ExtAttr.PSEUDOWIRE_ID_POOL,
                naef.mvo.mpls.Pseudowire.Attr.ID_POOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<Long, naef.mvo.mpls.Pseudowire>(
            naef.dto.mpls.PseudowireDto.class,
            naef.dto.mpls.PseudowireDto.ExtAttr.VC_ID,
            EvalStrategy.LAZY)
        {
            @Override public Long get(naef.mvo.mpls.Pseudowire mvo) {
                naef.mvo.mpls.PseudowireIdPool idPool = mvo.get(naef.mvo.mpls.Pseudowire.Attr.ID_POOL);
                return idPool == null ? null : idPool.getId(mvo);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.mpls.PseudowireLongIdPoolDto, naef.mvo.mpls.Pseudowire>(
                naef.dto.mpls.PseudowireDto.class,
                naef.dto.mpls.PseudowireDto.ExtAttr.LONG_IDPOOL,
                naef.mvo.mpls.Pseudowire.Attr.LONG_IDPOOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<Long, naef.mvo.mpls.Pseudowire>(
            naef.dto.mpls.PseudowireDto.class,
            naef.dto.mpls.PseudowireDto.ExtAttr.LONG_ID,
            EvalStrategy.LAZY)
        {
            @Override public Long get(naef.mvo.mpls.Pseudowire mvo) {
                naef.mvo.mpls.PseudowireIdPool longIdPool = mvo.get(naef.mvo.mpls.Pseudowire.Attr.LONG_IDPOOL);
                return longIdPool == null ? null : longIdPool.getId(mvo);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.mpls.PseudowireStringIdPoolDto, naef.mvo.mpls.Pseudowire>(
                naef.dto.mpls.PseudowireDto.class,
                naef.dto.mpls.PseudowireDto.ExtAttr.STRING_IDPOOL,
                naef.mvo.mpls.Pseudowire.Attr.STRING_IDPOOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.mpls.Pseudowire>(
            naef.dto.mpls.PseudowireDto.class,
            naef.dto.mpls.PseudowireDto.ExtAttr.STRING_ID,
            EvalStrategy.LAZY)
        {
            @Override public String get(naef.mvo.mpls.Pseudowire mvo) {
                naef.mvo.mpls.PseudowireStringIdPool stringIdPool
                    = mvo.get(naef.mvo.mpls.Pseudowire.Attr.STRING_IDPOOL);
                return stringIdPool == null ? null : stringIdPool.getId(mvo);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleAttr<naef.mvo.mpls.Pseudowire.TransportType, naef.mvo.mpls.Pseudowire>(
                naef.dto.mpls.PseudowireDto.class,
                naef.dto.mpls.PseudowireDto.ExtAttr.TRANSPORT_TYPE,
                naef.mvo.mpls.Pseudowire.Attr.TRANSPORT_TYPE,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRef<naef.dto.PortDto, naef.mvo.mpls.Pseudowire>(
            naef.dto.mpls.PseudowireDto.class,
            naef.dto.mpls.PseudowireDto.ExtAttr.AC1,
            EvalStrategy.LAZY)
        {
            @Override public naef.mvo.Port getValue(naef.mvo.mpls.Pseudowire mvo) {
                return mvo.getAttachmentCircuit1();
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRef<naef.dto.PortDto, naef.mvo.mpls.Pseudowire>(
            naef.dto.mpls.PseudowireDto.class,
            naef.dto.mpls.PseudowireDto.ExtAttr.AC2,
            EvalStrategy.LAZY)
        {
            @Override public naef.mvo.Port getValue(naef.mvo.mpls.Pseudowire mvo) {
                return mvo.getAttachmentCircuit2();
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.mpls.RsvpLspDto, naef.mvo.mpls.Pseudowire>(
            naef.dto.mpls.PseudowireDto.class,
            naef.dto.mpls.PseudowireDto.ExtAttr.RSVPLSPS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.mpls.RsvpLsp> getValues(naef.mvo.mpls.Pseudowire mvo) {
                return select(naef.mvo.mpls.RsvpLsp.class, mvo.getCurrentLowerLayers(false));
            }
        });

        installUiTypeName(naef.mvo.mpls.PseudowireIdPool.class, "pseudowire.id-pool.long-type");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.mpls.PseudowireIdPool>(naef.mvo.mpls.PseudowireIdPool.home));
        installMvoDtoMapping(naef.mvo.mpls.PseudowireIdPool.class, naef.dto.mpls.PseudowireLongIdPoolDto.class);

        installUiTypeName(naef.mvo.mpls.PseudowireStringIdPool.class, "pseudowire.id-pool.string-type");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.mpls.PseudowireStringIdPool>(
                naef.mvo.mpls.PseudowireStringIdPool.home));
        installMvoDtoMapping(naef.mvo.mpls.PseudowireStringIdPool.class, naef.dto.mpls.PseudowireStringIdPoolDto.class);

        installUiTypeName(naef.mvo.mpls.RsvpLsp.class, "rsvp-lsp");
        installAttributes(naef.mvo.mpls.RsvpLsp.class, naef.mvo.mpls.RsvpLsp.Attr.class);
        installMvoDtoMapping(naef.mvo.mpls.RsvpLsp.class, naef.dto.mpls.RsvpLspDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.mpls.RsvpLsp, naef.dto.mpls.RsvpLspDto>(
            naef.dto.mpls.RsvpLspDto.class)
        {
            @Override public void initialize(naef.mvo.mpls.RsvpLsp mvo, naef.dto.mpls.RsvpLspDto dto) {
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.ACTIVE_HOP_SERIES,
                    MvoDtoDesc.<naef.dto.mpls.RsvpLspHopSeriesDto>build1(
                        mvo.get(naef.mvo.mpls.RsvpLsp.Attr.ACTIVE_HOP_SERIES)));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.HOP_SERIES_1,
                    MvoDtoDesc.<naef.dto.mpls.RsvpLspHopSeriesDto>build1(
                        mvo.get(naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_1)));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.HOP_SERIES_2,
                    MvoDtoDesc.<naef.dto.mpls.RsvpLspHopSeriesDto>build1(
                        mvo.get(naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_2)));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.HOP_SERIES_3,
                    MvoDtoDesc.<naef.dto.mpls.RsvpLspHopSeriesDto>build1(
                        mvo.get(naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_3)));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.HOP_SERIES_4,
                    MvoDtoDesc.<naef.dto.mpls.RsvpLspHopSeriesDto>build1(
                        mvo.get(naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_4)));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.HOP_SERIES_5,
                    MvoDtoDesc.<naef.dto.mpls.RsvpLspHopSeriesDto>build1(
                        mvo.get(naef.mvo.mpls.RsvpLsp.Attr.HOP_SERIES_5)));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.ACTUAL_HOP_SERIES,
                    MvoDtoDesc.<naef.dto.mpls.RsvpLspHopSeriesDto>build1(
                        mvo.get(naef.mvo.mpls.RsvpLsp.Attr.ACTUAL_HOP_SERIES)));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.INGRESS_NODE,
                    MvoDtoDesc.<naef.dto.NodeDto>build1(mvo.getIngressNode()));
                dto.set(
                    naef.dto.mpls.RsvpLspDto.ExtAttr.EGRESS_NODE,
                    MvoDtoDesc.<naef.dto.NodeDto>build1(mvo.getEgressNode()));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.mpls.RsvpLsp>(
            naef.dto.mpls.RsvpLspDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.mpls.RsvpLsp mvo) {
                naef.mvo.mpls.RsvpLspIdPool idpool = mvo.get(naef.mvo.mpls.RsvpLsp.Attr.IDPOOL);
                String id = idpool == null ? null : idpool.getId(mvo);
                return getNetworkAbsoluteNameByIdPool(mvo, idpool, id);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.mpls.RsvpLsp>(
            naef.dto.mpls.RsvpLspDto.class,
            tef.skelton.Attribute.NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.mpls.RsvpLsp mvo) {
                naef.mvo.mpls.RsvpLspIdPool idpool = mvo.get(naef.mvo.mpls.RsvpLsp.Attr.IDPOOL);
                String id = idpool == null ? null : idpool.getId(mvo);
                return id;
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.mpls.RsvpLspIdPoolDto, naef.mvo.mpls.RsvpLsp>(
                naef.dto.mpls.RsvpLspDto.class,
                naef.dto.mpls.RsvpLspDto.ExtAttr.IDPOOL,
                naef.mvo.mpls.RsvpLsp.Attr.IDPOOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.mpls.RsvpLsp>(
            naef.dto.mpls.RsvpLspDto.class,
            naef.dto.mpls.RsvpLspDto.ExtAttr.ID,
            EvalStrategy.LAZY)
        {
            @Override public String get(naef.mvo.mpls.RsvpLsp mvo) {
                naef.mvo.mpls.RsvpLspIdPool idpool = mvo.get(naef.mvo.mpls.RsvpLsp.Attr.IDPOOL);
                String id = idpool == null ? null : idpool.getId(mvo);
                return id;
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.mpls.PseudowireDto, naef.mvo.mpls.RsvpLsp>(
            naef.dto.mpls.RsvpLspDto.class,
            naef.dto.mpls.RsvpLspDto.ExtAttr.PSEUDOWIRES,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.mpls.Pseudowire> getValues(naef.mvo.mpls.RsvpLsp mvo) {
                return select(naef.mvo.mpls.Pseudowire.class, mvo.getCurrentUpperLayers(false));
            }
        });

        installUiTypeName(naef.mvo.mpls.RsvpLspHop.class, "rsvp-lsp-hop");
        installMvoDtoMapping(naef.mvo.mpls.RsvpLspHop.class, naef.dto.PathHopDto.class);

        installUiTypeName(naef.mvo.mpls.RsvpLspHopSeries.class, "rsvp-lsp-hop-series");
        installAttributes(naef.mvo.mpls.RsvpLspHopSeries.class, naef.mvo.mpls.RsvpLspHopSeries.Attr.class);
        installMvoDtoMapping(naef.mvo.mpls.RsvpLspHopSeries.class, naef.dto.mpls.RsvpLspHopSeriesDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.mpls.RsvpLspHopSeries, naef.dto.mpls.RsvpLspHopSeriesDto>(
            naef.dto.mpls.RsvpLspHopSeriesDto.class)
        {
            @Override public void initialize(
                naef.mvo.mpls.RsvpLspHopSeries mvo,
                naef.dto.mpls.RsvpLspHopSeriesDto dto)
            {
                dto.set(naef.dto.mpls.RsvpLspHopSeriesDto.ExtAttr.HOPS, buildHopList(mvo.getLastHop()));
                dto.set(
                    naef.dto.mpls.RsvpLspHopSeriesDto.ExtAttr.INGRESS_NODE,
                    MvoDtoDesc.<naef.dto.NodeDto>build1(mvo.getIngressNode()));
                dto.set(
                    naef.dto.mpls.RsvpLspHopSeriesDto.ExtAttr.EGRESS_NODE,
                    MvoDtoDesc.<naef.dto.NodeDto>build1(mvo.getEgressNode()));
            }

            private List<EntityDto.Desc<naef.dto.PathHopDto>> buildHopList(naef.mvo.mpls.RsvpLspHop lastHop) {
                List<EntityDto.Desc<naef.dto.PathHopDto>> hoplist
                    = new ArrayList<EntityDto.Desc<naef.dto.PathHopDto>>();
                for (naef.mvo.mpls.RsvpLspHop hop : NaefMvoUtils.getHops(lastHop)) {
                    hoplist.add(MvoDtoDesc.<naef.dto.PathHopDto>build1(hop));
                }
                return hoplist;
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.mpls.RsvpLspHopSeries>(
            naef.dto.mpls.RsvpLspHopSeriesDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.mpls.RsvpLspHopSeries mvo) {
                naef.mvo.mpls.RsvpLspHopSeriesIdPool idpool = mvo.get(naef.mvo.mpls.RsvpLspHopSeries.Attr.ID_POOL);
                String id = idpool == null ? null : idpool.getId(mvo);
                return getNetworkAbsoluteNameByIdPool(mvo, idpool, id);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.mpls.RsvpLspHopSeries>(
            naef.dto.mpls.RsvpLspHopSeriesDto.class,
            tef.skelton.Attribute.NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.mpls.RsvpLspHopSeries mvo) {
                naef.mvo.mpls.RsvpLspHopSeriesIdPool idpool = mvo.get(naef.mvo.mpls.RsvpLspHopSeries.Attr.ID_POOL);
                String id = idpool == null ? null : idpool.getId(mvo);
                return id;
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<
                    naef.dto.mpls.RsvpLspHopSeriesIdPoolDto,
                    naef.mvo.mpls.RsvpLspHopSeries>(
                naef.dto.mpls.RsvpLspHopSeriesDto.class,
                naef.dto.mpls.RsvpLspHopSeriesDto.ExtAttr.ID_POOL,
                naef.mvo.mpls.RsvpLspHopSeries.Attr.ID_POOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.mpls.RsvpLspDto, naef.mvo.mpls.RsvpLspHopSeries>(
            naef.dto.mpls.RsvpLspHopSeriesDto.class,
            naef.dto.mpls.RsvpLspHopSeriesDto.ExtAttr.RSVPLSPS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.mpls.RsvpLsp> getValues(naef.mvo.mpls.RsvpLspHopSeries mvo) {
                return select(naef.mvo.mpls.RsvpLsp.class, mvo.getCurrentUpperLayers(false));
            }
        });

        installUiTypeName(naef.mvo.mpls.RsvpLspHopSeriesIdPool.class, "rsvp-lsp-hop-series.id-pool");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.mpls.RsvpLspHopSeriesIdPool>(
                naef.mvo.mpls.RsvpLspHopSeriesIdPool.home));
        installMvoDtoMapping(naef.mvo.mpls.RsvpLspHopSeriesIdPool.class, naef.dto.mpls.RsvpLspHopSeriesIdPoolDto.class);

        installUiTypeName(naef.mvo.mpls.RsvpLspIdPool.class, "rsvp-lsp.id-pool");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.mpls.RsvpLspIdPool>(naef.mvo.mpls.RsvpLspIdPool.home));
        installMvoDtoMapping(naef.mvo.mpls.RsvpLspIdPool.class, naef.dto.mpls.RsvpLspIdPoolDto.class);

        installUiTypeName(naef.mvo.of.OfPatchLink.class, "of-patch-link");
        installAttributes(naef.mvo.of.OfPatchLink.class, naef.mvo.of.OfPatchLink.class);
        installMvoDtoMapping(naef.mvo.of.OfPatchLink.class, naef.dto.of.OfPatchLinkDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.of.OfPatchLink>(
            naef.dto.of.OfPatchLinkDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.of.OfPatchLink mvo) {
                naef.mvo.of.OfPatchLink.PatchIdPool idPool = mvo.get(naef.mvo.of.OfPatchLink.PATCH_ID_POOL);
                String id = idPool == null ? null : idPool.getId(mvo);
                return id == null
                    ? null
                    : getNetworkAbsoluteNameByIdPool(mvo, idPool, id);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.of.OfPatchLinkDto.PatchIdPoolDto, naef.mvo.of.OfPatchLink>(
                naef.dto.of.OfPatchLinkDto.class,
                naef.dto.of.OfPatchLinkDto.PATCH_ID_POOL,
                naef.mvo.of.OfPatchLink.PATCH_ID_POOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.of.OfPatchLink>(
            naef.dto.of.OfPatchLinkDto.class,
            naef.dto.of.OfPatchLinkDto.PATCH_ID,
            EvalStrategy.LAZY)
        {
            @Override public String get(naef.mvo.of.OfPatchLink mvo) {
                naef.mvo.of.OfPatchLink.PatchIdPool idpool = mvo.get(naef.mvo.of.OfPatchLink.PATCH_ID_POOL);
                return idpool == null ? null : idpool.getId(mvo);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRef<naef.dto.PortDto, naef.mvo.of.OfPatchLink>(
                naef.dto.of.OfPatchLinkDto.class,
                naef.dto.of.OfPatchLinkDto.PATCH_PORT1,
                EvalStrategy.LAZY)
            {
                @Override public naef.mvo.Port getValue(naef.mvo.of.OfPatchLink mvo) {
                    return mvo.getPatchPort1();
                }
            });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRef<naef.dto.PortDto, naef.mvo.of.OfPatchLink>(
                naef.dto.of.OfPatchLinkDto.class,
                naef.dto.of.OfPatchLinkDto.PATCH_PORT2,
                EvalStrategy.LAZY)
            {
                @Override public naef.mvo.Port getValue(naef.mvo.of.OfPatchLink mvo) {
                    return mvo.getPatchPort2();
                }
            });

        installUiTypeName(naef.mvo.of.OfPatchLink.PatchIdPool.class, "of-patch-link.patch-id-pool");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.of.OfPatchLink.PatchIdPool>(
                naef.mvo.of.OfPatchLink.PatchIdPool.home));
        installMvoDtoMapping(
            naef.mvo.of.OfPatchLink.PatchIdPool.class,
            naef.dto.of.OfPatchLinkDto.PatchIdPoolDto.class);

        installUiTypeName(naef.mvo.ospf.OspfAreaId.class, "ospf.area-id");

        installUiTypeName(naef.mvo.ospf.OspfProcess.class, "ospf-process");
        installAttributes(naef.mvo.ospf.OspfProcess.class, naef.mvo.ospf.OspfProcess.Attr.class);
        installMvoDtoMapping(naef.mvo.ospf.OspfProcess.class, naef.dto.ospf.OspfProcessDto.class);

        installUiTypeName(naef.mvo.pos.PosApsIf.class, "pos-aps-if");
        installAttributes(naef.mvo.pos.PosApsIf.class, naef.mvo.atm.AtmPort.Attr.ATM_ENABLED);
        installMvoDtoMapping(naef.mvo.pos.PosApsIf.class, naef.dto.pos.PosApsIfDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.pos.PosPortDto, naef.mvo.pos.PosApsIf>(
            naef.dto.pos.PosApsIfDto.class,
            naef.dto.pos.PosApsIfDto.ExtAttr.POS_PORTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.pos.PosPort> getValues(naef.mvo.pos.PosApsIf mvo) {
                return (Set<naef.mvo.pos.PosPort>) asSet(mvo.getParts());
            }
        });

        installUiTypeName(naef.mvo.pos.PosApsLink.class, "pos-aps-link");

        installUiTypeName(naef.mvo.pos.PosLink.class, "pos-link");

        installUiTypeName(naef.mvo.pos.PosPort.class, "pos-port");
        installAttributes(
            naef.mvo.pos.PosPort.class,
            naef.mvo.atm.AtmPort.Attr.ATM_ENABLED,
            naef.mvo.fr.FrPvcType.ATTRIBUTE);
        installMvoDtoMapping(naef.mvo.pos.PosPort.class, naef.dto.pos.PosPortDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.pos.PosPort, naef.dto.pos.PosPortDto>(
            naef.dto.pos.PosPortDto.class)
        {
            @Override public void initialize(naef.mvo.pos.PosPort mvo, naef.dto.pos.PosPortDto dto) {
                if (mvo.getContainer() instanceof naef.mvo.pos.PosApsIf) {
                    dto.set(
                        naef.dto.pos.PosPortDto.ExtAttr.POS_APS_IF,
                        MvoDtoDesc.<naef.dto.pos.PosApsIfDto>build1(mvo.getContainer()));
                }
            }
        });

        installUiTypeName(naef.mvo.rip.RipProcess.class, "rip-process");
        installMvoDtoMapping(naef.mvo.rip.RipProcess.class, naef.dto.rip.RipProcessDto.class);

        installUiTypeName(naef.mvo.serial.SerialLink.class, "serial-link");

        installUiTypeName(naef.mvo.serial.SerialPort.class, "serial-port");
        installAttributes(
            naef.mvo.serial.SerialPort.class,
            naef.mvo.atm.AtmPort.Attr.ATM_ENABLED,
            naef.mvo.fr.FrPvcType.ATTRIBUTE);
        installMvoDtoMapping(naef.mvo.serial.SerialPort.class, naef.dto.serial.SerialPortDto.class);

        installUiTypeName(naef.mvo.serial.TdmSerialIf.class, "tdm-serial-if");
        installAttributes(naef.mvo.serial.TdmSerialIf.class, naef.mvo.serial.TdmSerialIf.Attr.class);
        installMvoDtoMapping(naef.mvo.serial.TdmSerialIf.class, naef.dto.serial.TdmSerialIfDto.class);

        installUiTypeName(naef.mvo.vlan.MultipointVlanSegment.class, "multipoint-vlan-segment");
        installMvoDtoMapping(naef.mvo.vlan.MultipointVlanSegment.class, naef.dto.vlan.MultipointVlanSegmentDto.class);

        installUiTypeName(naef.mvo.vlan.Vlan.class, "vlan");
        installAttributes(naef.mvo.vlan.Vlan.class, naef.mvo.vlan.Vlan.Attr.class);
        installMvoDtoMapping(naef.mvo.vlan.Vlan.class, naef.dto.vlan.VlanDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.vlan.Vlan>(
            naef.dto.vlan.VlanDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.vlan.Vlan mvo) {
                naef.mvo.vlan.VlanIdPool idPool = mvo.get(naef.mvo.vlan.Vlan.Attr.ID_POOL);
                Integer vlanId = idPool == null ? null : idPool.getId(mvo);
                return getNetworkAbsoluteNameByIdPool(mvo, idPool, vlanId);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRefAttr<naef.dto.vlan.VlanIdPoolDto, naef.mvo.vlan.Vlan>(
            naef.dto.vlan.VlanDto.class,
            naef.dto.vlan.VlanDto.ExtAttr.VLAN_ID_POOL,
            naef.mvo.vlan.Vlan.Attr.ID_POOL,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<Integer, naef.mvo.vlan.Vlan>(
            naef.dto.vlan.VlanDto.class,
            naef.dto.vlan.VlanDto.ExtAttr.VLAN_ID,
            EvalStrategy.LAZY)
        {
            @Override public Integer get(naef.mvo.vlan.Vlan mvo) {
                naef.mvo.vlan.VlanIdPool idPool = mvo.get(naef.mvo.vlan.Vlan.Attr.ID_POOL);
                return idPool != null
                    ? idPool.getId(mvo)
                    : mvo.get(naef.mvo.vlan.Vlan.Attr.ID);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.vlan.VlanIfDto, naef.mvo.vlan.Vlan>(
            naef.dto.vlan.VlanDto.class,
            naef.dto.vlan.VlanDto.ExtAttr.MEMBER_VLAN_IF,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.vlan.VlanIf> getValues(naef.mvo.vlan.Vlan mvo) {
                return select(naef.mvo.vlan.VlanIf.class, mvo.getCurrentMemberPorts());
            }
        });

        installUiTypeName(naef.mvo.vlan.VlanIdPool.Dot1q.class, "vlan.id-pool.dot1q");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.vlan.VlanIdPool.Dot1q>(naef.mvo.vlan.VlanIdPool.Dot1q.home));
        installMvoDtoMapping(naef.mvo.vlan.VlanIdPool.Dot1q.class, naef.dto.vlan.VlanIdPoolDto.class);

        installUiTypeName(naef.mvo.vlan.VlanIf.class, "vlan-if");
        installAttributes(naef.mvo.vlan.VlanIf.class, naef.mvo.vlan.VlanIf.Attr.class);
        installAttributes(naef.mvo.vlan.VlanIf.class, naef.mvo.vlan.VlanAttrs.ENABLED_NETWORKING_FUNCTION_VLAN);
        installMvoDtoMapping(naef.mvo.vlan.VlanIf.class, naef.dto.vlan.VlanIfDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.vlan.VlanIf, naef.dto.vlan.VlanIfDto>(
            naef.dto.vlan.VlanIfDto.class)
        {
            @Override public void initialize(naef.mvo.vlan.VlanIf mvo, naef.dto.vlan.VlanIfDto dto) {
                dto.set(naef.dto.vlan.VlanIfDto.ExtAttr.VLAN_ID, mvo.get(naef.mvo.vlan.VlanIf.Attr.VLAN_ID));
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SetRefAttr<naef.dto.vlan.VlanSegmentGatewayIfDto, naef.mvo.vlan.VlanIf>(
                naef.dto.vlan.VlanIfDto.class,
                naef.dto.vlan.VlanIfDto.ExtAttr.VLAN_SEGMENT_GATEWAY_IFS,
                naef.mvo.vlan.VlanIf.Attr.VLAN_SEGMENT_GATEWAY_IFS, 
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.vlan.VlanIf>(
            naef.dto.vlan.VlanIfDto.class,
            naef.dto.vlan.VlanIfDto.ExtAttr.TAGGED_PORTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.Port> getValues(naef.mvo.vlan.VlanIf mvo) {
                return mvo.getCurrentTaggedPorts();
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.vlan.VlanIf>(
            naef.dto.vlan.VlanIfDto.class,
            naef.dto.vlan.VlanIfDto.ExtAttr.UNTAGGED_PORTS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Port>
                getValues(naef.mvo.vlan.VlanIf mvo)
            {
                return asSet(mvo.getCurrentUntaggedPorts());
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRef<naef.dto.vlan.VlanDto, naef.mvo.vlan.VlanIf>(
            naef.dto.vlan.VlanIfDto.class,
            naef.dto.vlan.VlanIfDto.ExtAttr.VLAN,
            EvalStrategy.LAZY)
        {
            @Override protected naef.mvo.vlan.Vlan getValue(naef.mvo.vlan.VlanIf mvo) {
                return mvo.getVlan();
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.vlan.VlanSegmentDto, naef.mvo.vlan.VlanIf>(
            naef.dto.vlan.VlanIfDto.class,
            naef.dto.vlan.VlanIfDto.ExtAttr.VLAN_LINKS,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.vlan.VlanLink> getValues(naef.mvo.vlan.VlanIf mvo) {
                return mvo.getCurrentNetworks(naef.mvo.vlan.VlanLink.class);
            }
        });

        installUiTypeName(naef.mvo.vlan.VlanLink.class, "vlan-link");
        installMvoDtoMapping(naef.mvo.vlan.VlanLink.class, naef.dto.vlan.VlanLinkDto.class);

        installUiTypeName(naef.mvo.vlan.VlanSegment.class, "vlan-segment");
        installAttributes(naef.mvo.vlan.VlanSegment.class, naef.mvo.vlan.VlanSegment.Attr.class);
        installDtoAttrTranscript(
            new DtoAttrTranscript.SetRefAttr<naef.dto.vlan.VlanSegmentGatewayIfDto, naef.mvo.vlan.VlanSegment>(
                naef.dto.vlan.VlanSegmentDto.class,
                naef.dto.vlan.VlanSegmentDto.ExtAttr.VLAN_SEGMENT_GATEWAY_IFS, 
                naef.mvo.vlan.VlanSegment.Attr.VLAN_SEGMENT_GATEWAY_IFS,
                EvalStrategy.LAZY));

        installUiTypeName(naef.mvo.vlan.VlanSegmentGatewayIf.class, "vlan-segment-gateway-if");
        installAttributes(naef.mvo.vlan.VlanSegmentGatewayIf.class, naef.mvo.vlan.VlanSegmentGatewayIf.Attr.class);
        installMvoDtoMapping(naef.mvo.vlan.VlanSegmentGatewayIf.class, naef.dto.vlan.VlanSegmentGatewayIfDto.class);
        installDtoInitializer(
            new DtoInitializer<naef.mvo.vlan.VlanSegmentGatewayIf, naef.dto.vlan.VlanSegmentGatewayIfDto>(
                naef.dto.vlan.VlanSegmentGatewayIfDto.class)
            {
                @Override public void initialize(
                    naef.mvo.vlan.VlanSegmentGatewayIf mvo,
                    naef.dto.vlan.VlanSegmentGatewayIfDto dto)
                {
                    naef.mvo.vlan.VlanSegment vlansegment
                        = naef.mvo.vlan.VlanSegmentGatewayIf.Attr.VLAN_SEGMENT.get(mvo);
                    if (vlansegment instanceof naef.mvo.vlan.VlanLink) {
                        dto.set(
                            naef.dto.vlan.VlanSegmentGatewayIfDto.ExtAttr.VLAN_LINK,
                            MvoDtoDesc.<naef.dto.vlan.VlanLinkDto>build1((naef.mvo.vlan.VlanLink) vlansegment));
                    }
                    dto.set(
                        naef.dto.vlan.VlanSegmentGatewayIfDto.ExtAttr.VLAN_IF,
                        MvoDtoDesc.<naef.dto.vlan.VlanIfDto>build1(
                            naef.mvo.vlan.VlanSegmentGatewayIf.Attr.VLAN_IF.get(mvo)));
                }
            });

        installUiTypeName(naef.mvo.vpls.Vpls.class, "vpls");
        installAttributes(naef.mvo.vpls.Vpls.class, naef.mvo.vpls.Vpls.Attr.class);
        installMvoDtoMapping(naef.mvo.vpls.Vpls.class, naef.dto.vpls.VplsDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.vpls.Vpls>(
            naef.dto.vpls.VplsDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.vpls.Vpls mvo) {
                naef.mvo.vpls.VplsIdPool.IntegerType idPool
                    = (naef.mvo.vpls.VplsIdPool.IntegerType) mvo.get(naef.mvo.vpls.Vpls.Attr.ID_POOL);
                Integer id = idPool == null ? null : idPool.getId(mvo);
                if (id != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, idPool, id);
                }

                naef.mvo.vpls.VplsIdPool.IntegerType integerIdPool = mvo.get(naef.mvo.vpls.Vpls.Attr.INTEGER_IDPOOL);
                Integer integerId = integerIdPool == null ? null : integerIdPool.getId(mvo);
                if (integerId != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, integerIdPool, integerId);
                }

                naef.mvo.vpls.VplsIdPool.StringType stringIdPool = mvo.get(naef.mvo.vpls.Vpls.Attr.STRING_IDPOOL);
                String stringId = stringIdPool == null ? null : stringIdPool.getId(mvo);
                if (stringId != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, stringIdPool, stringId);
                }

                return null;
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.vpls.VplsIntegerIdPoolDto, naef.mvo.vpls.Vpls>(
                naef.dto.vpls.VplsDto.class,
                naef.dto.vpls.VplsDto.ExtAttr.VPLS_INTEGER_IDPOOL,
                naef.mvo.vpls.Vpls.Attr.INTEGER_IDPOOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<Integer, naef.mvo.vpls.Vpls>(
            naef.dto.vpls.VplsDto.class,
            naef.dto.vpls.VplsDto.ExtAttr.VPLS_INTEGER_ID,
            EvalStrategy.LAZY)
        {
            @Override public Integer get(naef.mvo.vpls.Vpls mvo) {
                naef.mvo.vpls.VplsIdPool.IntegerType integerIdPool = mvo.get(naef.mvo.vpls.Vpls.Attr.INTEGER_IDPOOL);
                return integerIdPool == null ? null : integerIdPool.getId(mvo);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.vpls.VplsStringIdPoolDto, naef.mvo.vpls.Vpls>(
                naef.dto.vpls.VplsDto.class,
                naef.dto.vpls.VplsDto.ExtAttr.VPLS_STRING_IDPOOL,
                naef.mvo.vpls.Vpls.Attr.STRING_IDPOOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.vpls.Vpls>(
            naef.dto.vpls.VplsDto.class,
            naef.dto.vpls.VplsDto.ExtAttr.VPLS_STRING_ID,
            EvalStrategy.LAZY)
        {
            @Override public String get(naef.mvo.vpls.Vpls mvo) {
                naef.mvo.vpls.VplsIdPool.StringType stringIdPool = mvo.get(naef.mvo.vpls.Vpls.Attr.STRING_IDPOOL);
                return stringIdPool == null ? null : stringIdPool.getId(mvo);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.vpls.VplsIfDto, naef.mvo.vpls.Vpls>(
            naef.dto.vpls.VplsDto.class,
            naef.dto.vpls.VplsDto.ExtAttr.MEMBER_VPLS_IF,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.vpls.VplsIf> getValues(naef.mvo.vpls.Vpls mvo) {
                return select(naef.mvo.vpls.VplsIf.class, mvo.getCurrentMemberPorts());
            }
        });

        installUiTypeName(naef.mvo.vpls.VplsIdPool.IntegerType.class, "vpls.id-pool.integer-type");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.vpls.VplsIdPool.IntegerType>(
                naef.mvo.vpls.VplsIdPool.IntegerType.home));
        installMvoDtoMapping(naef.mvo.vpls.VplsIdPool.IntegerType.class, naef.dto.vpls.VplsIntegerIdPoolDto.class);

        installUiTypeName(naef.mvo.vpls.VplsIdPool.StringType.class, "vpls.id-pool.string-type");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.vpls.VplsIdPool.StringType>(
                naef.mvo.vpls.VplsIdPool.StringType.home));
        installMvoDtoMapping(naef.mvo.vpls.VplsIdPool.StringType.class, naef.dto.vpls.VplsStringIdPoolDto.class);

        installUiTypeName(naef.mvo.vpls.VplsIf.class, "vpls-if");
        installAttributes(naef.mvo.vpls.VplsIf.class, naef.mvo.vpls.VplsIf.Attr.class);
        installMvoDtoMapping(naef.mvo.vpls.VplsIf.class, naef.dto.vpls.VplsIfDto.class);
        installDtoInitializer(new DtoInitializer<naef.mvo.vpls.VplsIf, naef.dto.vpls.VplsIfDto>(
            naef.dto.vpls.VplsIfDto.class)
        {
            @Override public void initialize(naef.mvo.vpls.VplsIf mvo, naef.dto.vpls.VplsIfDto dto) {
                dto.set(naef.dto.vpls.VplsIfDto.ExtAttr.VPLS_MEI, mvo.getName());
                dto.set(
                    naef.dto.vpls.VplsIfDto.ExtAttr.TRAFFIC_DOMAIN,
                    MvoDtoDesc.<naef.dto.vpls.VplsDto>build1(mvo.getVpls()));
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.vpls.VplsIf>(
            naef.dto.vpls.VplsIfDto.class,
            naef.dto.vpls.VplsIfDto.ExtAttr.SETSUZOKU_PORT,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Port> getValues(naef.mvo.vpls.VplsIf mvo) {
                return asSet(mvo.getCurrentCrossConnectedPorts());
            }
        });

        installUiTypeName(naef.mvo.vrf.Vrf.class, "vrf");
        installAttributes(naef.mvo.vrf.Vrf.class, naef.mvo.vrf.Vrf.Attr.class);
        installMvoDtoMapping(naef.mvo.vrf.Vrf.class, naef.dto.vrf.VrfDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.vrf.Vrf>(
            naef.dto.vrf.VrfDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.vrf.Vrf mvo) {
                naef.mvo.vrf.VrfIdPool.IntegerType idPool
                    = (naef.mvo.vrf.VrfIdPool.IntegerType) mvo.get(naef.mvo.vrf.Vrf.Attr.ID_POOL);
                Integer id = idPool == null ? null : idPool.getId(mvo);
                if (id != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, idPool, id);
                }

                naef.mvo.vrf.VrfIdPool.IntegerType integerIdPool = mvo.get(naef.mvo.vrf.Vrf.Attr.INTEGER_IDPOOL);
                Integer integerId = integerIdPool == null ? null : integerIdPool.getId(mvo);
                if (integerId != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, integerIdPool, integerId);
                }

                naef.mvo.vrf.VrfIdPool.StringType stringIdPool = mvo.get(naef.mvo.vrf.Vrf.Attr.STRING_IDPOOL);
                String stringId = stringIdPool == null ? null : stringIdPool.getId(mvo);
                if (stringId != null) {
                    return getNetworkAbsoluteNameByIdPool(mvo, stringIdPool, stringId);
                }

                return null;
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.vrf.VrfIntegerIdPoolDto, naef.mvo.vrf.Vrf>(
                naef.dto.vrf.VrfDto.class,
                naef.dto.vrf.VrfDto.ExtAttr.VRF_INTEGER_IDPOOL,
                naef.mvo.vrf.Vrf.Attr.INTEGER_IDPOOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<Integer, naef.mvo.vrf.Vrf>(
            naef.dto.vrf.VrfDto.class,
            naef.dto.vrf.VrfDto.ExtAttr.VRF_INTEGER_ID,
            EvalStrategy.LAZY)
        {
            @Override public Integer get(naef.mvo.vrf.Vrf mvo) {
                naef.mvo.vrf.VrfIdPool.IntegerType integerIdPool = mvo.get(naef.mvo.vrf.Vrf.Attr.INTEGER_IDPOOL);
                return integerIdPool == null ? null : integerIdPool.getId(mvo);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SingleRefAttr<naef.dto.vrf.VrfStringIdPoolDto, naef.mvo.vrf.Vrf>(
            naef.dto.vrf.VrfDto.class,
            naef.dto.vrf.VrfDto.ExtAttr.VRF_STRING_IDPOOL,
            naef.mvo.vrf.Vrf.Attr.STRING_IDPOOL,
            EvalStrategy.LAZY));
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.vrf.Vrf>(
            naef.dto.vrf.VrfDto.class,
            naef.dto.vrf.VrfDto.ExtAttr.VRF_STRING_ID,
            EvalStrategy.LAZY)
        {
            @Override public String get(naef.mvo.vrf.Vrf mvo) {
                naef.mvo.vrf.VrfIdPool.StringType stringIdPool = mvo.get(naef.mvo.vrf.Vrf.Attr.STRING_IDPOOL);
                return stringIdPool == null ? null : stringIdPool.getId(mvo);
            }
        });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.vrf.VrfIfDto, naef.mvo.vrf.Vrf>(
            naef.dto.vrf.VrfDto.class,
            naef.dto.vrf.VrfDto.ExtAttr.MEMBER_VRF_IF,
            EvalStrategy.LAZY)
        {
            @Override protected Set<naef.mvo.vrf.VrfIf> getValues(naef.mvo.vrf.Vrf mvo) {
                return select(naef.mvo.vrf.VrfIf.class, mvo.getCurrentMemberPorts());
            }
        });

        installUiTypeName(naef.mvo.vrf.VrfIdPool.IntegerType.class, "vrf.id-pool.integer-type");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.vrf.VrfIdPool.IntegerType>(
                naef.mvo.vrf.VrfIdPool.IntegerType.home));
        installMvoDtoMapping(naef.mvo.vrf.VrfIdPool.IntegerType.class, naef.dto.vrf.VrfIntegerIdPoolDto.class);

        installUiTypeName(naef.mvo.vrf.VrfIdPool.StringType.class, "vrf.id-pool.string-type");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.vrf.VrfIdPool.StringType>(naef.mvo.vrf.VrfIdPool.StringType.home));
        installMvoDtoMapping(naef.mvo.vrf.VrfIdPool.StringType.class, naef.dto.vrf.VrfStringIdPoolDto.class);

        installUiTypeName(naef.mvo.vrf.VrfIf.class, "vrf-if");
        installAttributes(naef.mvo.vrf.VrfIf.class, naef.mvo.vrf.VrfIf.Attr.class);
        installMvoDtoMapping(naef.mvo.vrf.VrfIf.class, naef.dto.vrf.VrfIfDto.class);
        installDtoInitializer(
            new DtoInitializer<naef.mvo.vrf.VrfIf, naef.dto.vrf.VrfIfDto>(naef.dto.vrf.VrfIfDto.class) {

                @Override public void initialize(naef.mvo.vrf.VrfIf mvo, naef.dto.vrf.VrfIfDto dto) {
                    dto.set(naef.dto.vrf.VrfIfDto.ExtAttr.VRF_MEI, mvo.getName());
                    dto.set(
                        naef.dto.vrf.VrfIfDto.ExtAttr.TRAFFIC_DOMAIN,
                        MvoDtoDesc.<naef.dto.vrf.VrfDto>build1(mvo.getVrf()));
                }
            });
        installDtoAttrTranscript(new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.vrf.VrfIf>(
            naef.dto.vrf.VrfIfDto.class,
            naef.dto.vrf.VrfIfDto.ExtAttr.SETSUZOKU_PORT,
            EvalStrategy.LAZY)
        {
            @Override protected Set<? extends naef.mvo.Port> getValues(naef.mvo.vrf.VrfIf mvo) {
                return asSet(mvo.getCurrentCrossConnectedPorts());
            }
        });

        installUiTypeName(naef.mvo.vxlan.Vxlan.class, "vxlan");
        installAttributes(naef.mvo.vxlan.Vxlan.class, naef.mvo.vxlan.Vxlan.class);
        installMvoDtoMapping(naef.mvo.vxlan.Vxlan.class, naef.dto.vxlan.VxlanDto.class);
        installDtoAttrTranscript(new DtoAttrTranscript<String, naef.mvo.vxlan.Vxlan>(
            naef.dto.vxlan.VxlanDto.class,
            naef.dto.NaefDtoAttrs.ABSOLUTE_NAME,
            EvalStrategy.EAGER)
        {
            @Override public String get(naef.mvo.vxlan.Vxlan mvo) {
                final naef.mvo.vxlan.VxlanIdPool idPool = naef.mvo.vxlan.Vxlan.ID_POOL.get(mvo);
                final Long id = idPool == null ? null : idPool.getId(mvo);
                return id == null ? null : getNetworkAbsoluteNameByIdPool(mvo, idPool, id);
            }
        });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SingleRefAttr<naef.dto.vxlan.VxlanIdPoolDto, naef.mvo.vxlan.Vxlan>(
                naef.dto.vxlan.VxlanDto.class,
                naef.dto.vxlan.VxlanDto.VXLAN_ID_POOL,
                naef.mvo.vxlan.Vxlan.ID_POOL,
                EvalStrategy.LAZY));
        installDtoAttrTranscript(
            new DtoAttrTranscript<Long, naef.mvo.vxlan.Vxlan>(
                naef.dto.vxlan.VxlanDto.class,
                naef.dto.vxlan.VxlanDto.VXLAN_ID,
                EvalStrategy.LAZY)
            {
                @Override public Long get(Vxlan mvo) {
                    final naef.mvo.vxlan.VxlanIdPool idPool = naef.mvo.vxlan.Vxlan.ID_POOL.get(mvo);
                    return idPool == null ? null : idPool.getId(mvo);
                }
            });
        installDtoAttrTranscript(
            new DtoAttrTranscript.SetRef<naef.dto.vxlan.VtepIfDto, naef.mvo.vxlan.Vxlan>(
                naef.dto.vxlan.VxlanDto.class,
                naef.dto.vxlan.VxlanDto.VTEP_IFS,
                EvalStrategy.LAZY)
            {
                @Override public Set<naef.mvo.vxlan.VtepIf> getValues(naef.mvo.vxlan.Vxlan mvo) {
                    return select(naef.mvo.vxlan.VtepIf.class, mvo.getCurrentMemberPorts());
                }
            }
        );

        installUiTypeName(naef.mvo.vxlan.VxlanIdPool.class, "vxlan.id-pool");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.vxlan.VxlanIdPool>(naef.mvo.vxlan.VxlanIdPool.home));
        installMvoDtoMapping(naef.mvo.vxlan.VxlanIdPool.class, naef.dto.vxlan.VxlanIdPoolDto.class);

        installUiTypeName(naef.mvo.vxlan.VtepIf.class, "vtep-if");
        installAttributes(naef.mvo.vxlan.VtepIf.class, naef.mvo.vxlan.VtepIf.class);
        installDtoAttrTranscript(
            new DtoAttrTranscript.SetRef<naef.dto.PortDto, naef.mvo.vxlan.VtepIf>(
                naef.dto.vxlan.VtepIfDto.class,
                naef.dto.vxlan.VtepIfDto.CONNECTED_PORTS,
                EvalStrategy.LAZY)
            {
                @Override protected Set<? extends naef.mvo.Port> getValues(naef.mvo.vxlan.VtepIf mvo) {
                    return asSet(mvo.getCurrentCrossConnectedPorts());
                }
            });

        installUiTypeName(naef.mvo.wdm.OpticalPath.class, "optical-path");

        installUiTypeName(naef.mvo.wdm.WdmLink.class, "wdm-link");

        installUiTypeName(naef.mvo.wdm.OpticalPathIdPool.class, "optical-path.id-pool");
        installMainResolver(
            new UniquelyNamedModelResolver<naef.mvo.wdm.OpticalPathIdPool>(naef.mvo.wdm.OpticalPathIdPool.home));

        installUiTypeName(naef.mvo.wdm.WdmPort.class, "wdm-port");

        for (UiTypeName typename : uiTypeNames().instances()) {
            if (naef.mvo.NodeElement.class.isAssignableFrom(typename.type())
                && typename.type() != naef.mvo.Node.class)
            {
                final Class<? extends naef.mvo.NodeElement> modelType
                    = typename.type().asSubclass(naef.mvo.NodeElement.class);
                final String name = typename.name();
                installMainResolver(
                    new Resolver.SingleNameResolver<naef.mvo.NodeElement, naef.mvo.NodeElement>(
                        modelType, name, naef.mvo.NodeElement.class)
                    {
                        @Override protected naef.mvo.NodeElement resolveImpl(naef.mvo.NodeElement context, String arg)
                            throws ResolveException
                        {
                            String subelementName = arg;

                            return context.getHereafterSubElement(modelType, subelementName);
                        }

                        @Override public String getName(naef.mvo.NodeElement obj) {
                            return obj.getFqn();
                        }
                    });
            }
            if (naef.mvo.Network.class.isAssignableFrom(typename.type())) {
                final Class<? extends naef.mvo.Network> modelType = typename.type().asSubclass(naef.mvo.Network.class);
                final String name = typename.name();
                installMainResolver(new Resolver.ClusteringResolver<naef.mvo.Network, naef.mvo.Port>(
                    modelType, name, null)
                {
                    @Override protected naef.mvo.Network resolveImpl(List<naef.mvo.Port> elements)
                        throws ResolveException
                    {
                        naef.mvo.Network network;
                        try {
                            network = NetworkUtils.resolveHereafterNetwork(modelType, elements);
                        } catch (ResolveException re) {
                            throw new ResolveException(re.getMessage());
                        }
                        if (network == null) {
                            throw new ResolveException("network が見つかりません.");
                        }
                        return network;
                    }

                    @Override public String getName(naef.mvo.Network obj) {
                        return null;
                    }
                });
            }
        }
    }

    private static <T extends Object & Comparable<T>> String getNetworkAbsoluteNameByIdPool(
        naef.mvo.AbstractNetwork network, IdPool<?, T, ?> pool, T id)
    {
        if (pool == null || id == null) {
            return "mvo-id;" + network.getMvoId().toString();
        }

        return SkeltonTefService.instance().uiTypeNames().getName(pool.getClass())
            + naef.dto.NaefDto.getFqnPrimaryDelimiter()
            + ((tef.skelton.NamedModel) pool).getName()
            + naef.dto.NaefDto.getFqnSecondaryDelimiter()
            + "id"
            + naef.dto.NaefDto.getFqnPrimaryDelimiter()
            + id.toString();
    }

    @Override protected void setupObjectQuerySingleValueAccesses() {
        super.setupObjectQuerySingleValueAccesses();

        ObjectQueryExpression.installSingleValueAccess(
            "node-element.owner",
            new ObjectQueryExpression.SingleValueAccess() {

                @Override public Object get(Object o) {
                    return naef.mvo.NodeElement.class.cast(o).getOwner();
                }
            });
        ObjectQueryExpression.installSingleValueAccess(
            "node-element.node",
            new ObjectQueryExpression.SingleValueAccess() {

                @Override public Object get(Object o) {
                    return naef.mvo.NodeElement.class.cast(o).getNode();
                }
            });
        ObjectQueryExpression.installSingleValueAccess(
            "node-element.fqn",
            new ObjectQueryExpression.SingleValueAccess() {

                @Override public Object get(Object o) {
                    return naef.mvo.NodeElement.class.cast(o).getFqn();
                }
            });
    }

    @Override protected void setupObjectQueryCollectionValueAccesses() {
        super.setupObjectQueryCollectionValueAccesses();

        ObjectQueryExpression.installCollectionValueAccess(
            "node-element.sub-elements",
            new ObjectQueryExpression.CollectionValueAccess() {

                @Override public Collection<?> get(Object o) {
                    return naef.mvo.NodeElement.class.cast(o).getCurrentSubElements();
                }
            });
    }

    protected boolean authenticateHttpRequest(HttpRequest request) {
        return true;
    }

    public String authenticate(String clientHost) throws AuthenticationException {
        return null;
    }

    public String beginWriteTransaction(String clientHost) throws AuthenticationException {
        if (TransactionContext.isTransactionRunning()) {
            if (! (TransactionContext.getTransactionId() instanceof TransactionId.R)) {
                throw new RuntimeException();
            }

            TransactionContext.close();
        }

        String username = authenticate(clientHost);

        TransactionContext.beginWriteTransaction();

        return username;
    }

    public String setupReadTransaction(String clientHost) throws AuthenticationException {
        String username = authenticate(clientHost);

        TransactionContext.setupReadTransaction();

        return username;
    }

    public boolean isRmiClientAuthenticationEnabled() {
        return rmiClientAuthenticationEnabled_;
    }

    private static <T> Set<T> select(Class<T> klass, Set<?> selectees) {
        Set<T> result = new HashSet<T>();
        for (Object elem : selectees) {
            if (klass.isInstance(elem)) {
                result.add((T) elem);
            }
        }
        return result;
    }

    private static <T> Set<T> asSet(Collection<T> source) {
        return new LinkedHashSet<T>(source);
    }
}
