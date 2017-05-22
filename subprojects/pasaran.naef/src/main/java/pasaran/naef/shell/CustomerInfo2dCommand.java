package pasaran.naef.shell;

import naef.mvo.CustomerInfo;
import naef.shell.NaefShellCommand;
import pasaran.naef.mvo.CustomerInfo2d;
import tef.skelton.Model;
import tef.skelton.ObjectResolver;
import tef.skelton.ResolveException;

/**
 * customer-info に対してオブジェクトを紐づける
 * customerinfo-2d [operation: add,remove] [object (fqn, mvo-id)] [customer-info (fqn, mvo-id)]
 * <p>
 * TefShellPluginsConfig.xml へ以下を追加する必要がある
 * <plug-in name="customerinfo-2d" class="pasaran.naef.shell.CustomerInfo2dCommand" />
 */
public class CustomerInfo2dCommand extends NaefShellCommand {
    private enum Operation {
        ADD, REMOVE;

        public static Operation resolve(String str) throws ShellCommandException {
            for (Operation instance : values()) {
                if (instance.name().toLowerCase().equals(str)) {
                    return instance;
                }
            }
            throw new ShellCommandException("no such operation, " + str);
        }
    }

    @Override
    public String getArgumentDescription() {
        return "[operation: add,remove] [object (fqn, mvo-id)] [customer-info (fqn, mvo-id)]";
    }

    @Override
    public void process(Commandline args) throws ShellCommandException {
        checkArgsSize(args, 3);

        this.beginWriteTransaction();
        Operation operation = Operation.resolve(args.arg(0));


        Model obj;
        CustomerInfo customerInfo;
        try {
            obj = ObjectResolver.resolve(Model.class, null, this.getSession(), args.arg(1));
            customerInfo = ObjectResolver.resolve(CustomerInfo.class, null, this.getSession(), args.arg(2));
        } catch (ResolveException var7) {
            throw new ShellCommandException(var7.getMessage());
        }

        CustomerInfo2d.NetworkModelAttr.Holder holder4obj
                = CustomerInfo2d.NetworkModelAttr.Mvo.CUSTOMER_INFOS_2D.get(obj);
        CustomerInfo2d.CustomerInfoAttr.Holder holder4customerInfo
                = CustomerInfo2d.CustomerInfoAttr.Mvo.REFERENCES_2D.get(customerInfo);

        switch (operation) {
            case ADD:
                if (holder4obj == null) {
                    holder4obj = new CustomerInfo2d.NetworkModelAttr.Holder();
                    CustomerInfo2d.NetworkModelAttr.Mvo.CUSTOMER_INFOS_2D.set(obj, holder4obj);
                }
                holder4obj.addCustomerInfo(customerInfo);

                if (holder4customerInfo == null) {
                    holder4customerInfo = new CustomerInfo2d.CustomerInfoAttr.Holder();
                    CustomerInfo2d.CustomerInfoAttr.Mvo.REFERENCES_2D.set(customerInfo, holder4customerInfo);
                }
                holder4customerInfo.addReference(obj);

                break;
            case REMOVE:
                if (holder4obj != null) {
                    holder4obj.removeCustomerInfo(customerInfo);
                }
                if (holder4customerInfo != null) {
                    holder4customerInfo.removeReference(obj);
                }
                break;
            default:
                throw new RuntimeException();
        }
        this.commitTransaction();
    }
}
