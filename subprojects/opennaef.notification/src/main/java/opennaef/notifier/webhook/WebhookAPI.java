package opennaef.notifier.webhook;

import opennaef.notifier.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Webhooks
 */
@Path("/")
public class WebhookAPI {

    /**
     * List hooks
     *
     * @return hooks
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public static Response get() {
        return Responses.ok(Webhooks.instance().hooks());
    }

    /**
     * Get single hook
     *
     * @param id hook id
     * @return hook
     * @throws NotFound
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public static Response get(@PathParam("id") long id) throws NotFound {
        Hook hook = Webhooks.instance().hook(id);
        return Responses.ok(hook);
    }

    /**
     * Create a hook
     *
     * @param uriInfo uri info
     * @param reqBody request body
     * @return hook
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Response create(
            @Context UriInfo uriInfo,
            InputStream reqBody) throws NotFound, PingFailed {
        try {
            Hook raw = JsonUtils.json.parse(reqBody, Hook.class);
            Hook hook = Webhooks.instance().add(raw);
            return Responses.created(
                    uriInfo.getAbsolutePathBuilder().path("" + hook.getId()).build(),
                    hook);
        } catch (IOException e) {
            Logs.hook.error("create", e);
            return Responses.badRequest();
        }
    }

    /**
     * Edit a hook
     *
     * @param id      hook id
     * @param reqBody request body
     * @return hook
     * @throws NotFound
     */
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Response update(
            @PathParam("id") long id,
            InputStream reqBody) throws NotFound, PingFailed {
        try {
            Map attr = JsonUtils.json.parse(reqBody, Map.class);
            Hook next = Webhooks.instance().update(id, attr);
            return Responses.ok(next);
        } catch (IOException e) {
            Logs.hook.error("update", e);
            return Responses.badRequest();
        }
    }

    /**
     * Delete a hook
     *
     * @param id hook id
     * @return no content
     * @throws NotFound
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static Response delete(@PathParam("id") long id) throws NotFound {
        boolean removed = Webhooks.instance().remove(id);
        if (removed) {
            return Responses.noContent();
        } else {
            return Responses.badRequest();
        }
    }

    /**
     * Ping a hook
     *
     * @param id hook id
     * @return no content
     * @throws NotFound
     */
    @POST
    @Path("/{id}/pings")
    @Consumes(MediaType.APPLICATION_JSON)
    public static Response ping(@PathParam("id") long id) throws NotFound {
        Hook hook = Webhooks.instance().hook(id);
        Webhooks.instance().sendPing(hook);
        return Responses.noContent();
    }
}
