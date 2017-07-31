package uk.ac.openlab.cryptocam.utility;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Created by Kyle Montague on 31/07/2017.
 */

class CombinedObject {

    String location;
    String model;
    String version;
    String name;
    String key;


    public CombinedObject(byte[] location, byte[] model, byte[] version, byte[] name, byte[] key) {
        this.location = new String(location, StandardCharsets.UTF_8);
        this.model = new String(model, StandardCharsets.UTF_8);
        this.version = new String(version, StandardCharsets.UTF_8);
        this.name = new String(name, StandardCharsets.UTF_8);
        this.key = new String(key, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),"%s,%s,%s,%s,%s",location,model,version,name,key);
    }
}
