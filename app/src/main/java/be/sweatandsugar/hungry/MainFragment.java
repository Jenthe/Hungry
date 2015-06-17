package be.sweatandsugar.hungry;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.facebook.Profile;

import org.apache.http.client.HttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import be.sweatandsugar.hungry.classes.User;
import be.sweatandsugar.hungry.helpers.Constants;


public class MainFragment extends Fragment {
    private OnMainFragmentListener mCallback;

    private ImageButton btnTakePicture;
    private ImageButton btnMyPictures;
    private ImageButton btnAllPictures;
    private String mPictureName;
    private String mPicturePath;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    String boundary = "*****";
    String crlf = "\r\n";
    String twoHyphens = "--";

    User mUser;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnMainFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMainFragmentListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        btnTakePicture = (ImageButton) v.findViewById(R.id.btnTakePicture);
        btnMyPictures = (ImageButton) v.findViewById(R.id.btnMyPictures);
        btnAllPictures = (ImageButton) v.findViewById(R.id.btnAllPictures);

        mUser = (User) getArguments().getSerializable("mainuser");

        btnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    File file = null;

                    try {
                        file = createImageFile();
                    } catch (IOException ex) {
                        Log.e("Error creating file: ", ex.toString());
                    }

                    if (file != null) {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    }

                }
            }
        });

        btnAllPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onAllPicturesButton();
            }
        });

        btnMyPictures.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onMyPicturesButton();
            }
        });

        return v;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {

            InputStream inputStream;
            Bitmap original_picture;
            try {
                //Load high-quality picture
                inputStream = getActivity().getContentResolver().openInputStream(Uri.parse(mPicturePath));
                BufferedInputStream bis = new BufferedInputStream(inputStream);
                original_picture = BitmapFactory.decodeStream(bis);

                //Resize it
                int originalHeight = original_picture.getHeight();
                int originalWidth = original_picture.getWidth();
                int targetHeight = 720;
                float targetWidth = ((float) originalWidth / (float) originalHeight * (float) targetHeight);
                Bitmap resized_picture = Bitmap.createScaledBitmap(original_picture, Math.round(targetWidth), targetHeight, false);

                //Compress it
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                resized_picture.compress(Bitmap.CompressFormat.JPEG, 60, out);
                byte[] pixels = out.toByteArray();

                new UploadPictureTask().execute(pixels);

                //Overwrite original picture with the compressed one:
                File storageDir = getActivity().getApplicationContext().getExternalFilesDir(null);
                File file = new File(storageDir, mPictureName);
                FileOutputStream f = null;

                try {
                    f = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (f != null) {
                    try {
                        f.write(pixels);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        f.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        f.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }
    }

    private class UploadPictureTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... bytes) {
            try {
                URL url = new URL(Constants.API_URL + "users/" + mUser.getId() + "/meals");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoOutput(true);

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("Cache-Control", "no-cache");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(conn.getOutputStream());

                request.writeBytes(twoHyphens + boundary + crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"" + "upload" + "\";filename=\"" + mPicturePath + "\"" + crlf);
                request.writeBytes(crlf);
                request.write(bytes[0]);
                request.writeBytes(crlf);

                String auth = "/users/" + mUser.getId() + "/meals&upload=" + mPictureName + mUser.getToken(getActivity());
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(auth.getBytes("UTF-8"));
                byte[] digest = md.digest();

                StringBuilder sb = new StringBuilder();
                for (byte aDigest : digest) {
                    sb.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
                }

                request.writeBytes(twoHyphens + boundary + crlf);
                request.writeBytes("Content-Disposition: form-data; name=\"" + "token" + "\"" + crlf);
                request.writeBytes(crlf);
                request.writeBytes(sb.toString());
                request.writeBytes(crlf);
                request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

                request.flush();
                request.close();

                InputStream response = new BufferedInputStream(conn.getInputStream());
                BufferedReader responseReader = new BufferedReader(new InputStreamReader(response));
                String line = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((line = responseReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                responseReader.close();

                String responseString = stringBuilder.toString();
                response.close();
                conn.disconnect();

            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    private File createImageFile() throws IOException {
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + time + "_";
        File storageDir = getActivity().getApplicationContext().getExternalFilesDir(null);
        File file = File.createTempFile(imageFileName, ".jpg", storageDir);

        mPicturePath = "file:" + file.getAbsolutePath();
        mPictureName = file.getName();

        return file;
    }

    public interface OnMainFragmentListener {
        void onMyPicturesButton();
        void onAllPicturesButton();
    }
}
