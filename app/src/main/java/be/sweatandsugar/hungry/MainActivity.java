package be.sweatandsugar.hungry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.Profile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import be.sweatandsugar.hungry.classes.User;
import be.sweatandsugar.hungry.helpers.Constants;


public class MainActivity extends ActionBarActivity implements LoginFragment.OnLoginFragmentListener, MainFragment.OnMainFragmentListener {
    private Toolbar toolbar;
    private ImageView imgProfile;

    private Bitmap bmProfilePicture;
    private TextView tvExp;

    private User mUser;

    private DownloadProfilePictureAndSave mDownloadTask;

    private int mExp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this);
        setContentView(R.layout.activity_main);

        imgProfile = (ImageView) findViewById(R.id.imgProfile);
        tvExp = (TextView) findViewById(R.id.tvExp);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mDownloadTask = new DownloadProfilePictureAndSave();

        FragmentManager fm2 = getSupportFragmentManager();

        if (fm2.findFragmentByTag("login_fragment") != null) {
            toolbar.setVisibility(View.GONE);
        }

        if (savedInstanceState == null) {
            toolbar.setVisibility(View.GONE);
            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction()
                    .replace(R.id.container, new LoginFragment(), "login_fragment")
                    .commit();
        } else {
            bmProfilePicture = savedInstanceState.getParcelable("profile_picture");
            imgProfile.setImageBitmap(bmProfilePicture);
            tvExp.setText(savedInstanceState.getInt("exp") + "/100");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("profile_picture", bmProfilePicture);
        outState.putInt("exp", mExp);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        mDownloadTask.cancel(true);
        super.onStop();
    }

    @Override
    public void onLoginSuccess(User user) {
        toolbar.setVisibility(View.VISIBLE);

        if (user != null) {
            mUser = user;
            tvExp.setText(user.getExp() + "/100");

            File f = new File(getFilesDir(), user.getPicture());

            if (f.exists()) {
                try {
                    bmProfilePicture = BitmapFactory.decodeStream(new FileInputStream(f));
                    imgProfile.setImageBitmap(bmProfilePicture);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                mDownloadTask.execute(user);
            }
        }

        Bundle bundle = new Bundle();
        bundle.putSerializable("mainuser", mUser);
        MainFragment frag = new MainFragment();
        frag.setArguments(bundle);

        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.container, frag, "main_fragment")
                .commit();
    }

    @Override
    public void onMyPicturesButton() {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.container, new MyPicturesFragment(), "my_pictures_fragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAllPicturesButton() {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction()
                .replace(R.id.container, new AllPicturesFragment(), "my_pictures_fragment")
                .addToBackStack(null)
                .commit();
    }

    public class DownloadProfilePictureAndSave extends AsyncTask<User, Void, Bitmap> {
        private User newUser;

        @Override
        protected Bitmap doInBackground(User... users) {
            try {
                newUser = users[0];
                URL url = new URL(Constants.PROFILES_URL + "/" + newUser.getPicture());
                InputStream in = (InputStream) url.getContent();
                return BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            File file = new File(getFilesDir(), newUser.getPicture());

            FileOutputStream outputStream;

            try {
                outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.close();
                imgProfile.setImageBitmap(bitmap);
                bmProfilePicture = bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
