package voss.multilayernms.inventory.nmscore.inventory.ipsubnet;

import jp.iiga.nmt.core.model.resistvlansubnet.IpSubnetNamespaceModel;
import jp.iiga.nmt.core.model.resistvlansubnet.RegularIpSubnetNamespaceModel;
import jp.iiga.nmt.core.model.resistvlansubnet.RootIpSubnetNamespaceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import voss.multilayernms.inventory.nmscore.inventory.ipsubnet.updater.*;

import java.io.IOException;

public class IpSubnetNamespaceUpdaterFactory {

    @SuppressWarnings("unused")
    private final static Logger log = LoggerFactory.getLogger(IpSubnetNamespaceUpdaterFactory.class);

    public static IpSubnetNamespaceUpdater getUpdater(IpSubnetNamespaceModel target, String userName) throws IOException {
        String targetClassName = target.getClass().getName();

        if (!target.isRemove()) {
            if (targetClassName.equals(RootIpSubnetNamespaceModel.class.getName())) {
                return new RootIpSubnetNamespaceUpdater(target, userName);
            } else if (targetClassName.equals(RegularIpSubnetNamespaceModel.class.getName())) {
                return new RegularIpSubnetNamespaceUpdater(target, userName);
            }
        } else {
            if (targetClassName.equals(RootIpSubnetNamespaceModel.class.getName())) {
                return new RootIpSubnetNamespaceRemover(target, userName);
            } else if (targetClassName.equals(RegularIpSubnetNamespaceModel.class.getName())) {
                return new RegularIpSubnetNamespaceRemover(target, userName);
            }
        }


        throw new IllegalArgumentException("objectType is unknown.");
    }

}