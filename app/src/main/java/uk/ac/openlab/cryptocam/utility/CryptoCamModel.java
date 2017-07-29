package uk.ac.openlab.cryptocam.utility;

/**
 * Created by Kyle Montague on 27/07/2017.
 */

class CryptoCamModel {

    byte[] keys;
    byte[] name;
    byte[] version;
    byte[] model;
    byte[] location;

    public <T1, T2, T3, T4, T5> CryptoCamModel(byte[] t1, byte[] t2, byte[] t3, byte[] t4, byte[] t5) {
        keys = t1;
        name = t2;
        version = t3;
        model = t4;
        location = t5;
    }
}
