package com.generated.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements GameView.OnGameEventListener {

    private static final String PREFS_NAME = "2048_prefs";
    private static final String KEY_BEST_SCORE = "best_score";

    private GameView gameView;
    private TextView tvScore;
    private TextView tvBestScore;
    private Button btnUndo;
    private Button btnNewGame;

    private LinearLayout overlayLayout;
    private TextView overlayTitle;
    private TextView overlayMessage;
    private Button btnOverlayAction;
    private Button btnOverlayContinue;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int bestScore = prefs.getInt(KEY_BEST_SCORE, 0);

        tvScore = findViewById(R.id.tv_score);
        tvBestScore = findViewById(R.id.tv_best_score);
        tvBestScore.setText(String.valueOf(bestScore));

        btnUndo = findViewById(R.id.btn_undo);
        btnNewGame = findViewById(R.id.btn_new_game);

        overlayLayout = findViewById(R.id.overlay_layout);
        overlayTitle = findViewById(R.id.overlay_title);
        overlayMessage = findViewById(R.id.overlay_message);
        btnOverlayAction = findViewById(R.id.btn_overlay_action);
        btnOverlayContinue = findViewById(R.id.btn_overlay_continue);

        gameView = findViewById(R.id.game_view);
        gameView.setBestScore(bestScore);
        gameView.setOnGameEventListener(this);

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gameView.canUndo()) {
                    gameView.undo();
                    hideOverlay();
                }
            }
        });

        btnNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewGame();
            }
        });

        btnOverlayAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewGame();
            }
        });

        btnOverlayContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideOverlay();
                gameView.continuePlaying();
            }
        });

        startNewGame();
    }

    private void startNewGame() {
        hideOverlay();
        gameView.startNewGame();
    }

    private void hideOverlay() {
        overlayLayout.setVisibility(View.GONE);
    }

    @Override
    public void onScoreChanged(int currentScore, int bestScore) {
        tvScore.setText(String.valueOf(currentScore));
        tvBestScore.setText(String.valueOf(bestScore));

        prefs.edit().putInt(KEY_BEST_SCORE, bestScore).apply();
    }

    @Override
    public void onGameOver() {
        overlayTitle.setText(R.string.game_over_title);
        overlayMessage.setText(R.string.game_over_msg);
        btnOverlayAction.setText(R.string.try_again);
        btnOverlayContinue.setVisibility(View.GONE);
        overlayLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGameWon() {
        overlayTitle.setText(R.string.you_won_title);
        overlayMessage.setText(R.string.you_won_msg);
        btnOverlayAction.setText(R.string.new_game);
        btnOverlayContinue.setVisibility(View.VISIBLE);
        overlayLayout.setVisibility(View.VISIBLE);
    }
}
