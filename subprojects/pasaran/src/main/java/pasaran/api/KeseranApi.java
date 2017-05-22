package pasaran.api;


import net.arnx.jsonic.JSON;
import pasaran.pojo.PasaranPOJO;
import pasaran.pojo.PasaranVlanPOJO;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.rmi.RemoteException;
import java.util.List;

@Path("/keseran")
public class KeseranApi {
    public static final JSON json;

    static {
        json = new JSON();
        json.setPrettyPrint(true);
        json.setSuppressNull(true);
    }

    @GET
    @Path("/node")
    @Produces(MediaType.APPLICATION_JSON)
    public String 時間を指定してノードを取得(
            @Context HttpHeaders header,
            @Context HttpServletResponse response,
            @QueryParam("time") String time,
            @QueryParam("version") String version
    ) throws RemoteException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<PasaranPOJO> pojos = KeseranApiUtil.getNodes(time, version);
        return json.format(pojos);
    }

    @GET
    @Path("/link")
    @Produces(MediaType.APPLICATION_JSON)
    public String 時間を指定してリンクを取得(
            @Context HttpHeaders header,
            @Context HttpServletResponse response,
            @QueryParam("time") String time,
            @QueryParam("version") String version
    ) throws RemoteException
    {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<PasaranPOJO> pojos = KeseranApiUtil.getLinks(time, version);
        return json.format(pojos);
    }

    @GET
    @Path("/dummy-link")
    @Produces(MediaType.APPLICATION_JSON)
    public String 時間を指定してダミーリンクを取得(
            @Context HttpHeaders header,
            @Context HttpServletResponse response,
            @QueryParam("time") String time,
            @QueryParam("version") String version
    ) throws RemoteException
    {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<PasaranPOJO> pojos = KeseranApiUtil.getDummyLinks(time, version);
        return json.format(pojos);
    }

    @GET
    @Path("/vlan")
    @Produces(MediaType.APPLICATION_JSON)
    public String 時間を指定してVlan2を取得(
            @Context HttpHeaders header,
            @Context HttpServletResponse response,
            @QueryParam("time") String time,
            @QueryParam("version") String version
    ) throws RemoteException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<PasaranVlanPOJO> pojos = VlanPOJOUtil.getMvoVlans(time, version);
        return json.format(pojos);
    }

    @GET
    @Path("/lsp")
    @Produces(MediaType.APPLICATION_JSON)
    public String 時間を指定してLSPを取得(
            @Context HttpHeaders header,
            @Context HttpServletResponse response,
            @QueryParam("time") String time,
            @QueryParam("version") String version
    ) throws RemoteException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<PasaranPOJO> pojos = LspPOJOUtil.getLsps(time, version);
        return json.format(pojos);
    }

    @GET
    @Path("/pseudo-wire")
    @Produces(MediaType.APPLICATION_JSON)
    public String 時間を指定してPWを取得(
            @Context HttpHeaders header,
            @Context HttpServletResponse response,
            @QueryParam("time") String time,
            @QueryParam("version") String version
    ) throws RemoteException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<PasaranPOJO> pojos = PseudoWirePOJOUtil.getPseudoWire(time, version);
        return json.format(pojos);
    }

    @GET
    @Path("/customers")
    @Produces(MediaType.APPLICATION_JSON)
    public String 時間を指定してCustomerInfoを取得(
            @Context HttpHeaders header,
            @Context HttpServletResponse response,
            @QueryParam("time") String time,
            @QueryParam("version") String version
    ) throws RemoteException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        List<PasaranPOJO> pojos = CustomerInfoPOJOUtil.getCustomerInfos(time);
        return json.format(pojos);
    }
}
