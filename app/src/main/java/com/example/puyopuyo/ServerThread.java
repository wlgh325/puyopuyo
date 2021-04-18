package com.example.puyopuyo;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import static com.example.puyopuyo.FieldSize.*;
import java.nio.Buffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class ServerThread extends Thread {
    private GameSurface gameSurface;
    private static int GIVEN_PORT;
    private ServerSocket serverSocket;
    private OpponentPuyo opponentPuyo;

    public ServerThread(int port, GameSurface gameSurface, OpponentPuyo opponentPuyo) {
        GIVEN_PORT = port;
        this.gameSurface = gameSurface;
        this.opponentPuyo = opponentPuyo;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(GIVEN_PORT);
            HashMap<Integer, Object> hashMap = new HashMap<Integer, Object>();

            while (true) {
                if (Network.current_player < Network.max_player) {
                    Socket connection = serverSocket.accept();
                    Thread worker = new SimpleWorkerThread(connection, hashMap, opponentPuyo);
                    worker.start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeServer() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
        }
        serverSocket = null;
    }

    private static class SimpleWorkerThread extends Thread {
        private Socket socket;
        private OpponentPuyo opponentPuyo;
        final private HashMap<Integer, Object> hashMap;

        SimpleWorkerThread(Socket socket, HashMap<Integer, Object> hashMap, OpponentPuyo opponentPuyo) {
            this.socket = socket;
            this.hashMap = hashMap;
            this.opponentPuyo = opponentPuyo;
        }

        @Override
        public void run() {
            try {
                final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                String msg;

                while ((msg = in.readLine()) != null) {
                    Log.d("SERVER WORKER THREAD", "client: "+msg+" sent data");

                    if (msg.startsWith("[CONNECTION]")) {
                        //msg = msg.substring(12);
                        out.write("[CONNECTION]"+Network.current_player+"\r\n");
                        synchronized (hashMap) {
                            hashMap.put(Network.current_player, out);
                        }
                        Network.current_player++;

                        if (Network.current_player == Network.max_player) {
                            broadcast("[START]");
                        }
                    }
                    else if (msg.startsWith("[FIELD]")) {
                        int player = msg.charAt(7) - 48;
                        String temp_msg = msg.substring(7+1);
                        for (int p = 0; p < 4; p++) {
                            out.write("[FIELD]" + p + opponentPuyo.getStringMap(p)+"\r\n");
                        }
                        opponentPuyo.updateField(player, temp_msg);
                    }
                    else if (msg.startsWith("[ATTACK]")) {
                        broadcast(msg);
                    }
                    out.flush();
                }

                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void broadcast(String msg) { // TODO. 자신에게도 영향 주는지 확인
            synchronized (hashMap) {
                Collection<Object> collection = hashMap.values();
                Iterator<?> iterator = collection.iterator();
                while (iterator.hasNext()) {
                    PrintWriter pw = (PrintWriter)iterator.next();
                    pw.println(msg);
                    pw.flush();
                }
            }
        }
    }
}
