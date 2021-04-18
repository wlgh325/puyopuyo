package com.example.puyopuyo;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.Buffer;
import java.util.Scanner;

import static com.example.puyopuyo.FieldSize.*;

public class ClientThread extends Thread {
    private GameSurface gameSurface;
    private static String addr;
    private static int port;
    private static final int TIMEOUT = 5000;

    private Socket mSocket;
    private boolean mShouldStop;
    private String msg;

    private OpponentPuyo opponentPuyo;

    public ClientThread(String addr, int port, GameSurface gameSurface, OpponentPuyo opponentPuyo) {
        this.addr = addr;
        this.port = port;
        this.gameSurface = gameSurface;
        this.opponentPuyo = opponentPuyo;
        mSocket = null;
        mShouldStop = true;

        sendConnectionMessage();
    }

    @Override
    public void run() {
        try {
            mSocket = new Socket(addr, port);
            mSocket.setSoTimeout(TIMEOUT);
            final PrintWriter out = new PrintWriter(mSocket.getOutputStream(), true);
            final BufferedReader in = new BufferedReader((new InputStreamReader(mSocket.getInputStream())));

            BufferedReader test_in;
            String str;

            String reply;
            mShouldStop = false;

            while (!mShouldStop) {
                test_in = new BufferedReader((new InputStreamReader(new ByteArrayInputStream(msg.getBytes()))));
                while ((str = test_in.readLine()) != null) {
                    Log.d("TAG", str);
                    out.write(str + "\n");
                    out.flush();

                    reply = in.readLine();
                    Log.d("REPLY", reply);

                    if (reply.startsWith("[CONNECTION]")) {
                        Network.player = reply.charAt(12) - 48;
                        Network.current_player++;
                    }
                    else if (reply.startsWith("[START]")) {
                        gameSurface.gameState = 1;
                    }
                    else if (reply.startsWith("[FIELD]")) {
                        int player = msg.charAt(7);
                        msg = msg.substring(7+1);
                        opponentPuyo.updateField(player, msg);
                    }
                }
                Thread.sleep(100);
            }

            /*
            while (!mShouldStop) {
                String s = "";
                for(int i = 0; i < MAX_ACTIVE_ROW; i++) {
                    for(int j = 0; j < MAX_COLUMN; j++) {
                        s += opponentPuyo.puyo[1][i][j]+"";
                    }
                }
                out.write(s + "\n");
                out.flush();

                    recv = in.readLine();
                    Log.d("Client Thread", recv);
                    if (recv == null) {
                        break;
                    }
                    Thread.sleep(1000);
            }*/

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (mSocket != null) {
                try {
                    mSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mSocket = null;
        mShouldStop = true;
    }

    public void closeConnection() throws IOException {
        mShouldStop = true;
        if (mSocket != null) {
            mSocket.close();
        }
        mSocket = null;
    }

    public void sendMessage(String message) {
        msg += message + "\r\n";
    }

    public void sendConnectionMessage() {
        msg = "[CONNECTION]";
    }

    public void sendFieldMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[FIELD]").append(Network.player).append(opponentPuyo.getStringMap(Network.player));

        msg = stringBuilder.append("\r\n").toString();
    }

    public void sendScoreMessage() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[SCORE]").append(gameSurface.fieldUIScoreList.get(Network.player).getScore());

        msg = stringBuilder.append("\r\n").toString();
    }

    public void sendAttackMessage(int garbagePuyo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ATTACK]").append(garbagePuyo);

        msg = stringBuilder.append("\r\n").toString();
    }
}
