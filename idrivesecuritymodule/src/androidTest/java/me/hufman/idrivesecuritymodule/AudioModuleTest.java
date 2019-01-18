package me.hufman.idrivesecuritymodule;

import android.support.test.runner.AndroidJUnit4;

import com.bmwgroup.connected.core.audio.AudioModule;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AudioModuleTest {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Test
    public void testVector() throws Exception {
        AudioModule.a("Test");
        int handle = AudioModule.c("me.hufman", "test");
        byte[] challenge = new byte[16];
        byte[] response = AudioModule.g(handle, challenge);
        String acceptableResponse = "0398B6B847F4E9F25579EF0DFE6606EB8AEFC41D646FF1981BA1C6E9C0939FDB1B0DC0AD455E4F27D71C4A95EF9D8E12B9B8375F9465188C0456A9217932201FCCE81CC532F131F366EA1AD069B472970F30FE10FF7E74D5525CCB7AAACE7486F5E3A9F54F549A2B7B5E69D267442A396C949A1CB676B7DCC7C4C7BB62D399226F5D04225D14248B5E61D6E328EA93C612DB70A77A29A222EF349D887B7BE3CDB5AEAF4C0F8159B53BA212A22CB6C689D1F1843499EA274525750F0C8D94856D4D731BEF23886C1A41CD70CCA78BD9A580736BE595120530A5F2BA63D83923A4F08665CEF67BB14293277E055E3587BD86507C708421ACE8313DA736ED6AA551A76732C06249A066ED4CC531EAA85FB6310ED901981E947A0F67455A740F69E05F938542B5FFE7DEB4BD919472FB029C9151F79B5A8A143FD2D9C5C7DC41A66A022CABA7F93891C4CA8E778FE3220A6994BED3A13F47DF1855CBC542FC7EB59955B0D07AEB5CAA98507790F90D65D4FCAF39A951B46D4C01D6F3462AB5CA368403E78EACBD5CCC113AB850C82876F2D771D7A310D0BFF1A8407C83500D6B080AE30AF92E958C7AEC05F8AD957596479ECC6BCDBB298ED071F3B9F7D987C0D9FB37987084FB93511C892050D0E28A2295CEAE54580FCCC618A723C89568F5443B94DADF455CCAA9BE85B43886131DB731AF52F30AB97FDEF31646DC96BEABD46B";
        //Log.i("Test", bytesToHex(response));
        assertEquals(acceptableResponse, bytesToHex(response));
    }
}
