package tef.skelton;

public interface NameConfigurableModel extends NamedModel {

    public void setName(String newName) throws ConstraintException;
}
