package voss.multilayernms.inventory.nmscore.view.datamodifier;

import jp.iiga.nmt.core.model.IModel;

import java.util.Collection;

public abstract class Modifier {

    private final Collection<? extends IModel> targets;
    private final String userName;

    public Modifier(Collection<? extends IModel> targets, String userName) {
        this.targets = targets;
        this.userName = userName;
    }

    public Collection<? extends IModel> getTargets() {
        return targets;
    }

    public String getUserName() {
        return (userName == null ? "null" : userName);
    }

}