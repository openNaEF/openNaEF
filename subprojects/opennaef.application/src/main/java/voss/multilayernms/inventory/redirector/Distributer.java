package voss.multilayernms.inventory.redirector;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class Distributer {

    private static class Match {

        private final String key, value, url;

        private Match(String key, String value, String url) {
            this.key = key;
            this.value = value;
            this.url = url;
        }

        public String getKey() {
            return key;
        }

        public String getURL() {
            return url;
        }

        public boolean isKeyExistType() {
            return value == null;
        }

        public String getValue() {
            return value;
        }

    }

    private List<Match> matchList = new ArrayList<Match>();
    private String defaultUrl;

    public Distributer(String file) throws IOException {
        initialize(file);
    }

    private void initialize(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (defaultUrl == null) {
                defaultUrl = new URL(line).toString();
            } else {
                String[] part = line.split("\\s+");
                if (part.length == 2) {
                    matchList.add(new Match(part[0], null, part[1]));
                } else {
                    matchList.add(new Match(part[0], part[1], part[2]));
                }
            }
        }
    }

    private String internalGetRedirectUrl(HttpServletRequest req) {
        for (Match match : getMatchs()) {
            String value = req.getParameter(match.getKey());
            if (match.isKeyExistType()) {
                if (value != null) {
                    return match.getURL();
                }
            } else if (match.getValue().equals(value)) {
                return match.getURL();
            }
        }
        return defaultUrl;
    }

    public String getRedirectUrl(HttpServletRequest req) {
        String url = internalGetRedirectUrl(req);
        if (req.getQueryString() != null) {
            url += "?" + req.getQueryString();
        }
        return url;
    }

    private List<Match> getMatchs() {
        return matchList;
    }

}