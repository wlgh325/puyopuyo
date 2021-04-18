package com.example.puyopuyo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import static android.graphics.Bitmap.createScaledBitmap;
import static com.example.puyopuyo.FieldSize.SCREEN_RATIO;

public class Score extends GameObject {
    private GameSurface gameSurface;

    private int rowIndex; // For Image
    private int colIndex; // For Image
    private int score;

    public Score(GameSurface gameSurface, Bitmap image, int x, int y) {
        super(image, 1, 10, x, y, 0.3f, 0.4f); // row, col 에 맞는 위치로 설정
        this.gameSurface = gameSurface;
    }

    @Override
    public void draw(Canvas canvas)  {
        int scoreTemp = score;
        int scorePrint[] = {0, 0, 0, 0, 0, 0, 0, 0};

        for (int i = 7; i >= 0; i--) {
            if (scoreTemp == 0)
                break;

            scorePrint[i] = scoreTemp % 10;
            scoreTemp /= 10;
        }

        for (int i = 0; i < 8; i++) {
            Bitmap bitmap = this.createSubImageAt(0, scorePrint[i]);
            bitmap = createScaledBitmap(bitmap, (int) (width * SCREEN_RATIO * xscale), (int) (height * SCREEN_RATIO * yscale), true);
            canvas.drawBitmap(bitmap, (x + i*20) * SCREEN_RATIO, y * SCREEN_RATIO, null);
        }

        lastDrawNanoTime = System.nanoTime();
    }

    @Override
    public void update() {
        super.update();
    }

    public int getScore() {
        return score;
    }

    public void addScore(int add) {
        score += add;
    }

    public void addScore(int puyo_number, int chain_cnt, int[] connection_number, int color_number) {
        int add;
        // 분석할 필요 X
        add = puyo_number * Math.max(1, (getChainBonus(chain_cnt) + getConnectionBonus(connection_number) + getColorBonus(color_number))) * 10;
        score += add;
        gameSurface.calculateGarbagePuyo(add);
        //Log.d("TAG", puyo_number+", "+chain_cnt+", ["+connection_number[0]+connection_number[1]+connection_number[2]+connection_number[3]+"], "+color_number);
        //Log.d("TAG", "Score: "+add);
    }



    private int getChainBonus(int chain) {
        int bonus = 0;
        if (chain == 0) {
            return bonus;
        }
        else if (chain <= 5) {
            bonus = (int) Math.pow(2, chain - 2) * 8;
        }
        else {
            bonus = (chain - 3)*32;
        }

        return bonus;
    }

    private int getConnectionBonus(int[] connection_number) {
        int bonus = 0;

        for (int i = 0; i < connection_number.length; i++) {
            if (connection_number[i] <= 4) {
                bonus = 0;
            } else if (connection_number[i] < 10) {
                bonus = connection_number[i] - 3;
            } else {
                bonus = 10;
            }
        }

        return bonus;
    }

    private int getColorBonus(int color_number) {
        int bonus = 0;

        if (color_number <= 1) {
            return bonus;
        }
        else {
            bonus = (int) Math.pow(2, color_number - 2) * 3;
        }

        return bonus;
    }
}
