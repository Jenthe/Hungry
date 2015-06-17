package be.sweatandsugar.hungry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;

import be.sweatandsugar.hungry.classes.User;
import be.sweatandsugar.hungry.helpers.Constants;

public class LoginFragment extends Fragment {
    OnLoginFragmentListener mCallbackListener;

    private ImageView imgCat;
    private AccessTokenTracker mTokenTracker;
    private ProfileTracker mProfileTracker;
    private CallbackManager mCallbackManager;

    private APIPOSTUsers mTaskPOSTAPIUsers;
    private APIGETUsersFbId mTaskGETAPIUsersFbId;
    private String test;

    private Profile mProfile;

    private Handler mHandler;

    private FacebookCallback<LoginResult> mCallback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            mProfile = Profile.getCurrentProfile();

            //Check if user has an account. If not, create one and return User object to activity
            mTaskGETAPIUsersFbId.execute(mProfile.getId());
        }

        @Override
        public void onCancel() {

        }

        @Override
        public void onError(FacebookException e) {

        }
    };

    public interface OnLoginFragmentListener {
        void onLoginSuccess(User user);
    }

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallbackListener = (OnLoginFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnLoginFragmentListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getActivity().getApplicationContext());
        mCallbackManager = CallbackManager.Factory.create();
        mTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldToken, AccessToken newToken) {

            }
        };
        mProfileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile newProfile) {

            }
        };

        mTokenTracker.startTracking();
        mProfileTracker.startTracking();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        mHandler = new Handler();

        imgCat = (ImageView) v.findViewById(R.id.imgCat);
        LoginButton loginButton = (LoginButton) v.findViewById(R.id.login_button);
        loginButton.setReadPermissions("user_friends");
        loginButton.setFragment(this);
        loginButton.registerCallback(mCallbackManager, mCallback);

        mProfile = Profile.getCurrentProfile();
        mTaskGETAPIUsersFbId = new APIGETUsersFbId();
        mTaskPOSTAPIUsers = new APIPOSTUsers();

        if (mProfile != null) {
            loginButton.setVisibility(View.GONE);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTaskGETAPIUsersFbId.execute(mProfile.getId());
                }
            }, 1000);
        }

        return v;
    }

    @Override
    public void onStop() {
        super.onStop();

        mHandler.removeCallbacksAndMessages(null);
        mTaskGETAPIUsersFbId.cancel(true);
        mTaskPOSTAPIUsers.cancel(true);
        mTokenTracker.stopTracking();
        mProfileTracker.stopTracking();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private User JSONtoUser(JsonReader reader) throws IOException {
        User user = null;
        reader.beginObject();
        if (reader.peek() == JsonToken.NAME) {
            user = new User();
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "id":
                        user.setId(Integer.parseInt(reader.nextString()));
                        break;
                    case "fb_id":
                        user.setFb_id(reader.nextString());
                        break;
                    case "token":
                        SharedPreferences.Editor edit = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                        edit.putString("token",reader.nextString());
                        edit.apply();
                        break;
                    case "exp":
                        user.setExp(Integer.parseInt(reader.nextString()));
                        break;
                    case "name":
                        user.setName(reader.nextString());
                        break;
                    case "picture":
                        user.setPicture(reader.nextString());
                        break;
                    default:
                        reader.skipValue();
                        break;
                }
            }
        }
        reader.endObject();
        return user;
    }

    public class APIGETUsersFbId extends AsyncTask<String, Void, User> {

        @Override
        protected User doInBackground(String... strings) {
            JsonReader reader = null;
            try {
                InputStream url = new URL(Constants.API_URL + "users/fb/" + strings[0]).openStream();
                reader = new JsonReader(new InputStreamReader(url, "UTF-8"));
                reader.beginArray();

                if (reader.hasNext()) {
                    return JSONtoUser(reader);
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
        protected void onPostExecute(User user) {
            if (user != null) {
                mCallbackListener.onLoginSuccess(user);
            } else {
                mTaskPOSTAPIUsers.execute(mProfile.getId(), mProfile.getName(), mProfile.getProfilePictureUri(50, 50).toString(), mProfile.getFirstName() + mProfile.getId());
            }
        }
    }

    public class APIPOSTUsers extends AsyncTask<String, Void, User> {

        @Override
        protected User doInBackground(String... strings) {
            JsonReader reader = null;
            User user = null;
            try {
                URL url = new URL(Constants.API_URL + "users");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                String uri = "fb_id=" + strings[0] + "&name=" + strings[1] + "&picturename=" + strings[3] + ".jpg";
                String tokenuri = uri + "kwGHOLhrT9DkuS8kH3fX";
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(tokenuri.getBytes("UTF-8"));
                byte[] digest = md.digest();

                StringBuilder sb = new StringBuilder();
                for (byte aDigest : digest) {
                    sb.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
                }

                uri += "&picture=" + Uri.encode(strings[2]);
                uri += "&registertoken=" + sb.toString();

                request.writeBytes(uri);

                request.flush();
                request.close();

                InputStream errorstream = conn.getErrorStream();

                int responseCode = conn.getResponseCode();

                if (errorstream != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(errorstream));

                    String response = "";
                    String nachricht;
                    while ((nachricht = br.readLine()) != null) {
                        response += nachricht;
                    }

                } else {
                    InputStream response = conn.getInputStream();
                    reader = new JsonReader(new InputStreamReader(response, "UTF-8"));
                    reader.beginArray();

                    user = JSONtoUser(reader);

                    reader.endArray();
                    response.close();
                }
                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return user;
        }

        @Override
        protected void onPostExecute(User user) {
            mCallbackListener.onLoginSuccess(user);
        }
    }
}
