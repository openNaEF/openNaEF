package opennaef.rest.api.helper;

import opennaef.rest.api.CRUD;
import opennaef.rest.api.config.api.ApiConfig;
import opennaef.rest.api.response.ApiException;
import opennaef.rest.api.response.BadRequest;
import naef.dto.NaefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pasaran.naef.dto.CustomerInfo2dDto;
import tef.DateTime;
import voss.core.server.builder.CommandBuilder;
import voss.core.server.builder.CustomerInfoCommandBuilder;
import voss.nms.inventory.builder.CustomerInfo2dReferencesBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static opennaef.rest.api.helper.CmdBuilderProcessor.toValue;


/**
 * CustomerInfo ビルド
 */
public class CustomerInfoBuildHelper implements BuildHelper {
    private static final Logger log = LoggerFactory.getLogger(CustomerInfoBuildHelper.class);

    @Override
    public List<CommandBuilder> help(CRUD crud, ApiConfig conf, Map<String, Object> reqArgs) throws ApiException {
        CustomerInfoCommandBuilder b = new CustomerInfoCommandBuilder("editor");
        log.debug("help " + crud + " builderConfig conf is null. " + reqArgs.get("$id"));

        List<CommandBuilder> builders = new ArrayList<>();
        CustomerInfoCommandBuilder builder;
        try {
            CustomerInfo2dDto targetDto = (CustomerInfo2dDto) CmdBuilderProcessor.toValue(reqArgs.get("$id"), CustomerInfo2dDto.class.getTypeName());
            switch (crud) {
                case CREATE:
                    builder = new CustomerInfoCommandBuilder("editor");
                    break;
                case UPDATE:
                    builder = new CustomerInfoCommandBuilder(targetDto, "editor");
                    break;
                case DELETE:
                    builder = new CustomerInfoCommandBuilder(targetDto, "editor");
                    break;
                default:
                    // CmdBuilderProcessor でcrudのチェックをしているため、ここは実行されない
                    throw new IllegalStateException();
            }

            builders.add(builder);
            String name = (String) CmdBuilderProcessor.toValue(reqArgs.get("name"), String.class.getName());
            if (crud == CRUD.CREATE && (name == null || name.isEmpty())) {
                throw new BadRequest("API-02400", "入力値が不正です. name は必須属性です.");
            }
            if (targetDto == null || crud == CRUD.CREATE) {
                builder.setID(name);
            }

            // REFERENCE_2D
            List<String> addIds = (List<String>) CmdBuilderProcessor.toValue(reqArgs.get("add_references"), "java.util.ArrayList<java.lang.String>");
            List<String> removeIds = (List<String>) CmdBuilderProcessor.toValue(reqArgs.get("remove_references"), "java.util.ArrayList<java.lang.String>");
            if (addIds == null) {
                addIds = Collections.emptyList();
            }
            if (removeIds == null) {
                removeIds = Collections.emptyList();
            }

            if (!addIds.isEmpty() || !removeIds.isEmpty()) {
                String timeString = (String) reqArgs.get("&time");
                DateTime time;
                if (timeString == null) {
                    time = new DateTime(System.currentTimeMillis());
                } else {
                    time = new DateTime(Long.parseLong(timeString));
                }

                CustomerInfo2dReferencesBuilder refBuilder;
                if (targetDto == null) {
                    refBuilder = new CustomerInfo2dReferencesBuilder(targetDto, "editor");
                } else {
                    refBuilder = new CustomerInfo2dReferencesBuilder(targetDto.getAbsoluteName(), "editor");
                }
                addIds.forEach(id -> {
                    try {
                        NaefDto ref = CmdBuilderProcessor.toValue(id, NaefDto.class);
                        refBuilder.addReference(time, ref);
                    } catch (ClassNotFoundException | ApiException e) {
                        e.printStackTrace();
                    }
                });
                removeIds.forEach(id -> {
                    try {
                        NaefDto ref = CmdBuilderProcessor.toValue(id, NaefDto.class);
                        refBuilder.removeReference(time, ref);
                    } catch (ClassNotFoundException | ApiException e) {
                        e.printStackTrace();
                    }
                });
                // FIXME ワークアラウンド
                builder.recordChange("REFERENCE_2D", "prev", "next");
                builders.add(refBuilder);
            }
        } catch (ClassNotFoundException e) {
            log.error("customer-info build helper", e);
            throw new BadRequest("API-02400", "入力値が不正です. " + e.getMessage(), e);
        }

        return builders;
    }
}
