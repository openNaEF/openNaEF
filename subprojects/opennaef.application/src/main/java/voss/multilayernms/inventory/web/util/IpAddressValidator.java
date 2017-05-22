package voss.multilayernms.inventory.web.util;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Map;

public class IpAddressValidator extends AbstractValidator<String> {
    private static final long serialVersionUID = 1L;

    public IpAddressValidator() {
        super();
    }

    @Override
    protected void onValidate(IValidatable<String> arg0) {
        String range = arg0.getValue();
        if (range == null || range.length() == 0) {
            return;
        }
        if (range.indexOf(':') != -1) {
            String[] v6Array = range.split("[:.]");
            for (String s : v6Array) {
                if (s == null || s.length() == 0) {
                    continue;
                }
                try {
                    int i = Integer.parseInt(s, 16);
                    if (i < 0 || i > 255) {
                        error(arg0);
                        return;
                    }
                } catch (NumberFormatException e) {
                    try {
                        int i = Integer.parseInt(s, 10);
                        if (i < 0 || i > 255) {
                            error(arg0);
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        error(arg0);
                        return;
                    }
                }
            }
            try {
                Inet6Address.getByName(range);
            } catch (UnknownHostException e) {
                error(arg0);
                return;
            }
        } else if (range.indexOf('.') != -1) {
            String[] v4Array = range.split("\\.");
            if (v4Array.length != 4) {
                error(arg0);
                return;
            } else {
                for (String s : v4Array) {
                    try {
                        int i = Integer.parseInt(s);
                        if (i < 0 || i > 255) {
                            error(arg0);
                            return;
                        }
                    } catch (NumberFormatException e) {
                        error(arg0);
                        return;
                    }
                }
            }
            try {
                Inet4Address.getByName(range);
            } catch (UnknownHostException e) {
                error(arg0);
                return;
            }
        } else {
            error(arg0);
        }
    }

    @Override
    protected String resourceKey() {
        return "IpAddressValidator";
    }

    @Override
    protected Map<String, Object> variablesMap(IValidatable<String> validatable) {
        Map<String, Object> map = super.variablesMap(validatable);
        map.put("address", validatable.getValue());
        return map;
    }

}