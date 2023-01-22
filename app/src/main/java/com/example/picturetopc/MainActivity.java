package com.example.picturetopc;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;





class Listener implements View.OnClickListener {
    IoHandler Handler;

    public Listener(IoHandler handler){
        Handler = handler;
    }

    @Override
    public void onClick(View view) {
        Handler.OnClick(view);
    }
}



class IoHandler {
    SharedPreferences.Editor Editor;
    EditText EditText;

    Button ButtonConnect;

    Button ButtonOkay;
    Button ButtonCancel;

    ProgressBar ProgressBar;
    MainActivity Main;

    ConnHanlder connHanlder;
    ConnListener connListener;

    public IoHandler(String code, SharedPreferences.Editor editor, EditText editText, Button buttonConnect, ProgressBar progessBar, MainActivity main){
        Editor = editor;
        EditText = editText;

        ButtonConnect = buttonConnect;
        ButtonCancel = main.dialog.findViewById(R.id.btn_cancel);
        ButtonOkay = main.dialog.findViewById(R.id.btn_okay);

        ProgressBar =progessBar;
        Main = main;

        ButtonConnect.setOnClickListener(new Listener(this));
        ButtonCancel.setOnClickListener(new Listener(this));
        ButtonOkay.setOnClickListener(new Listener(this));



        Load(code);

        try {
            connHanlder = new ConnHanlder("224.69.69.69", 42069, EditText);
            connListener = new ConnListener(ProgressBar, ButtonConnect);

        } catch (IOException e) {
            e.printStackTrace();
        }
        connHanlder.start();
        connListener.start();
    }
    public void Load(String code){
        EditText.setText(code);
    }

    public void Save() {
        Editor.putString("IP", EditText.getText().toString());
        Editor.commit();
    }

    public void OnClick(View view) {
        if (ButtonConnect.equals(view)) {
            Main.GetImage();
        }
        else if (ButtonCancel.equals(view)){
            Main.dialog.dismiss();
        }
        else if (ButtonOkay.equals(view)){
            Main.download();
            Main.dialog.dismiss();
        }
    }

    public void onPicture(Bitmap pic){
        connListener.SendAll(pic);
    }
}


public class MainActivity extends AppCompatActivity {

    private IoHandler connection;
    public Dialog dialog;
    DownloadManager manager;
    VersionControl versionControl;

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.update_dialog);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.update_background));
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().getAttributes().windowAnimations = R.style.animation;

        versionControl = new VersionControl();
        versionControl.getRequest(dialog::show);
        //dialog.setCancelable(false);

        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);

        final String code = sharedPreferences.getString("IP", null);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        connection = new IoHandler(code, editor, findViewById(R.id.IpAdress), findViewById(R.id.ConnectBtn), findViewById(R.id.progressBar),this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        String pic = "img.bmp";
        String dir = getExternalCacheDir().getAbsolutePath();
        new File(dir + "/" + pic).delete();

        connection.connListener.DisconnectAll();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        connection.Save();

    }


    public void GetImage(){
        intent = new Intent(this, Camera.class);

        canmeraActivity.launch(intent);


    }

    private ActivityResultLauncher<Intent> canmeraActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        String pic = "img.bmp";
                        String dir = getExternalCacheDir().getAbsolutePath();

                        Bitmap bmp = BitmapFactory.decodeFile(dir+ "/"+pic);
                        connection.onPicture(bmp);
                    }
                }
            });

    public void download(){
        manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(String.format("https://github.com/%s/%s/releases/latest/download/App.apk", Settings.AUTHOR, Settings.REPOSITORY));
        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        long r = manager.enqueue(request);
        DownloadBroadcast br = new DownloadBroadcast(manager, r);
        registerReceiver(br, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

}