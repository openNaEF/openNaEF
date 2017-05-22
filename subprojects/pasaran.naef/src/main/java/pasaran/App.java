package pasaran;


import pasaran.naef.PasaranNaefService;

import java.util.Objects;


public class App {
    public static void main(String[] args) throws Exception {
        // Naef 設定
        System.setProperty("naef-rmi-port", "38100");
        System.setProperty("voss.mplsnms.rmi-service-name", "mplsnms");
        System.setProperty("running_mode", "console");
        System.setProperty("tef-working-directory",
                Objects.toString(System.getProperty(
                        "tef-working-directory"),
                        "./naef"));

        // Naef 起動
        new PasaranNaefService().start();
    }

    public static PasaranNaefService startNaef() throws Exception {
        PasaranNaefService service = new PasaranNaefService();
        return service;
    }
}
