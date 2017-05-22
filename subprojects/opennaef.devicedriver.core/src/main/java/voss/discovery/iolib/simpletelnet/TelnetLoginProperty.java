package voss.discovery.iolib.simpletelnet;


public class TelnetLoginProperty {

    private final String loginUsername;
    private final String loginPassword;
    private final String enableUsername;
    private final String enablePassword;

    public TelnetLoginProperty(String aLoginPassword, String anEnablePassword) {
        loginUsername = null;
        loginPassword = aLoginPassword;
        enableUsername = null;
        enablePassword = anEnablePassword;
    }

    public TelnetLoginProperty(String aLoginUsername, String aLoginPassword, String anEnableUsername, String anEnablePassword) {
        loginUsername = aLoginUsername;
        loginPassword = aLoginPassword;
        enableUsername = anEnableUsername;
        enablePassword = anEnablePassword;
    }

    public String getLoginUsername() {
        return loginUsername;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    public String getEnableUsername() {
        return enableUsername;
    }

    public String getEnablePassword() {
        return enablePassword;
    }
}