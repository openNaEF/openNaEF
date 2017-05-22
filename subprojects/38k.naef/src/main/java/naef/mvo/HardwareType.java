package naef.mvo;

import tef.skelton.UniquelyNamedModelHome;

public class HardwareType extends NodeElementType {

    public static class Attr {
    }

    public static final UniquelyNamedModelHome.SharedNamespace<HardwareType> home
         = new UniquelyNamedModelHome.SharedNamespace<HardwareType>(NaefObjectType.home, HardwareType.class);

    public HardwareType(MvoId id) {
        super(id);
    }

    public HardwareType(String name) {
        super(name);
    }
}
