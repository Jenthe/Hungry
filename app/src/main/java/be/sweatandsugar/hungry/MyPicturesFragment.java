package be.sweatandsugar.hungry;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.Profile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class MyPicturesFragment extends Fragment {
    private ImageView imgTest;
    private RecyclerView rvPictures;
    private ArrayList<String> list;
    private Bitmap bitmap;
    private ArrayList<Bitmap> mPictures;
    private int currentID = 0;

    private MyPicturesAdapter adapter;

    public MyPicturesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        list = new ArrayList<String>();
        mPictures = new ArrayList<Bitmap>();

        Profile profile = Profile.getCurrentProfile();
        if (profile != null) {
            new CallAPI().execute("?fb_id=" + profile.getId() + "&action=pictures");
        }

        View v = inflater.inflate(R.layout.fragment_my_pictures, container, false);

        rvPictures = (RecyclerView) v.findViewById(R.id.rvMyPictures);

        return v;
    }

    public class CallAPI extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            JsonReader reader = null;
            try {
                InputStream url = new URL("http://poolsidefashion.com/uploads/api.php" + strings[0]).openStream();
                reader = new JsonReader(new InputStreamReader(url, "UTF-8"));
                reader.beginArray();

                while (reader.hasNext()) {
                    reader.beginObject();
                    while (reader.hasNext()) {
                        if (reader.nextName().equals("url")) {
                            list.add(reader.nextString());
                        }
                        ;
                    }

                    reader.endObject();
                }
                reader.endArray();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    assert reader != null;
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (list.size() > 0) {
                new DownloadPicturesTask().execute();
            }
        }
    }

    private class DownloadPicturesTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... voids) {

            try {
                URL url = new URL("http://poolsidefashion.com/uploads/" + list.get(currentID));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();

                InputStream response = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(response);

                response.close();
                conn.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            mPictures.add(bitmap);

            if (currentID == 0) {

                RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
                rvPictures.setLayoutManager(layoutManager);
                adapter = new MyPicturesAdapter();
                rvPictures.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }

            currentID++;

            if (currentID < list.size()) {
                new DownloadPicturesTask().execute();
            }
        }
    }

    public class MyPicturesAdapter extends RecyclerView.Adapter<MyPicturesAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView imgPicture;

            public ViewHolder(View itemView) {
                super(itemView);
                this.imgPicture = (ImageView) itemView.findViewById(R.id.imgPicture);
            }
        }

        @Override
        public MyPicturesAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {

            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_picture, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyPicturesAdapter.ViewHolder viewHolder, int i) {
            viewHolder.imgPicture.setImageBitmap(mPictures.get(i));
        }

        @Override
        public int getItemCount() {
            return mPictures.size();
        }
    }

}