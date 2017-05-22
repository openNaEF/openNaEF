package voss.multilayernms.inventory.nmscore.web.flow.state;

import jp.iiga.nmt.core.model.portedit.Vlan;
import jp.iiga.nmt.core.model.portedit.VlanEditModel;
import naef.dto.IdRange;
import naef.dto.vlan.VlanIdPoolDto;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.multilayernms.inventory.nmscore.portedit.VlanIdCreate;
import voss.multilayernms.inventory.nmscore.web.flow.FlowContext;
import voss.multilayernms.inventory.nmscore.web.flow.Operation;
import voss.nms.inventory.util.VlanUtil;

import javax.servlet.ServletException;
import java.io.IOException;

public class InventoryVlanIdCreateState extends UnificUIViewState {

    public InventoryVlanIdCreateState(StateId stateId) {
        super(stateId);
    }

    @Override
    public void execute(FlowContext context) throws ServletException, IOException, InventoryException, ExternalServiceException {
        try {
            VlanEditModel model = (VlanEditModel) Operation.getTargets(context);
            String user = context.getUser();
            if (model instanceof Vlan) {
                Integer vlanId = model.getVlanId();
                String vlanPool = model.getVlanPoolName();
                if (vlanId == null) {
                    throw new IllegalArgumentException("VLAN ID is null.");
                }
                if (vlanPool == null || vlanPool.isEmpty()) {
                    throw new IllegalArgumentException("VLAN Pool is null.");
                }
                if (!checkVlanIdRange(vlanPool, vlanId)) {
                    throw new IllegalArgumentException("VLAN ID : " + vlanId + " out of range");
                }

                if (vlanIdExists(vlanPool, vlanId)) {
                    throw new IllegalArgumentException("VLAN ID : " + vlanId + " is already exists.");
                }
                new VlanIdCreate(model, user).update();
            } else {
                throw new IllegalArgumentException("Target is wrong.");
            }
            super.execute(context);
        } catch (InventoryException e) {
            log.error("" + e);
            throw e;
        } catch (ExternalServiceException e) {
            log.error("" + e);
            throw e;
        } catch (IOException e) {
            log.error("" + e);
            throw e;
        } catch (RuntimeException e) {
            log.error("" + e);
            throw e;
        } catch (ServletException e) {
            log.error("", e);
            throw e;
        }
    }

    private boolean checkVlanIdRange(String vlanPool, Integer vlanId) throws ExternalServiceException, IOException {
        VlanIdPoolDto pool = VlanUtil.getPool(vlanPool);
        for (IdRange<Integer> range : pool.getIdRanges()) {
            if (range.lowerBound > vlanId || vlanId > range.upperBound) {
                return false;
            }
        }
        return true;
    }

    private boolean vlanIdExists(String vlanPool, Integer vlanId) throws ExternalServiceException, IOException {
        VlanIdPoolDto pool = VlanUtil.getPool(vlanPool);
        if (VlanUtil.getVlan(pool, vlanId) != null) {
            return true;
        }
        return false;
    }
}