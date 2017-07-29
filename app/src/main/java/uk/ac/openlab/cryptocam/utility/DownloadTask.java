package uk.ac.openlab.cryptocam.utility;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by kylemontague on 15/02/2017.
 */

public class DownloadTask extends AsyncTask<DownloadRequest, Integer, String> {

    public static final String TAG = "DownloadTask";
    private Context context;
    private DownloadTaskProgress progressListener = null;
    private PowerManager.WakeLock mWakeLock;

    public DownloadTask(Context context) {
        this.context = context;
    }

    public DownloadTask(Context context, DownloadTaskProgress progressListener){
        this.context=context;
        this.progressListener = progressListener;
    }

    @Override
    protected String doInBackground(DownloadRequest... requests) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        String local = null;
        try {
            URL url = new URL(requests[0].url);

            connection = (HttpURLConnection) url.openConnection();
            connection.connect();


            int fileLength = connection.getContentLength();

            input = connection.getInputStream();
            String[] parts = url.getFile().split("/");
            local = String.format("%s/%s",requests[0].path, parts[parts.length-1]);
            output = new FileOutputStream(local);

            SecretKeySpec sks = new SecretKeySpec(DownloadTask.hexStringToByteArray(requests[0].key), "AES/CBC/NoPadding");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, sks, new IvParameterSpec(DownloadTask.hexStringToByteArray(requests[0].iv)));
            CipherInputStream cis = new CipherInputStream(input, cipher);


            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = cis.read(data)) != -1) {
                // allow canceling with back button
                if (isCancelled()) {
                    cis.close();
                    return null;
                }
                total += count;
                // publishing the progress....
                if (fileLength > 0) // only if total length is known
                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
            cis.close();
        } catch (Exception e) {
            Log.e("File",e.toString());
            return null;
        } finally {
            try {

                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return local;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        mWakeLock.acquire();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        // if we get here, length is known, now set indeterminate to false
        if(progressListener!=null)
            progressListener.onProgressUpdate(progress[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        mWakeLock.release();
        if(progressListener!=null)
            progressListener.onDownloadComplete(result!=null,result);

    }


    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }


    public interface DownloadTaskProgress{
        void onProgressUpdate(int progress);
        void onDownloadComplete(boolean successful, String uri);
    }
}
