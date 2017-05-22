package opennaef.notifier;

import org.junit.Test;
import tef.DateTime;
import tef.TransactionId;

import java.net.URL;

import static org.junit.Assert.assertEquals;

public class NotifierTest {
    public static final TransactionId.W tx = (TransactionId.W) TransactionId.getInstance("wffff");
    public static final DateTime time = new DateTime(12345);


    @Test
    public void DtoChangesURLを生成() throws Exception {
        URL url = Notifier.getDtoChangesUri(tx, time);

        assertEquals(
                "http://localhost:2510/api/v1/dto-changes?version=wffff&time=12345",
                url.toString());

    }
}