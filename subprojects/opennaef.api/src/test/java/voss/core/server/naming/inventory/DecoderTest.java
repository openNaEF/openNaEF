package voss.core.server.naming.inventory;

import naef.dto.NaefDto;

public class DecoderTest {

    public static void main(String[] args) {
        try {
            String id = "NODE:OpenNaefTestNode";
            NaefDto result = InventoryIdDecoder.getDto(id);
            if (result != null) {
                System.out.println("id found: " + result.getAbsoluteName());
            } else {
                System.out.println("id not found: " + id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}