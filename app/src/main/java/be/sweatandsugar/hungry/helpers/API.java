package be.sweatandsugar.hungry.helpers;

import android.os.AsyncTask;
import android.util.JsonReader;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Jenthe on 4/06/2015.
 */
public class API {
    public static JsonReader getJSON(final String requestMethod, final String url, final Map values, final Integer mode){
        new AsyncTask<Void, Void, JsonReader>(){
            private String boundary = "*****";
            private String crlf = "\r\n";
            private String twoHyphens = "--";
            @Override
            protected JsonReader doInBackground(Void... voids) {
                try {
                    URL apiUrl = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                    conn.setUseCaches(false);
                    conn.setDoOutput(true);

                    conn.setRequestMethod(requestMethod);
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("Cache-Control", "no-cache");
                    if (mode == 1) {
                        conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    } else {
                        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    }

                    DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                    Iterator it = values.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();

                        request.writeBytes(twoHyphens + boundary + crlf);
                        request.writeBytes("Content-Disposition: form-data; name=\"" + pair.getKey() + "\"" + crlf);
                        request.writeBytes(crlf);
                        request.write((byte[]) pair.getValue());
                        request.writeBytes(crlf);

                        System.out.println(pair.getKey() + " = " + pair.getValue());
                        it.remove(); // avoids a ConcurrentModificationException
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };

        return null;
    }

    public static JsonReader getJSON(String requestMethod, String url, Map values, String hashkey){
        return null;
    }
}
