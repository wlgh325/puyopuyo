package com.example.puyopuyo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

import static android.content.ContentValues.TAG;

public class PuyoChainingAnimation extends GameObject {
    private GameSurface gameSurface;
    private StageManager stageManager;
    private Queue<Puyo> puyos = new LinkedList<>(); // 지워질 뿌요 목록
    private int removeTimer;
    private boolean isRemoving;

    public PuyoChainingAnimation(GameSurface gameSurface, StageManager stageManager) {
        this.gameSurface = gameSurface;
        this.stageManager = stageManager;
    }

    @Override
    public void draw(Canvas canvas)  {
        lastDrawNanoTime = System.nanoTime();
    }

    @Override
    public void update() {
        super.update();
        if (isRemoving) {
            removeTimer += deltaTime;
            if (removeTimer >= 1000) {
                while (!puyos.isEmpty()) {
                    gameSurface.destroyPuyo(puyos.element().getRow(), puyos.element().getColumn());
                    puyos.remove();
                }
            }
            if (removeTimer >= 1100) {
                stageManager.endChainStage();
                isRemoving = false;
            }
        }
    }

    public void startChainingAnimation() {
        removeTimer = 0;
        isRemoving = true;
    }

    public void addPuyo(Puyo puyo) {
        puyos.add(puyo);
    }
}