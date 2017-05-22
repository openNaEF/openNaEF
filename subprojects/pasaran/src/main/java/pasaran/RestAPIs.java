package pasaran;

import org.eclipse.jetty.servlet.ServletHolder;

public class RestAPIs {
    public static void installRestApi(ServletHolder servletHolder, Class<?>... clazzes) {
        final String PARAMETER_NAME = "jersey.config.server.provider.classnames";
        final String SEPARATOR = ";";

        String initParam = servletHolder.getInitParameter(PARAMETER_NAME);
        StringBuilder sb = new StringBuilder(initParam == null ? "" : initParam);
        if(sb.length() > 0 && sb.indexOf(SEPARATOR) != sb.length()) {
            sb.append(SEPARATOR);
        }

        for(Class<?> clazz : clazzes) {
            sb.append(clazz.getCanonicalName()).append(SEPARATOR);
        }
        servletHolder.setInitParameter(PARAMETER_NAME, sb.toString());
    }
}
