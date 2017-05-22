package voss.nms.inventory.model;

import org.apache.wicket.model.AbstractReadOnlyModel;

@SuppressWarnings("serial")
abstract public class RenewableAbstractReadOnlyModel<T> extends AbstractReadOnlyModel<T> {

    @Override
    abstract public T getObject();

    abstract public void renew();
}