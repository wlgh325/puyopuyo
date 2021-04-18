package com.example.puyopuyo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.Log;

import java.util.Arrays;

import static android.graphics.Bitmap.createScaledBitmap;
import static com.example.puyopuyo.FieldSize.*;

public class OpponentPuyo extends GameObject {
    private GameSurface gameSurface;
    private Bitmap[][] bitmaps = new Bitmap[2][6];
    private String[] stringMap = new String[4];

    public int[][][] puyo = new int[4][MAX_ACTIVE_ROW][MAX_COLUMN];

    public OpponentPuyo(GameSurface gameSurface, Bitmap image) {
        super(image, 2, 6, 0, 0);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 5; j++) {
                bitmaps[i][j] = this.createSubImageAt(i, j);
                bitmaps[i][j] = createScaledBitmap(bitmaps[i][j], (int) (width * SCREEN_RATIO), (int) (height * SCREEN_RATIO), true);
            }
        }

        this.gameSurface = gameSurface;
    }

    @Override
    public void draw(Canvas canvas)  {
        //Log.d("TAG", ""+puyo[1][0][0]+"\n");
        int draw_x, draw_y;
        for (int p = 0; p < 4; p++) {
            for (int i = 0; i < MAX_ACTIVE_ROW; i++) {
                for (int j = 0; j < MAX_COLUMN; j++) {
                    if (p != Network.player) {
                        //Log.d("TAG", ""+puyo[p][i][j]);
                        if (puyo[p][i][j] != 0) {
                            draw_x = 24 + 9 + j * COLUMN;
                            draw_y = (502 - (i * ROW));
                            canvas.drawBitmap(bitmaps[0][puyo[p][i][j]], (234 * p + draw_x) * SCREEN_RATIO, (draw_y - height) * SCREEN_RATIO, null);
                        }
                    }
                }
            }
        }

        lastDrawNanoTime = System.nanoTime();
    }

    @Override
    public void update() {
        for (int i = 0; i < MAX_ACTIVE_ROW; i++) {
            for (int j = 0; j < MAX_COLUMN; j++) {
                if (gameSurface.puyoArray[i][j] == null)
                    puyo[Network.player][i][j] = 0;
                else
                    puyo[Network.player][i][j] = gameSurface.puyoArray[i][j].getColor();
            }
        }
    }

    public void updateField(int player, String string) {
        stringMap[player] = string;
        //String string = "100300000000000000000000000000000000000000000000000000000000000000000000000000";
        for (int i = 0; i < MAX_ACTIVE_ROW; i++) {
            for (int j = 0; j < MAX_COLUMN; j++) {
                puyo[player][i][j] = (int) stringMap[player].charAt(MAX_COLUMN*i + j) - 48;
            }
        }
        //Log.d("TAG", ""+puyo[1][0][0]+"\n");
    }

    public String getStringMap(int player) {
        if (player < 0 || player > stringMap.length) {
            return null;
        }
        return stringMap[player];
    }

    public void setColors() {

    }
}
