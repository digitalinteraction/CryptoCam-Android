package uk.ac.openlab.cryptocam.utility;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kylemontague on 13/02/2017.
 */

public class CryptoCamPacket {
    String key = "";
    String url = "";
    long reconnectIn = 30000;

    public static CryptoCamPacket fromJson(JSONObject object){
        CryptoCamPacket packet = new CryptoCamPacket();
        try {
            packet.key = object.getString("key");
            packet.url = object.getString("url");
            packet.reconnectIn = object.getLong("reconnectIn");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return packet;
    }

    public CryptoCamPacket(){
        key = "";
        url = "";
        reconnectIn = 30000;
    }
}
