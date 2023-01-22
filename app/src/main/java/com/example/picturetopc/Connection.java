package com.example.picturetopc;

import android.graphics.Bitmap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
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

class ConnHanlder extends Thread {
    MulticastSocket socket;

    InetAddress ip;
    int port;

    EditText code;


    public ConnHanlder(String _ip, int _port, EditText _code) throws IOException {
        socket = new MulticastSocket();
        ip = InetAddress.getByName(_ip);
        socket.joinGroup(ip);
        socket.setTimeToLive(20);


        port = _port;

        code = _code;
    }

    public void run() {
        socket.connect(ip, port);

        while (true) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String msg = null;


            msg = "{\"code\": \"" + code.getText().toString() + "\", \"ip\": \"" + "0" + "\", \"port\":" + 42069 + "}";


            DatagramPacket msgPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, this.ip, this.port);


            try {
                socket.send(msgPacket);
            } catch (IOException e) {
                run();
                e.printStackTrace();
            }
        }
    }
}

class ConnListener extends Thread
{
    ServerSocket socket;
    ProgressBar progressBar;
    List<Connection> connections;

    Button button;

    public ConnListener(ProgressBar _progressBar, Button _button) throws IOException{
        socket = new ServerSocket(42069);
        progressBar = _progressBar;

        connections = new ArrayList<>();

        button = _button;
    }

    public void run(){
        while (socket != null){
            try {
                connections.add(new Connection(socket.accept(), this));
                button.setText("Connected: " + connections.size());
            } catch (IOException e) {
                run();
                return;
            }
        }
    }

    public void Disconnect(Connection connection) {
        connections.remove(connection);
        button.setText("Connected: " + connections.size());
    }

    public void SendAll(Bitmap pic) {
        for (Connection conn:
                connections) {
            conn.Send(pic);
        }
    }

    public void DisconnectAll() {
        for (Connection conn:
                connections) {
            conn.Disconnect();
        }
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket = null;
    }
}

class ConnSender extends Thread {
    Connection Listener;

    List<byte[]> Sendable;

    public ConnSender(Connection listener){
        Sendable = new LinkedList<>();
        Listener = listener;
    }

    public void run() {
        while (!Listener.Stop){
            SendAll();
        }
    }


    private void SendAll() {
        if (Sendable.isEmpty()) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        int size = 1024;
        byte[] send = Sendable.remove(0);
        byte[] sdata = new byte[size];

        Listener.ConnListener.progressBar.setMax(send.length);
        try {
            for (int i = 0; i < send.length; i += size) {
                Listener.ConnListener.progressBar.setProgress(i);
                byte[] data = Arrays.copyOfRange(send, i, i+size);
                Listener.Output.write(data);
            }
        } catch (IOException e) {
            Listener.Disconnect();
            return;
        }
    }
    public void KeepAlive(){
        final byte[] biteses = "-1".getBytes(StandardCharsets.UTF_8);
        Sendable.add(biteses);
    }

    public void Send(Bitmap bmp){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();

        Sendable.add((String.valueOf(b.length)).getBytes(StandardCharsets.UTF_8));
        Sendable.add(b);

    }
}


class ConnTimeout extends Thread {
    ConnSender Sender;

    public ConnTimeout(ConnSender sender){
        Sender = sender;
    }

    public void run() {
        while (!Sender.Listener.Stop){
            SendAll();
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void SendAll() {
        Sender.KeepAlive();
    }
}

class Connection {
    java.net.Socket Socket;
    InputStream Input;
    OutputStream Output;

    //ConnReader Reader;
    ConnSender Sender;

    ConnTimeout Timeout;



    ConnListener ConnListener;
    boolean Stop;

    public Connection(Socket socket, ConnListener connListener) throws SocketException {
        Socket = socket;
        socket.setSoTimeout(1*1000);
        ConnListener = connListener;

        Stop = false;

        try {
            Input = Socket.getInputStream();
            Output = Socket.getOutputStream();

        } catch (IOException e) {
            return;
        }

        //Reader = new ConnReader(this);
        Sender = new ConnSender(this);

        Timeout = new ConnTimeout(Sender);

        //Reader.start();
        Sender.start();

        Timeout.start();
    }

    public void Send(Bitmap bmp){
        Sender.Send(bmp);
    }

    public void Disconnect() {
        try {
            Socket.close();}
        catch (IOException e)
        {}
        Stop = true;

        //Sender.join();
        //Reader.join();

        ConnListener.Disconnect(this);
    }
}
