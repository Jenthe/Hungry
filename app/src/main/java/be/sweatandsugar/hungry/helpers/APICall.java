package be.sweatandsugar.hungry.helpers;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by Jenthe on 28/05/2015.
 */
public class APICall {
    private String GetAllUsers(){
        new CallAPI().execute("users");
        return null;
    }

    public class CallAPI extends AsyncTask<String, Void, Object> {
        HttpURLConnection urlConnection = null;
        InputStream inStream = null;
        URL url = null;
        @Override
        protected Object doInBackground(String... strings) {
            try {
                URL url = new URL("http://poolsidefashion.com/uploads/" + strings[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.connect();
                inStream = urlConnection.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }
}
