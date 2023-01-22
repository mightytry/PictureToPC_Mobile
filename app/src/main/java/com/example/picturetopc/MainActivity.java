package com.example.picturetopc;

import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;





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

    EditText NameEdit;
    EditText CodeEdit;

    Button ButtonConnect;

    Button ButtonOkay;
    Button ButtonCancel;

    ProgressBar ProgressBar;
    MainActivity Main;

    ConnHandler connHanlder;
    ConnListener connListener;

    public IoHandler(SharedPreferences sharedPreferences, EditText nameEdit, EditText codeEdit, Button buttonConnect, ProgressBar progessBar, MainActivity main){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Editor = editor;

        NameEdit = nameEdit;
        CodeEdit = codeEdit;

        ButtonConnect = buttonConnect;
        ButtonCancel = main.dialog.findViewById(R.id.btn_cancel);
        ButtonOkay = main.dialog.findViewById(R.id.btn_okay);

        ProgressBar =progessBar;
        Main = main;

        ButtonConnect.setOnClickListener(new Listener(this));
        ButtonCancel.setOnClickListener(new Listener(this));
        ButtonOkay.setOnClickListener(new Listener(this));



        Load(sharedPreferences.getString("Name", null), sharedPreferences.getString("Code", null));

        try {
            connHanlder = new ConnHandler("224.69.69.69", 42069, NameEdit, CodeEdit);
            connListener = new ConnListener(ProgressBar, Main.findViewById(R.id.Connections));

        } catch (IOException e) {
            e.printStackTrace();
        }
        connHanlder.start();
        connListener.start();
    }
    public void Load(String name, String code){
        NameEdit.setText(name);
        CodeEdit.setText(code);
    }

    public void Save() {
        Editor.putString("Name", NameEdit.getText().toString());
        Editor.putString("Code", CodeEdit.getText().toString());
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

        connection = new IoHandler(sharedPreferences, findViewById(R.id.Name), findViewById(R.id.IpAdress), findViewById(R.id.ConnectBtn), findViewById(R.id.progressBar),this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        String pic = "img.bmp";
        String dir = getExternalCacheDir().getAbsolutePath();
        deleteFile(dir + "/" + pic);

        connection.connListener.DisconnectAll();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        connection.Save();

    }


    public void GetImage(){
        Intent intent = new Intent(this, Camera.class);

        canmeraActivity.launch(intent);


    }

    private final ActivityResultLauncher<Intent> canmeraActivity = registerForActivityResult(
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