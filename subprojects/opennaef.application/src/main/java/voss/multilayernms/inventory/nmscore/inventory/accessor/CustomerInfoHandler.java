package voss.multilayernms.inventory.nmscore.inventory.accessor;

import naef.dto.CustomerInfoDto;
import naef.dto.NaefDto;
import naef.ui.NaefDtoFacade;
import naef.ui.NaefDtoFacade.SearchMethod;
import net.phalanx.core.expressions.ObjectFilterQuery;
import net.phalanx.core.models.CustomerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.skelton.AuthenticationException;
import tef.skelton.dto.EntityDto;
import voss.core.server.database.ATTR;
import voss.core.server.exception.ExternalServiceException;
import voss.core.server.exception.InventoryException;
import voss.core.server.util.DtoUtil;
import voss.multilayernms.inventory.database.MplsNmsInventoryConnector;
import voss.multilayernms.inventory.nmscore.model.creator.CustomerInfoModelCreator;
import voss.multilayernms.inventory.nmscore.rendering.CustomerInfoRenderingUtil;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


public class CustomerInfoHandler {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(CustomerInfoHandler.class);

    public static List<CustomerInfo> getList(ObjectFilterQuery query) throws NotBoundException, AuthenticationException, IOException, InventoryException, ExternalServiceException {
        return createModel(filter(query, getAllCustomerInfo()));
    }

    public static List<CustomerInfo> getList(String mvoid) throws NotBoundException, AuthenticationException, IOException, InventoryException, ExternalServiceException {
        EntityDto dto = MplsNmsInventoryConnector.getInstance().getMvoDtoByMvoId(mvoid);
        if (dto == null || !(dto instanceof NaefDto)) return new ArrayList<CustomerInfo>();

        return createModel(((NaefDto) dto).getCustomerInfos());
    }

    private static List<CustomerInfo> createModel(Collection<CustomerInfoDto> dtos) throws IOException {
        List<CustomerInfo> customers = new ArrayList<CustomerInfo>();
        if (dtos == null) return customers;

        for (CustomerInfoDto customer : dtos) {
            customers.add(CustomerInfoModelCreator.createModel(customer, DtoUtil.getMvoId(customer).toString()));
        }
        return customers;
    }

    private static Set<CustomerInfoDto> getAllCustomerInfo() throws RemoteException, IOException, ExternalServiceException {
        NaefDtoFacade facade = MplsNmsInventoryConnector.getInstance().getDtoFacade();
        return facade.selectCustomerInfos(SearchMethod.REGEXP, ATTR.CUSTOMER_INFO_ID, ".*");
    }

    private static List<CustomerInfoDto> filter(ObjectFilterQuery query, Collection<CustomerInfoDto> customers) {
        List<CustomerInfoDto> result = new ArrayList<CustomerInfoDto>();

        for (CustomerInfoDto dto : customers) {
            boolean unmatched = false;
            for (String field : query.keySet()) {
                String value = CustomerInfoRenderingUtil.rendering(dto, field);

                if (!query.get(field).matches(value)) {
                    unmatched = true;
                    break;
                }
            }
            if (!unmatched) {
                result.add(dto);
            }
        }
        return result;
    }
}