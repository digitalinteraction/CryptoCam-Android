package uk.ac.openlab.cryptocam.services;

import java.net.URI;

/**
 * Created by Kyle Montague on 26/02/2017.
 */

public interface ICryptoCamReceiver {

    /**
     * Triggered after a video has been downloaded (and decrypted) into the local storage.
     *
     * @param uri local URI location for the video file
     * @param id  unique id for the video within the collection
     */
    void downloadedVideo(URI uri, long id);

    /**
     * Triggered after a thumbnail has been downloaded (and decrypted) into the local storage.
     *
     * @param uri local URI location for the thumbnail file
     * @param id  unique id for the related video within the collection
     */
    void downloadedThumbnail(URI uri, long id);


    /**
     * Triggered when a previously discovered / known camera appears nearby.
     *
     * @param id unique id of the camera in the collection
     */
    void cameraHasAppeared(long id);

    /**
     * Triggered when a previously discovered / known camera disappears or is no longer nearby.
     *
     * @param id unique id of the camera in the collection
     */
    void cameraHasDisappeared(long id);


    /**
     * Triggered when the device begins scanning for new nearby cameras
     */
    void scanningForCameras();

    /**
     * Triggered when the device stops scanning for nearby cameras.
     */
    void stoppedScanning();

    /**
     * This method will be triggered after the new camera device has been added to the collection.
     *
     * @param id unique id for the camera object.
     */
    void cameraAdded(long id);

    /**
     * Triggered when a new CryptoCam video has been added to the collection.
     *
     * @param id unique id for the video within the collection.
     */
    void videoAdded(long id);


    void newVideoKey(long id);
}
