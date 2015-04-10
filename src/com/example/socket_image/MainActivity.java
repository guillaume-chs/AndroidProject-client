package com.example.socket_image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends Activity {
    /**
     * Le port par défaut du serveur.
     *
     * @see @string/default_port
     */
    public int SERVEUR_PORT;
    /**
     * L'adresse par défaut du serveur.
     *
     * @see @string/default_host
     */
    public String SERVEUR_ADRESSE;

    private ImageView imageView;
    private Socket socket = null;
    private DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        SERVEUR_PORT = Integer.parseInt(getResources().getString(R.string.default_port));
        SERVEUR_ADRESSE = getResources().getString(R.string.default_host);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        imageView = (ImageView) findViewById(R.id.imageView);

        (findViewById(R.id.buttonConnect)).setOnClickListener((View v) ->
                new Thread(() ->
                        MainActivity.this.imageViewListener(v)
                ).start());
    }

    public void imageViewListener(View v) {
        try {
            SERVEUR_ADRESSE = ((EditText) findViewById(R.id.host)).getText().toString();
            SERVEUR_PORT = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString());
            ((TextView) findViewById(R.id.imageTitle)).setText(SERVEUR_ADRESSE + ":" + SERVEUR_PORT);
            Log.d("Sock", " Trying to reach" + SERVEUR_ADRESSE + ":" + SERVEUR_PORT);

            socket = new Socket(SERVEUR_ADRESSE, SERVEUR_PORT);
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
            String base64Code = dataInputStream.readUTF();

            Log.d("String", ":" + base64Code);
            byte[] decodedString = null;
            try {
                decodedString = Base64.decode(base64Code, Base64.DEFAULT);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("ErrorHere", "" + e);
            }
            Log.d("St--", ":" + decodedString.length);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0,
                    decodedString.length);

            imageView.setImageBitmap(bitmap);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Error", "" + e);
        } finally {
            try {
                if (socket != null)
                    socket.close();
                if (dataOutputStream != null)
                    dataOutputStream.close();
                if (dataInputStream != null)
                    dataInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
