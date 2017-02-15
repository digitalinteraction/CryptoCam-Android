package uk.ac.openlab.cryptocam.utility;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kylemontague on 13/02/2017.
 */

public class CryptoCamPacket {
    public String key = "";
    public String iv = "";
    public String url = "";
    public String encryption = "";

    long reconnectIn = 30000;

    public static CryptoCamPacket fromJson(JSONObject object){
        CryptoCamPacket packet = new CryptoCamPacket();
        try {
            packet.key = object.getString("key");
            packet.iv = object.getString("iv");
            packet.url = object.getString("url");
            packet.reconnectIn = object.getLong("reconnectIn");
            packet.encryption = object.getString("encryption");

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return packet;
    }

    public CryptoCamPacket(){
        key = "";
        url = "";
        encryption = "";
        reconnectIn = 30000;
    }
}
