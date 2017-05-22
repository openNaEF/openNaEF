package naef.mvo;

import tef.skelton.ConstraintException;
import tef.skelton.NameConfigurableModel;

import java.util.Set;

public interface NodeElement extends NameConfigurableModel {

    public Node getNode();
    public void setOwner(NodeElement owner) throws ConstraintException;
    public NodeElement getOwner();
    public String getFqn();

    public void addSubElement(NodeElement subelement) throws ConstraintException;
    public void removeSubElement(NodeElement subelement, OperationType opType)
        throws ConstraintException;
    public Set<NodeElement> getCurrentSubElements();
    public Set<NodeElement> getHereafterSubElements();
    public <T extends NodeElement> T getHereafterSubElement(Class<T> klass, String name);
}
