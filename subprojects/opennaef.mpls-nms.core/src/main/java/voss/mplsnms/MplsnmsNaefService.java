package voss.mplsnms;

public class MplsnmsNaefService {

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        new MplsnmsTefService().start();
    }

    public static MplsnmsRmiServiceAccessPoint getRmiServiceAccessPoint() {
        return MplsnmsRmiServiceAccessPoint.Impl.instance;
    }
}
