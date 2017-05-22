package voss.discovery.agent.extreme;

public class ExtremeMibUtil {

    public static String getType(int machineID) {
        switch (machineID) {
            case 1:
                return "Summit 1";
            case 2:
                return "Summit 2";
            case 3:
                return "Summit 3";
            case 4:
                return "Summit 4";
            case 5:
                return "Summit 4fx";
            case 6:
                return "Summit 48";
            case 7:
                return "Summit 24";
            case 8:
                return "BlackDiamond 6800";
            case 11:
                return "BlackDiamond 6808";
            case 12:
                return "Summit 7iSX";
            case 13:
                return "Summit 7iTX";
            case 14:
                return "Summit 1iTX";
            case 15:
                return "Summit 5i";
            case 16:
                return "Summit 48i";
            case 17:
                return "Alpine 3808";
            case 19:
                return "Summit 1iSX";
            case 20:
                return "Alpine 3804";
            case 21:
                return "Summit 5iLX";
            case 22:
                return "Summit 5iTX";
            case 23:
                return "EnetSwitch24Port";
            case 24:
                return "BlackDiamond 6816";
            case 25:
                return "Summit 24e3";
            case 26:
                return "Alpine 3802";
            case 28:
                return "Summit 48si";
            case 30:
                return "Summit Px1";
            case 40:
                return "Summit 24e2TX";
            case 41:
                return "Summit 24e2SX";
            case 53:
                return "Summit 200-24";
            case 54:
                return "Summit 200-48";
            case 55:
                return "Summit 300-48";
            case 56:
                return "BlackDiamond 10808";
            case 58:
                return "Summit 400-48t";
            case 59:
                return "Summit 400-24x";
            case 61:
                return "summit 300-24";
            case 62:
                return "BlackDiamond 8810";
            case 63:
                return "Summit 400-24t";
            case 64:
                return "Summit 400-24p";
            case 65:
                return "Summit X450-24x";
            case 66:
                return "Summit X450-24t";
            case 67:
                return "Summit Stack";
            case 68:
                return "Summit WM100";
            case 69:
                return "Summit WM1000";
            case 70:
                return "Summit 200-24fx";
            case 71:
                return "Summit X450a-24t";
            case 72:
                return "Summit X450e-24p";
            case 74:
                return "BlackDiamond 8806";
            case 75:
                return "Altitude 350";
            case 76:
                return "Summit X450a-48t";
            case 77:
                return "BlackDiamond 12804";
            case 79:
                return "Summit X450e-48p";
            case 80:
                return "Summit X450a-24tDC";
            case 81:
                return "Summit X450a-24t";
            case 82:
                return "Summit X450a-24xDC";
            case 83:
                return "Sentriant CE150";
            case 84:
                return "Summit X450a-24x";
            case 85:
                return "BlackDiamond 12802";
            case 86:
                return "Altitude 300";
            case 87:
                return "Summit X450a-48tDC";
            case 88:
                return "Summit X250-24t";
            case 89:
                return "Summit X250-24p";
            case 90:
                return "Summit X250-24x";
            case 91:
                return "Summit X250-48t";
            case 92:
                return "Summit X250-48p";
            case 93:
                return "Summit Ver2Stack";
            case 94:
                return "Summit WM200";
            case 95:
                return "Summit WM2000";
            default:
                return "unknown type(" + machineID + ")";
        }
    }

    public static String getModuleType(int id) {
        switch (id) {
            case 1:
                return "none";
            case 2:
                return "fe32";
            case 3:
                return "g4x";
            case 4:
                return "g6x";
            case 5:
                return "fe32fx";
            case 6:
                return "msm";
            case 7:
                return "f48ti";
            case 8:
                return "g8xi";
            case 9:
                return "g8ti";
            case 10:
                return "g12sxi";
            case 11:
                return "g12ti";
            case 18:
                return "msm64i";
            case 19:
                return "alpine3808";
            case 20:
                return "alpine3804";
            case 21:
                return "fm32t";
            case 22:
                return "gm4x";
            case 23:
                return "gm4sx";
            case 24:
                return "gm4t";
            case 25:
                return "wdm8";
            case 26:
                return "fm24f";
            case 27:
                return "fm24sf";
            case 28:
                return "fm24te";
            case 29:
                return "f96ti";
            case 30:
                return "wdm4";
            case 31:
                return "f32fi";
            case 32:
                return "tenGx3";
            case 33:
                return "tenGigLR";
            case 34:
                return "g16x3";
            case 35:
                return "g24t3";
            case 36:
                return "gm16x3";
            case 37:
                return "gm16t3";
            case 38:
                return "fm16t3";
            case 39:
                return "fm32p";
            case 50:
                return "fm8v";
            case 51:
                return "wm4t1";
            case 52:
                return "wm4t3";
            case 53:
                return "wm1t3";
            case 54:
                return "wm4e1";
            case 55:
                return "alpine3802";
            case 101:
                return "p3c";
            case 102:
                return "p12c";
            case 103:
                return "arm";
            case 104:
                return "mpls";
            case 105:
                return "sma";
            case 106:
                return "p48c";
            case 107:
                return "a3c";
            case 108:
                return "a12c";
            case 200:
                return "pxm";
            case 201:
                return "s300fixed";
            case 202:
                return "msm3";
            case 203:
                return "msm1";
            case 204:
                return "msm1xl";
            case 301:
                return "s300expansion";
            case 400:
                return "g60t";
            case 401:
                return "g60x";
            case 402:
                return "teng6x";
            case 414:
                return "msmG8x";
            case 416:
                return "g48T";
            case 417:
                return "g48P";
            case 419:
                return "tenG4X";
            case 420:
                return "tenG2X";
            case 421:
                return "g20X";
            case 422:
                return "tenG2XH";
            case 433:
                return "g48te";
            case 434:
                return "g48ta";
            case 435:
                return "g48pe";
            case 437:
                return "g48x";
            default:
                return "unknown(" + id + ")";
        }
    }

    public static String getPortTypeName(int type) {
        switch (type) {
            case 1:
                return "1000BAES-SX";
            case 2:
                return "1000BASE-LX";
            case 3:
                return "1000BASE-CX";
            case 4:
                return "1000BASE-SXFD";
            case 5:
                return "1000BASE-LXFD";
            case 6:
                return "1000BASE-CXFD";
            case 7:
                return "1000BASE-WDMHD";
            case 8:
                return "1000BASE-WDMFD";
            case 9:
                return "1000BASE-LX70HD";
            case 10:
                return "1000BASE-LX70FD";
            case 11:
                return "1000BASE-ZXHD";
            case 12:
                return "1000BASE-ZXFD";
            case 13:
                return "1000BASE-LX100HD";
            case 14:
                return "1000BASE-LX100FD";
            case 15:
                return "10GBASE-CX4";
            case 16:
                return "10GBASE-ZR";
            default:
                return "Unknown(ID:" + type + ")";
        }
    }

}