package voss.multilayernms.inventory.util;

import naef.dto.PortDto;
import voss.multilayernms.inventory.renderer.PortRenderer;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OspfAreaIdValidator {

    private static final Pattern pattern = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");

    public static void validate(PortDto current, String changed) throws ParseException {
        if (changed == null) {
            validate(current);
        } else {
            validate(changed);
        }
    }

    public static void validate(PortDto port) throws ParseException {
        String id = PortRenderer.getOspfAreaID(port);
        validate(id);
    }

    public static void validate(String id) throws ParseException {
        if (id == null) {
            return;
        }
        try {
            Integer.parseInt(id);
            return;
        } catch (NumberFormatException e) {
        }
        Matcher matcher = pattern.matcher(id);
        if (matcher.matches()) {
            return;
        }
        throw new ParseException("illegal OSPF ID:" + id, 0);
    }
}