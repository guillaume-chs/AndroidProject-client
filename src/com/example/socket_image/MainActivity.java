package com.example.socket_image;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends Activity {
    public static final int REQUEST_CODE = 200;

    private final Handler handler;

    private int SERVEUR_PORT;
    private String SERVEUR_ADRESSE;

    private Uri imageURI;
    private ImageView imageView;
    private TextView imageTitle;

    /**
     * Creates a new activity.
     */
    public MainActivity() {
        this.handler = new Handler(Looper.getMainLooper());
        this.imageURI = null;
    }

    /**
     * Encode le tableau-donnée d'une image passé en paramètre en une chaîne de caractères en 64bit, visée à être envoyée au serveur.
     *
     * @param imageByteArray le tableau-donnée d'une image
     * @return une chaîne de caractères-donnée de l'image en 64bit
     */
    private static String encodeImage(byte[] imageByteArray) {
        return Base64.encodeToString(imageByteArray, Base64.DEFAULT);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        SERVEUR_PORT = Integer.parseInt(getResources().getString(R.string.default_port));
        SERVEUR_ADRESSE = getResources().getString(R.string.default_host);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        imageView = (ImageView) findViewById(R.id.imageView);
        imageTitle = (TextView) findViewById(R.id.imageTitle);

        (findViewById(R.id.buttonConnect)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.getImageFromServeur(v);
                    }
                });
            }
        });

        (findViewById(R.id.chooseFile)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.getImageIntent();
                    }
                });
            }
        });
    }

    public void getImageFromServeur(final View v) {
        Socket socket = null;
        DataInputStream inputStream = null;

        try {
            SERVEUR_ADRESSE = ((EditText) findViewById(R.id.host)).getText().toString();
            SERVEUR_PORT = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString());
            this.imageTitle.setText(SERVEUR_ADRESSE + ":" + SERVEUR_PORT);
            Log.d("Sock", " Trying to reach" + SERVEUR_ADRESSE + ":" + SERVEUR_PORT);

            socket = new Socket(SERVEUR_ADRESSE, SERVEUR_PORT);
            inputStream = new DataInputStream(socket.getInputStream());
            String base64Code = inputStream.readUTF();

            Log.d("String length", ":" + base64Code.length());
            byte[] decodedString;
            try {
                decodedString = Base64.decode(base64Code, Base64.DEFAULT);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("ErrorHere", e.toString());
                return;
            }

            // Si on a pu décoder l'image reçue
            Log.d("St--", ":" + decodedString.length);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            this.imageView.setImageBitmap(bitmap);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Error", "" + e);
        } finally {
            try {
                if (socket != null)
                    socket.close();
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean sendImageToServeur(final Uri ressourceURI) {
        Socket socket = null;
        DataOutputStream outputStream = null;

        try {
            // Getting the socket parameters
            SERVEUR_ADRESSE = ((EditText) findViewById(R.id.host)).getText().toString();
            SERVEUR_PORT = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString());
            Log.d("Sock", " Trying to reach" + SERVEUR_ADRESSE + ":" + SERVEUR_PORT);

            // Creates the socket
            socket = new Socket(SERVEUR_ADRESSE, SERVEUR_PORT);
            outputStream = new DataOutputStream(socket.getOutputStream());

            // Gets the file/picture to send, and converts it to a byte array
            final InputStream inputStream = getContentResolver().openInputStream(ressourceURI);
            final ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            final int bufferSize = 1024;
            final byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            final byte imageData[] = byteBuffer.toByteArray();
            final long imageRead = inputStream.read(imageData);

            // Checks size read
            Log.d("Image reading", "Read : " + imageRead);

            // Converts the image byte array into a Base64 String
            final String imageDataString = MainActivity.encodeImage(imageData);
            final byte[] imageByte = imageDataString.getBytes();
            Log.d("Byte image length", String.valueOf(imageByte.length));

            // Outputs the image data
            outputStream = new DataOutputStream(socket.getOutputStream());
            Log.d("Socket status", "Data : " + imageDataString);
            outputStream.writeInt(imageByte.length);
            outputStream.write(imageByte);
            Log.d("Image", "written");
            outputStream.flush();

            Log.d("Socket status", "Sending data ... ");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Error", "" + e);
            return false;
        } finally {
            try {
                if (socket != null)
                    socket.close();
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    private void getImageIntent() {
        // Single image picker
        final Intent imagePickerIntent = new Intent();
        imagePickerIntent.setType("image/*");
        imagePickerIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Start intent activity with result option
        startActivityForResult(Intent.createChooser(imagePickerIntent, "Select picture"), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            final Uri selectedImageURI = data.getData();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageURI(selectedImageURI);
                    imageTitle.setText(selectedImageURI.getPath());
                }
            });
            handler.post(new Runnable() {
                @Override
                public void run() {
                    MainActivity.this.sendImageToServeur(selectedImageURI);
                }
            });
        }
    }
}
