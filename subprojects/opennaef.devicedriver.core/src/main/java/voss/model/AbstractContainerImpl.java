package voss.model;

import java.lang.reflect.Array;
import java.util.*;

public abstract class AbstractContainerImpl extends AbstractVlanModel implements Container {
    private static final long serialVersionUID = 1L;

    protected abstract class PortIndex<K, V extends Port> extends Index<K, V> {

        public PortIndex(String indexName) {
            super(indexName);
            AbstractContainerImpl.this.addPortIndex(PortIndex.this);
        }

        abstract protected boolean isTargetPort(Port port);

        protected String getValueString(V value) {
            return value.getFullyQualifiedName();
        }
    }

    private transient List<PortIndex<Object, Port>> portIndexes_;

    private synchronized List<PortIndex<Object, Port>> getPortIndexes() {
        if (portIndexes_ == null) {
            portIndexes_ = new ArrayList<PortIndex<Object, Port>>();
        }
        return portIndexes_;
    }

    private transient Index<Integer, Port> portIfindexes_;

    private synchronized Index<Integer, Port> getPortIfindexes() {
        if (portIfindexes_ == null) {
            portIfindexes_ = new PortIndex<Integer, Port>("device:port-ifindex") {

                protected boolean isTargetPort(Port port) {
                    return true;
                }

                protected boolean isInitializable(Port port) {
                    try {
                        port.getIfIndex();
                        return true;
                    } catch (NotInitializedException nie) {
                        return false;
                    }
                }

                protected boolean isUniqueKey(Integer key) {
                    return key != null;
                }

                protected boolean isMultipleKeyEntity() {
                    return true;
                }

                protected Integer getKey(Port port) {
                    return new Integer(port.getIfIndex());
                }

                protected List<Integer> getKeys(Port port) {
                    List<Integer> keys = new ArrayList<Integer>();
                    keys.add(port.getIfIndex());
                    if (port.getAlternativeIfIndex() != null) {
                        keys.add(port.getAlternativeIfIndex());
                    }
                    return keys;
                }

                protected Set<Port> getInitialValues() {
                    return new HashSet<Port>(Arrays.asList(AbstractContainerImpl.this.ports_));
                }

                protected String getKeyString(Integer key) {
                    return key.toString();
                }
            };
        }
        return portIfindexes_;
    }

    private transient Index<String, Port> portIfnames_;

    private synchronized Index<String, Port> getPortIfnames() {
        if (portIfnames_ == null) {
            portIfnames_ = new PortIndex<String, Port>("device:port-ifname") {

                protected boolean isTargetPort(Port port) {
                    return true;
                }

                protected boolean isInitializable(Port port) {
                    try {
                        return port.getIfName() != null;
                    } catch (NotInitializedException e) {
                        return false;
                    }
                }

                protected boolean isUniqueKey(String key) {
                    return key != null;
                }

                @Override
                protected boolean isMultipleKeyEntity() {
                    return false;
                }

                @Override
                protected List<String> getKeys(Port port) {
                    throw new IllegalStateException();
                }

                protected String getKey(Port port) {
                    return port.getIfName();
                }

                protected Set<Port> getInitialValues() {
                    return new HashSet<Port>(Arrays.asList(AbstractContainerImpl.this.ports_));
                }

                protected String getKeyString(String key) {
                    return key;
                }
            };
        }
        return portIfnames_;
    }

    private transient Index<Class<?>, Port> classifiedPorts_;

    private synchronized Index<Class<?>, Port> getClassifiedPorts() {
        if (classifiedPorts_ == null) {
            classifiedPorts_ = new PortIndex<Class<?>, Port>("device:ports") {

                protected boolean isTargetPort(Port port) {
                    return true;
                }

                protected boolean isInitializable(Port o) {
                    return true;
                }

                protected boolean isUniqueKey(Class<?> key) {
                    if (key == null) {
                        throw new NullArgumentIsNotAllowedException();
                    }

                    return false;
                }

                @Override
                protected boolean isMultipleKeyEntity() {
                    return false;
                }

                @Override
                protected List<Class<?>> getKeys(Port port) {
                    throw new IllegalStateException();
                }

                protected Class<?> getKey(Port port) {
                    return port.getClass();
                }

                protected Set<Port> getInitialValues() {
                    return new HashSet<Port>(Arrays.asList(AbstractContainerImpl.this.ports_));
                }

                protected String getKeyString(Class<?> key) {
                    return key.getName();
                }
            };
        }
        return classifiedPorts_;
    }

