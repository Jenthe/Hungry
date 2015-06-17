package be.sweatandsugar.hungry;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.Profile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import be.sweatandsugar.hungry.classes.Meal;


/**
 * A simple {@link Fragment} subclass.
 */
public class AllPicturesFragment extends Fragment {
    private RecyclerView rvAllPictures;
    private ImageView imgTest;

    private ArrayList<Meal> mAllMeals;
    private ArrayList<String> list;
    private ArrayList<String> mBys;
    private Bitmap bitmap;
    private ArrayList<Bitmap> mPictures;
    private int currentID = 0;

    private MyPicturesAdapter adapter;


    public AllPicturesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        list = new ArrayList<>();
        mBys = new ArrayList<>();
        mPictures = new ArrayList<>();

        Profile profile = Profile.getCurrentProfile();
        if (profile != null) {
            new CallAPI().execute("?fb_id=" + profile.getId() + "&action=allpictures");
        }
        View v = inflater.inflate(R.layout.fragment_all_pictures, container, false);

        rvAllPictures = (RecyclerView) v.findViewById(R.id.rvAllPictures);

        return v;
    }

    public class CallAPI extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            JsonReader reader = null;
            InputStream url = null;

            mAllMeals = new ArrayList<>();
            try {
                url = new URL("http://poolsidefashion.com/uploads/api.php" + strings[0]).openStream();
                reader = new JsonReader(new InputStreamReader(url, "UTF-8"));
                reader.beginArray();

                while (reader.hasNext()) {
                    reader.beginObject();
                    Meal meal = new Meal();
                    while (reader.hasNext()) {
                        String value = "";
                        value = reader.nextName();
                        if (value.equals("url")) {
                            //Log.d("url:", reader.nextString());
                            //list.add(reader.nextString());
                            meal.setUrl(reader.nextString());
                        } else if (value.equals("username")) {
                            //mBys.add(reader.nextString());
                            meal.setUsername(reader.nextString());
                            //Log.d("name:", reader.nextString());
                        } else {
                            reader.skipValue();
                            Log.d("skipped/", "skipped");
                        }

                    }
                    mAllMeals.add(meal);
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
                try {
                    url.close();
                } catch (IOException ignored) {
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (mAllMeals.size() > 0) {
                new DownloadPicturesTask().execute();
            }
        }
    }

    private class DownloadPicturesTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... voids) {

            try {
                URL url = new URL("http://poolsidefashion.com/uploads/" + mAllMeals.get(currentID).getUrl());
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
                rvAllPictures.setLayoutManager(layoutManager);
                adapter = new MyPicturesAdapter();
                rvAllPictures.setAdapter(adapter);
            } else {
                adapter.notifyDataSetChanged();
            }

            currentID++;

            if (currentID < mAllMeals.size()) {
                new DownloadPicturesTask().execute();
            }
        }
    }

    private class APILikePicture extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            try {
                URL url = new URL("http://poolsidefashion.com/uploads/api.php" + strings[0]);
                InputStream in = (InputStream) url.getContent();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }


    public class MyPicturesAdapter extends RecyclerView.Adapter<MyPicturesAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView imgPicture;
            private TextView tvBy;
            private ImageButton btnLike;

            public ViewHolder(View itemView) {
                super(itemView);
                this.imgPicture = (ImageView) itemView.findViewById(R.id.imgPicture);
                this.tvBy = (TextView) itemView.findViewById(R.id.tvBy);
                this.btnLike = (ImageButton) itemView.findViewById(R.id.btnLike);
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
            viewHolder.tvBy.setText(mAllMeals.get(i).getUsername());
            viewHolder.btnLike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
        }

        @Override
        public int getItemCount() {
            return mPictures.size();
        }
    }


}
