package opennaef.rest.api.response;

import opennaef.rest.api.NaefRestApi;
import opennaef.rest.api.config.CmdBuilderMapping;
import opennaef.rest.api.config.api.ApiConfig;
import net.arnx.jsonic.JSON;
import tef.DateTime;
import tef.TransactionId;
import tef.skelton.dto.EntityDto;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * opennaef.rest api の http request を処理する static な関数群
 */
public class Responses {
    public static JSON json;

    static {
        json = new JSON();
        json.setPrettyPrint(true);
        json.setSuppressNull(true);
    }

    /**
     * ApiException を Response へ変換する
     *
     * @param ex ApiException
     * @return Response
     */
    public static Response toResponse(ApiException ex) {
        ErrorResponse res = new ErrorResponse();
        res.setHttpResponseCode(ex.httpResponseCode());
        res.setCode(ex.errorCode());
        res.setMessage(ex.message());

        String stackTrace = ex.stackTraceString();
        if (stackTrace != null) {
            res.setDebugMessage(stackTrace);
        }

        return toResponse(res);
    }

    /**
     * 予期しないExceptionは 500 INTERNAL SERVER ERROR を返す
     *
     * @param t exception
     * @return 500 INTERNAL SERVER ERROR
     */
    public static Response toResponse(Throwable t) {
        ErrorResponse res = new ErrorResponse();
        res.setHttpResponseCode(500);
        res.setCode("INTERNAL SERVER ERROR");
        res.setMessage(t.getMessage());

        StringWriter errors = new StringWriter();
        t.printStackTrace(new PrintWriter(errors));
        res.setDebugMessage(errors.toString());

        return toResponse(res);
    }

    private static Response toResponse(ErrorResponse res) {
        return Response.status(res.getHttpResponseCode())
                .entity(json.format(res))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }


    /**
     * dtoをもとにURIを生成する
     *
     * @param dto
     * @return /api/v1/${context}/${mvo-id}?time=${target-time}&version=${target-version}
     */
    public static String getLocation(EntityDto dto, Long time, TransactionId.W tx) {
        if (dto == null) return null;

        List<ApiConfig> contents = CmdBuilderMapping.instance().contents(dto.getClass());
        if (contents.size() == 0) {
            throw new IllegalStateException(dto.getClass().getSimpleName() + "is not supported");
        }

        // api.yaml で最初に合致したcontentを使用する
        ApiConfig content = contents.get(0);

        StringBuilder sb = new StringBuilder();
        sb.append(NaefRestApi.APPLICATION_PATH).append("/")
                .append(content.context).append("/")
                .append(dto.getOid().toString());

        List<String> query = new ArrayList<>();
        if (time != null) {
            query.add("time=" + time);
        }
        if (tx != null) {
            query.add("version=" + tx.toString());
        }
        if (!query.isEmpty()) {
            sb.append("?").append(String.join("&", query));
        }
        return sb.toString();
    }

    /**
     * dtoをもとにURIを生成する
     *
     * @param dto
     * @return /api/v1/${context}/${mvo-id}?time=${target-time}&version=${target-version}
     */
    public static String getLocation(EntityDto dto, DateTime time, TransactionId.W tx) {
        if (time == null) {
            return getLocation(dto, (Long) null, tx);
        }
        return getLocation(dto, time.getValue(), tx);
    }

    /**
     * dtoのURIをフルパスで生成する
     *
     * @param event
     * @param dto
     * @return http://Host/api/v1/${context}/${mvo-id}
     */
    public static String getAbsoluteLocation(HttpServletRequest event, EntityDto dto, Long time, TransactionId.W tx) {
        return "http://" +
                event.getHeader("Host") +
                getLocation(dto, time, tx);
    }
}
