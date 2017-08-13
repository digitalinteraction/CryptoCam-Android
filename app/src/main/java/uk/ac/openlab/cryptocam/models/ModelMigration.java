package uk.ac.openlab.cryptocam.models;

import android.util.Log;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by Kyle Montague on 13/08/2017.
 */

public class ModelMigration implements RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        RealmSchema schema = realm.getSchema();

        Log.d("Model",schema.get("Video").getFieldNames().toString());

    }
}
