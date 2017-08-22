package uk.ac.openlab.cryptocam.models;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by Kyle Montague on 13/08/2017.
 */

public class ModelMigration implements RealmMigration {

    public static final int CurrentVersion = 2;

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        RealmSchema schema = realm.getSchema();

        if(oldVersion < CurrentVersion){
            if(!schema.get("Cam").getFieldNames().contains("updatedOn")){
                schema.get("Cam").addField("updatedOn",long.class);
            }
        }

    }
}
