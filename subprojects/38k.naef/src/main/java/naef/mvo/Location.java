package naef.mvo;

import tef.MvoHome;
import tef.skelton.AbstractHierarchicalModel;
import tef.skelton.NameConfigurableModel;
import tef.skelton.SkeltonTefService;
import tef.skelton.SkeltonUtils;
import tef.skelton.UniquelyNamedModelHome;
import tef.skelton.ValueException;

public class Location 
    extends AbstractHierarchicalModel<Location>
    implements NameConfigurableModel
{
    public static final UniquelyNamedModelHome.Indexed<Location> home
        = new UniquelyNamedModelHome.Indexed<Location>(Location.class);

    private final F1<String> name_ = new F1<String>(home.nameIndex());

    protected Location(MvoId id) {
        super(id);
    }

    public Location() {
    }

    @Override public String getName() {
        return name_.get();
    }

    @Override public void setName(String name) throws ValueException {
        try {
            name_.set(name);
        } catch (MvoHome.UniqueIndexDuplicatedKeyFoundException uidkfe) {
            throw new ValueException("名前の重複が検出されました: " + name);
        }
    }

    public String getFqn() {
        return SkeltonTefService.instance().uiTypeNames().getName(getClass())
            + SkeltonTefService.instance().getFqnPrimaryDelimiter()
            + SkeltonUtils.fqnEscape(getName());
    }
}
