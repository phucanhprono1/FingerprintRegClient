package com.example.fingerprintregclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAMERA_CODE = 1001;
    ImageView importPic;
    Button Import,Camera,SandC;
    TextView noti;
    private final int REQUEST_CODE=1000;
    Uri image_uri;
    String receive_mess="";
    String host="192.168.0.6";
    InputStream imgstream=null;
    byte[] array;
    private static final String CAMERA_IMAGE_FILE_NAME = "image.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        importPic = (ImageView) findViewById(R.id.imageView);
        Import = (Button) findViewById(R.id.Import);
        Camera = (Button) findViewById(R.id.camera);
        SandC = (Button) findViewById(R.id.check);
        noti = (TextView) findViewById(R.id.notification);
        noti.setTextColor(Color.BLACK);
        Drawable sendImage = importPic.getDrawable();
        Import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent,REQUEST_CODE);
            }
        });
        Camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){

                if(checkSelfPermission(Manifest.permission.CAMERA)== PackageManager.PERMISSION_DENIED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
                    String[] permission = {Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    requestPermissions(permission,PERMISSION_CODE);
                }
                else{
                    openCamera();
                }

            }
        });





        SandC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    imgstream = getContentResolver().openInputStream(image_uri);
                    final Bitmap selectedImage = BitmapFactory.decodeStream(imgstream);
                    importPic.setImageBitmap(selectedImage);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    selectedImage.compress(Bitmap.CompressFormat.PNG,0,bos);
                    array = bos.toByteArray();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
//                try {
//                    final InputStream imgstream = getContentResolver().openInputStream(image_uri);
//                    final Bitmap selectedImage = BitmapFactory.decodeStream(imgstream);
//                    importPic.setImageBitmap(selectedImage);
//                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                    selectedImage.compress(Bitmap.CompressFormat.PNG,0,bos);
//                    byte[] array = bos.toByteArray();
//                    SendImageClient sic = new SendImageClient();
//                    sic.execute(array);
//                    if(!receive_mess.equals("")){
//                        noti.setText(receive_mess);
//                    }
////                    Receiver r = new Receiver();
////                    r.execute();
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();

                Thread thread =new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Socket socket= new Socket(host, 5000);


                            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                            BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            dos.write(array, 0, array.length);
                            dos.close();
                            while (receive_mess.isEmpty()) {
                                receive_mess = in.readLine();
                            }

                            noti.setText(receive_mess);

                            socket.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }
                });

                thread.start();

            }

        });


    }

    public class SendImageClient extends AsyncTask<byte[],Void,Void>{
        @Override
        protected Void doInBackground(byte[]... bytes) {
            try {
                Socket socket = new Socket(host, 5000);

                DataOutputStream dos=new DataOutputStream(socket.getOutputStream());
                DataInputStream ois=new DataInputStream(socket.getInputStream());
                dos.write(bytes[0],0,bytes[0].length);

                dos.flush();
                dos.close();

                receive_mess = ois.readLine();

                ois.close();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private void openCamera(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION,"New Picture");
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,image_uri);
        startActivityForResult(cameraIntent,IMAGE_CAMERA_CODE);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==REQUEST_CODE){
                importPic.setImageURI(data.getData());
                image_uri = data.getData();


            }
            else if(requestCode==IMAGE_CAMERA_CODE){
                importPic.setImageURI(image_uri);

            }


        }
    }

}
