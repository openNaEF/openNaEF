package opennaef.rest.api.resource;

import opennaef.rest.api.AutoCloseableTx;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.InternalServerError;
import opennaef.rest.api.response.Responses;
import opennaef.rest.api.spawner.DtoChangesSpawner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tef.TransactionContext;
import tef.TransactionId;
import tef.skelton.dto.DtoChanges;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * DtoChanges 取得 API
 */
@Path("dto-changes")
public class DtoChangesApi {
    private static final Logger log = LoggerFactory.getLogger(DtoChangesApi.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public static Response get(
            @QueryParam("time") Date time,
            @QueryParam("version") TransactionId.W version
    ) throws ApiException {
        log.debug("[handle] dto-changes");
        try {
            TransactionId.W targetTx = version != null ? version : TransactionContext.getLastCommittedTransactionId();
            Long targetTime = time != null ? time.getTime() : null;
            DtoChanges dtoChanges = DtoChangesSpawner.getDtoChanges(targetTx);
            try (AutoCloseableTx tx = AutoCloseableTx.beginTx(targetTime, targetTx)) {
                return Response.ok(Responses.json.format(DtoChangesSpawner.toMap(dtoChanges)), MediaType.APPLICATION_JSON_TYPE).build();
            }
        } catch (RemoteException e) {
            throw new InternalServerError("NAEF-00500", "NAEF通信エラー. RMI接続失敗.");
        }
    }
}