    private String modelTypeName_;
    private Port[] ports_ = new Port[0];
    private Slot[] slots_ = new Slot[0];

    public AbstractContainerImpl() {
    }

    @Override
    public abstract Device getDevice();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void addPortIndex(PortIndex portIndex) {
        getPortIndexes().add(portIndex);
    }

    @Override
    public synchronized String getModelTypeName() {
        return modelTypeName_;
    }

    @Override
    public synchronized void setModelTypeName(String modelTypeName) {
        modelTypeName_ = modelTypeName;
    }

    @Override
    public synchronized Port[] getPorts() {
        return getRawPorts();
    }

    @Override
    public synchronized Port[] getRawPorts() {
        Port[] result = new Port[ports_.length];
        System.arraycopy(ports_, 0, result, 0, ports_.length);
        return result;
    }

    @Override
    public PhysicalPort[] getPhysicalPorts() {
        return selectPorts(PhysicalPort.class);
    }


    public LogicalPort[] getLogicalPorts() {
        return selectPorts(LogicalPort.class);
    }

    public Port getPortByIfIndex(int ifindex) {
        return getPortIfindexes().getUniqueValue(new Integer(ifindex));
    }

    public Port getPortByIfName(String ifname) {
        return getPortIfnames().getUniqueValue(ifname);
    }

    public synchronized void addPort(Port port) {
        if (VlanModelUtils.containsCompareAsSame(ports_, port)) {
            return;
        }

        for (PortIndex<Object, Port> portIndex : getPortIndexes()) {
            if (portIndex.isTargetPort(port)) {
                portIndex.addValue(port);
            }
        }

        ports_ = (Port[]) VlanModelUtils.arrayaddNoDuplicate(ports_, port);

        if (port.getDevice() == null) {
            port.initDevice(getDevice());
        }
    }

    @Override
    public synchronized void updatePort(Port port) {
        for (PortIndex<Object, Port> portIndex : getPortIndexes()) {
            if (portIndex.isTargetPort(port)) {
                portIndex.removeValue(port);
                portIndex.addValue(port);
            }
        }
    }

    public synchronized void removePort(Port port) {
        List<Port> ports = new ArrayList<Port>(Arrays.asList(ports_));
        if (!ports.contains(port)) {
            return;
        }

        ports.remove(port);

        ports_ = ports.toArray(new Port[0]);

        for (PortIndex<Object, Port> portIndex : getPortIndexes()) {
            if (portIndex.isTargetPort(port)) {
                portIndex.removeValue(port);
            }
        }
    }

    public synchronized Slot[] getSlots() {
        return slots_;
    }

    public synchronized Slot getSlotBySlotId(String slotId) {
        if (slots_ == null) {
            return null;
        }

        for (int i = 0; i < slots_.length; i++) {
            if (slots_[i].getSlotId().equals(slotId)) {
                return slots_[i];
            }
        }

        return null;
    }

    public synchronized Slot getSlotBySlotIndex(int slotIndex) {
        if (slots_ == null) {
            return null;
        }

        for (int i = 0; i < slots_.length; i++) {
            if (slots_[i].getSlotIndex() == slotIndex) {
                return slots_[i];
            }
        }

        return null;
    }

    public synchronized void addSlot(Slot slot) {
        if (slot == null) {
            throw new NullArgumentIsNotAllowedException();
        }
        if (VlanModelUtils.containsCompareAsSame(slots_, slot)) {
            return;
        }
        slots_ = (Slot[]) VlanModelUtils.arrayaddNoDuplicate(slots_, slot);
        slot.initContainer(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized <T extends Port> T[] selectPorts(Class<T> type) {
        List<Port> result = new ArrayList<Port>();
        for (Class<?> key : getClassifiedPorts().getNonUniquesKeys()) {
            if (type.isAssignableFrom(key)) {
                result.addAll(getClassifiedPorts().getNonUniqueValues(key));
            }
        }

        sortPorts(type, result);
        return result.toArray((T[]) Array.newInstance(type, result.size()));
    }

    protected synchronized <T extends Port> void sortPorts(Class<T> type, List<Port> ports) {
        if (PhysicalPort.class.isAssignableFrom(type)) {
            VlanModelUtils.sortPhysicalPort(ports);
        } else {
            VlanModelUtils.sortByIfname(ports);
        }
    }

    public abstract String getContainerName();
}