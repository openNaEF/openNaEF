package opennaef.rest.api.resource;

import opennaef.rest.api.AutoCloseableTx;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.NotFound;
import opennaef.rest.api.response.Responses;
import opennaef.rest.api.spawner.DtoSpawner;
import naef.dto.NaefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pasaran.naef.PasaranNaefService;
import pasaran.naef.dto.CustomerInfo2dDto;
import tef.MVO;
import tef.skelton.dto.EntityDto;
import tef.skelton.dto.MvoDtoDesc;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CustomerInfo と紐づいている MVO もしくは、
 * MVO と紐づいている CustomerInfo の関連の情報を扱うAPI
 */
@Path("/customer-info-references")
public class CustomerInfoReferencesAPI {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableMvoApi.class);

    /**
     * 指定された id の MVO を返す
     *
     * @param uri
     * @param id  MVO ID
     * @return json
     * @throws ApiException
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response get(
            @Context UriInfo uri,
            @PathParam("id") String id
    ) throws ApiException {
        log.debug("[handle] GET --- " + uri.getPath());

        try (AutoCloseableTx tx = AutoCloseableTx.beginTx((Long) null, null)) {
            final MVO mvo = PasaranNaefService.instance().getMvoRegistry().get(DtoSpawner.getMvoId(id));
            if (mvo == null) {
                throw new NotFound();
            }

            final EntityDto target = DtoSpawner.spawn(DtoSpawner.getMvoId(id));
            final Map<String, Object> res = (Map<String, Object>) (Map<String, ?>) DtoSpawner.createMvoLink(target, null, null);

            final Map<EntityDto.Desc<NaefDto>, List<TimeOP>> referenceChanges = new LinkedHashMap<>();

            // Reference 2d changes を取得する
            final Map<Long, List<EntityDto.Desc<NaefDto>>> changes;
            if (target instanceof CustomerInfo2dDto) {
                CustomerInfo2dDto customerInfo = (CustomerInfo2dDto) target;
                changes = new TreeMap<>(customerInfo.getReferences2dChanges());
            } else {
                changes = new TreeMap<>((Map<? extends Long, ? extends List<EntityDto.Desc<NaefDto>>>) target.getValue("CUSTOMER_INFOS_2D_CHANGES"));
            }

            // add, remove のリストを作る
            List<EntityDto.Desc<NaefDto>> prev = Collections.emptyList();
            for (Map.Entry<Long, List<EntityDto.Desc<NaefDto>>> entry : changes.entrySet()) {
                final Long time = entry.getKey();
                final List<EntityDto.Desc<NaefDto>> current = Collections.unmodifiableList(entry.getValue());

                // 1つ前のリストに含まれないオブジェクトは "add" されたもの
                for (EntityDto.Desc<NaefDto> desc : current) {
                    referenceChanges.putIfAbsent(desc, new ArrayList<>());
                    if (!prev.contains(desc)) {
                        List<TimeOP> ref = referenceChanges.get(desc);
                        ref.add(new TimeOP(time, OP.add));
                    }
                }

                // 1つ前のリストとの差集合をとり、そのオブジェクトが今のリストに含まれていない場合は "remove" されたもの
                final List<EntityDto.Desc<NaefDto>> expect = new ArrayList<>(prev);
                expect.removeAll(current);
                expect.stream()
                        .filter(desc -> !current.contains(desc))
                        .forEach(desc -> {
                            List<TimeOP> ref = referenceChanges.get(desc);
                            ref.add(new TimeOP(time, OP.remove));
                        });
                prev = current;
            }

            List<?> referenceChangesMap = referenceChanges.entrySet().stream()
                    .map(entry -> {
                        Map<String, Object> map = (Map<String, Object>) (Map<String, ?>) DtoSpawner.createMvoLink(((MvoDtoDesc) entry.getKey()), null, null);
                        map.put("reference_changes", entry.getValue());
                        return map;
                    })
                    .collect(Collectors.toList());
            res.put("reference_changes", referenceChangesMap);

            return Response.ok(Responses.json.format(res)).build();
        }
    }

    private static class TimeOP {
        private long time;
        private OP op;

        public TimeOP(long time, OP op) {
            this.time = time;
            this.op = op;
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public OP getOp() {
            return op;
        }

        public void setOp(OP op) {
            this.op = op;
        }
    }

    private enum OP {
        add, remove
    }
}
