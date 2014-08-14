package com.ethanmad.engarde;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;

public class MainActivity extends Activity implements CardAlertFragment.CardAlertListener {
    long mTimeRemaining, mPeriodLength, mBreakLength;
    long[] mStartVibrationPattern, mEndVibrationPattern;
    int mScoreOne, mScoreTwo, mPeriodNumber, mMode, mMaxPeriods, mPriority;
    TextView mTimer, mScoreOneView, mScoreTwoView, mPeriodView;
    ImageView mYellowIndicatorLeft, mRedIndicatorLeft, mYellowIndicatorRight, mRedIndicatorRight;
    boolean mTimerRunning, mInPeriod, mInBreak, mInPriority, mOneHasYellow, mOneHasRed, mTwoHasYellow, mTwoHasRed;
    CountDownTimer mCountDownTimer;
    Vibrator mVibrator;
    Uri mAlert;
    Ringtone mRinger;
    Animation mBlink;
    ArrayDeque<Integer> mRecentActions;
    int[] mRecentActionArray;
    MenuItem mActionUndo;
    Toast mToast;
    SharedPreferences mSharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        mTimer = (TextView) findViewById(R.id.timer);
        mScoreOneView = (TextView) findViewById(R.id.scoreOne);
        mScoreTwoView = (TextView) findViewById(R.id.scoreTwo);
        mPeriodView = (TextView) findViewById(R.id.periodView);
        mYellowIndicatorLeft = (ImageView) findViewById(R.id.yellowCircleViewOne);
        mRedIndicatorLeft = (ImageView) findViewById(R.id.redCircleViewOne);
        mYellowIndicatorRight = (ImageView) findViewById(R.id.yellowCircleViewTwo);
        mRedIndicatorRight = (ImageView) findViewById(R.id.redCircleViewTwo);

        // import previous data if it exists, otherwise use default values on right
        if (savedInstanceState == null) savedInstanceState = new Bundle();
        mPeriodLength = savedInstanceState.getLong("mPeriodLength", 3 * 60 * 1000);
        mTimeRemaining = savedInstanceState.getLong("mTimeRemaining", mPeriodLength);
        mScoreOne = savedInstanceState.getInt("mScoreOne", 0);
        mScoreTwo = savedInstanceState.getInt("mScoreTwo", 0);
        mPriority = savedInstanceState.getDouble("mPriority", -1);
        mTimerRunning = savedInstanceState.getBoolean("mTimerRunning", false);
        mPeriodNumber = savedInstanceState.getInt("mPeriodNumber", 1);
        mBreakLength = savedInstanceState.getLong("mBreakLength", 1 * 60 * 1000);
        mMode = savedInstanceState.getInt("mMode", 5);
        mInPeriod = savedInstanceState.getBoolean("mInPeriod", true);
        mInBreak = savedInstanceState.getBoolean("mInBreak", false);
        mInPriority = savedInstanceState.getBoolean("mInPriority", false);
        mOneHasYellow = savedInstanceState.getBoolean("mOneHasYellow", false);
        mOneHasRed = savedInstanceState.getBoolean("mOneHasRed", false);
        mTwoHasRed = savedInstanceState.getBoolean("mTwoHasRed", false);
        mTwoHasYellow = savedInstanceState.getBoolean("mTwoHasYellow", false);
        mRecentActionArray = savedInstanceState.getIntArray("mRecentActionArray");

        updateViews(); // update all views from default strings to real data
        loadSettings(); // load user settings

        if (mRecentActionArray == null) mRecentActions = new ArrayDeque<Integer>(0);
        else for (int action : mRecentActionArray)
            mRecentActions.push(action);

        // set-up blinking animation used when mTimer is paused TODO: make animation better (no fade)
        mBlink = new AlphaAnimation(0.0f, 1.0f);
        mBlink.setDuration(1000);
        mBlink.setStartOffset(0);
        mBlink.setRepeatCount(Animation.INFINITE);
        mBlink.setRepeatMode(Animation.START_ON_FIRST_FRAME);

        // used to signal to user that mTimeRemaining has expired
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mStartVibrationPattern = new long[]{0, 50, 100, 50};
        mEndVibrationPattern = new long[]{0, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50, 500, 50, 100, 50/**/};
        mAlert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (mAlert == null) {
            // mAlert is null, using backup
            mAlert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            // just in case
            if (mAlert == null) {
                // mAlert backup is null, using 2nd backup
                mAlert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }

        mRinger = RingtoneManager.getRingtone(getApplicationContext(), mAlert);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mRecentActionArray == null) {
            mRecentActions = new ArrayDeque<Integer>(0);
        } else for (int action : mRecentActionArray) {
            mRecentActions.push(action);
        }

        loadSettings();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putLong("mTimeRemaining", mTimeRemaining);
        savedInstanceState.putLong("mPeriodLength", mPeriodLength);
        savedInstanceState.putInt("mScoreOne", mScoreOne);
        savedInstanceState.putInt("mScoreTwo", mScoreTwo);
        savedInstanceState.putDouble("mPriority", mPriority);
        savedInstanceState.putBoolean("mTimerRunning", mTimerRunning);
        savedInstanceState.putInt("mPeriodNumber", mPeriodNumber);
        savedInstanceState.putLong("mBreakLength", mBreakLength);
        savedInstanceState.putInt("mMode", mMode);
        savedInstanceState.putBoolean("mInPeriod", mInPeriod);
        savedInstanceState.putBoolean("mInBreak", mInBreak);
        savedInstanceState.putBoolean("mInPriority", mInPriority);
        savedInstanceState.putBoolean("mOneHasYellow", mOneHasYellow);
        savedInstanceState.putBoolean("mOneHasRed", mOneHasRed);
        savedInstanceState.putBoolean("mTwoHasRed", mTwoHasRed);
        savedInstanceState.putBoolean("mTwoHasYellow", mTwoHasYellow);
        mRecentActionArray = new int[mRecentActions.size()];
        for (int i = mRecentActions.size() - 1; i >= 0; i--)
            mRecentActionArray[i] = mRecentActions.pop();
        savedInstanceState.putIntArray("mRecentActionArray", mRecentActionArray);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.items, menu);
        mActionUndo = menu.findItem(R.id.action_undo);
        updateUndoButton();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    // METHODS FOR ALL TYPES
    private void updateViews() { // call all refresh methods (except updateTimer)
        updatePeriod();
        updateScores();
        updateCardIndicators();
        updateTimer(mTimeRemaining);
    }

    public void updateAll() {
        updateViews();
        updateUndoButton();
    }

    public void resetAll(MenuItem menuItem) { // onClick for action_reset
        resetScores();
        if (mTimeRemaining != mPeriodLength) resetTime();
        resetCards();
        resetPeriod();
        resetRecentActions();
        updateAll();
        if(mToast != null) mToast.cancel();
    }


    // METHODS FOR TIME & PERIODS
    public void countDown(View v) { // onClick method for mTimer
        mRinger.stop();
        mVibrator.cancel();
        if (mTimerRunning) pauseTimer();
        else startTimer(mTimeRemaining);
    }

    private void updateTimer(long millisUntilFinished) {
        long minutes = millisUntilFinished / 60000;
        long seconds = millisUntilFinished / 1000 - minutes * 60;
        long milliseconds = millisUntilFinished % 1000 / 10;
        String timeStr = String.format("%1d:%02d.%02d", minutes,
                seconds, milliseconds);
        mTimer.setText(timeStr);
    }

    private void startTimer(long time) {
        mTimer.clearAnimation();
        mTimer.setTextColor(Color.WHITE);
        mVibrator.vibrate(mStartVibrationPattern, -1);
        mCountDownTimer = new CountDownTimer(time, 10) {
            public void onTick(long millisUntilFinished) {
                updateTimer(millisUntilFinished);
                mTimeRemaining = millisUntilFinished;
            }

            public void onFinish() {
                endPeriod();
            }
        }.start();
        mTimerRunning = true;
    }

    private void pauseTimer() {
        mRinger.stop();
        mVibrator.cancel();
        mVibrator.vibrate(100);
        if (mTimerRunning) {
            mCountDownTimer.cancel();
            mTimerRunning = false;
            mTimer.startAnimation(mBlink);
        }
    }

    private void endPeriod() {
        mTimer.setText("Done!");
        mTimer.setTextColor(Color.argb(180, 255, 20, 20));
        mTimer.setAnimation(mBlink);
        mVibrator.vibrate(mEndVibrationPattern, -1);
        mRinger.play();
        mTimerRunning = false;
        if (mInPriority) {
            if (mScoreOne == mScoreTwo)
                switch (mPriority) {
                    case (0)
                }
        } else {
            if (mPeriodNumber < mMaxPeriods) {
                mInPeriod = !mInPeriod;
                mInBreak = !mInBreak;
                if (mInPeriod) {
                    mTimeRemaining = mPeriodLength;
                    nextPeriod();
                } else if (mInBreak)
                    mTimeRemaining = mBreakLength;
            } else if(mScoreOne == mScoreTwo) {
                mPriority = (int) determinePriority();
                mInPriority = true;
                startPriority();
            }
        }
    }

    private void nextPeriod() {
        mPeriodNumber++;
        updatePeriod();
    }

    private void updatePeriod() {
        mPeriodView.setText(getResources().getString(R.string.period) + " " + mPeriodNumber);
    }

    public double determinePriority() {
        double rand  = Math.random() * 100 + 1;
        return rand % 2;
    }

    private void startPriority() {
        startTimer(60 * 1000);
    }

    private void resetPeriod() {
        mPeriodNumber = 1;
    }

    private void resetTime() {
        mTimeRemaining = mPeriodLength;
        mTimer.setText("" + mTimeRemaining);
        updateTimer(mTimeRemaining);
        mTimerRunning = false;
        mRinger.stop();
        mVibrator.cancel();
        mTimer.clearAnimation();
        mPeriodNumber = 1;
    }

    // METHODS FOR SCORES
    private void updateScores() {
        mScoreOneView.setText("" + mScoreOne);
        mScoreTwoView.setText("" + mScoreTwo);
    }

    public void addScore(View view) { //onClick for score textViews
        switch (view.getId()) {
            case R.id.scoreOne:
                mScoreOne++;
                mRecentActions.push(0);
                showToast(getResources().getString(R.string.gave), getResources().getString(R.string.touch),
                        getResources().getString(R.string.left));
                break;
            case R.id.scoreTwo:
                mScoreTwo++;
                mRecentActions.push(1);
                showToast(getResources().getString(R.string.gave), getResources().getString(R.string.touch),
                        getResources().getString(R.string.right));
                break;
            case R.id.doubleTouchButton:
                mScoreOne++;
                mScoreTwo++;
                mRecentActions.push(2);
                showToast(getResources().getString(R.string.gave),getResources().getString(R.string.double_toast),
                        getResources().getString(R.string.touch));
                break;
        }
        pauseTimer();
        updateAll();
    }

    private void subScore(int fencer) {
        switch (fencer) {
            case 0:
                mScoreOne--;
                break;
            case 1:
                mScoreTwo--;
                break;
            case 2:
                mScoreOne--;
                mScoreTwo--;
                break;
        }
    }

    private void resetScores() {
        mScoreOne = 0;
        if (mTimerRunning)
            mCountDownTimer.cancel();
        mScoreTwo = 0;
        updateScores();
    }

    // METHODS FOR CARDS
    public void onDialogClick(DialogFragment dialogFragment, int fencer, int cardType) {
        giveCard(fencer, cardType);
    }

    public void giveCard(int fencer, int cardType) { // logic for assigning cards
        Intent cardIntent = new Intent(this, CardActivity.class);
        boolean alreadyHadYellow = false;
        switch (fencer) {
            case (0):
                switch (cardType) {
                    case (0):
                        if (mOneHasYellow) {
                            alreadyHadYellow = true;
                            mRecentActions.push(4);
                        }
                        mOneHasYellow = true;
                        if (!alreadyHadYellow) {
                            mRecentActions.push(3);
                            showToast(getResources().getString(R.string.gave), getResources().getString(R.string.yellow),
                                    getResources().getString(R.string.card), getResources().getString(R.string.left));
                            break;
                        }
                    case (1):
                        mScoreTwo++;
                        mOneHasRed = true;
                        mRecentActions.push(4);
                        showToast(getResources().getString(R.string.gave), getResources().getString(R.string.red),
                                getResources().getString(R.string.card), getResources().getString(R.string.left));
                        break;
                }
                if (mOneHasRed) cardIntent.putExtra("red", true);
                startActivity(cardIntent);
                break;
            case (1):
                switch (cardType) {
                    case (0):
                        if (mTwoHasRed) {
                            alreadyHadYellow = true;
                            mRecentActions.push(6);
                        }
                        mTwoHasRed = true;
                        if (!alreadyHadYellow) {
                            mRecentActions.push(5);
                            showToast(getResources().getString(R.string.gave), getResources().getString(R.string.yellow),
                                    getResources().getString(R.string.card), getResources().getString(R.string.right));
                            break;
                        }
                    case (1):
                        mScoreOne++;
                        mTwoHasYellow = true;
                        mRecentActions.push(6);
                        showToast(getResources().getString(R.string.gave), getResources().getString(R.string.red),
                                getResources().getString(R.string.card), getResources().getString(R.string.right));
                        break;
                }
                if (mTwoHasYellow) cardIntent.putExtra("red", true);
                startActivity(cardIntent); // launch card activity
        }
        updateAll();
        pauseTimer();
    }

    private void resetCards() { // remove all penalties and clear indicator views
        mOneHasYellow = mOneHasRed = mTwoHasYellow = mTwoHasRed = false;
        updateCardIndicators();
    }

    private void updateCardIndicators() { // update penalty indicator views
        if (mOneHasYellow) mYellowIndicatorLeft.setVisibility(View.VISIBLE);
        else mYellowIndicatorLeft.setVisibility(View.INVISIBLE);
        if (mOneHasRed) mRedIndicatorLeft.setVisibility(View.VISIBLE);
        else mRedIndicatorLeft.setVisibility(View.INVISIBLE);
        if (mTwoHasRed) mYellowIndicatorRight.setVisibility(View.VISIBLE);
        else mYellowIndicatorRight.setVisibility(View.INVISIBLE);
        if (mTwoHasYellow) mRedIndicatorRight.setVisibility(View.VISIBLE);
        else mRedIndicatorRight.setVisibility(View.INVISIBLE);
    }

    public void showCardDialog(View view) { // onClick for yellowCardButton & redCardButton
        FragmentManager man = this.getFragmentManager();
//        CardAlertFragment dialog = new CardAlertFragment(view);
        CardAlertFragment dialog = CardAlertFragment.newInstance(view);
        dialog.show(man, "Penalty Card");
    }

    // METHODS FOR RECENT ACTIONS & UNDO
    // 0 = touch left, 1 = touch right, 2 = double touch,
    // 3 = yellow to left, 4 = red to left, 5 = yellow to right, 6 = red to right
    private void resetRecentActions() {
        mRecentActions = new ArrayDeque<Integer>(0);
    }

    private void undoAction(Integer action) {
        switch (action) {
            case 0:
                subScore(0);
                showToast(getResources().getString(R.string.undid), getResources().getString(R.string.touch),
                        getResources().getString(R.string.left));
                break;
            case 1:
                subScore(1);
                showToast(getResources().getString(R.string.undid), getResources().getString(R.string.touch),
                        getResources().getString(R.string.right));
                break;
            case 2:
                subScore(2);
                showToast(getResources().getString(R.string.undid), getResources().getString(R.string.double_toast),
                        getResources().getString(R.string.touch));
                break;
            case 3:
                mOneHasYellow = false;
                showToast(getResources().getString(R.string.undid), getResources().getString(R.string.yellow),
                        getResources().getString(R.string.card), getResources().getString(R.string.left));
                break;
            case 4:
                mOneHasRed = false;
                subScore(1);
                showToast(getResources().getString(R.string.undid), getResources().getString(R.string.red),
                        getResources().getString(R.string.card), getResources().getString(R.string.left));
                break;
            case 5:
                mTwoHasRed = false;
                showToast(getResources().getString(R.string.undid), getResources().getString(R.string.yellow),
                        getResources().getString(R.string.card), getResources().getString(R.string.right));
                break;
            case 6:
                mTwoHasYellow = false;
                subScore(0);
                showToast(getResources().getString(R.string.undid), getResources().getString(R.string.red),
                        getResources().getString(R.string.card), getResources().getString(R.string.right));
                break;
        }
        mRecentActions.pop();
        updateAll();
    }

    public void undoMostRecent(MenuItem item) {
        undoAction(mRecentActions.peek());
    }

    private void updateUndoButton() {
        if (mRecentActions.isEmpty()) mActionUndo.setVisible(false);
        else mActionUndo.setVisible(true);
    }

    public void openSettings(MenuItem item) {
        Intent settingsIntent = new Intent(this, Settings.class);
        startActivity(settingsIntent);
    }

    private void loadSettings() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mMode = Integer.parseInt(mSharedPreferences.getString("pref_mode", "5"));
        switch (mMode) {
            case (5):
                mMaxPeriods = 1;
                break;
            case (15):
                mMaxPeriods = 3;
                break;
        }
    }

    private void showToast(String verb, String noun, String recipient) {
        Context context = getApplicationContext();
        CharSequence text = verb + " " + noun + " " + recipient + ".";
        int duration = Toast.LENGTH_SHORT;

        mToast = Toast.makeText(context, text, duration);
        mToast.show();
    }

    private void showToast(String verb, String color, String noun, String recipient) {
        Context context = getApplicationContext();
        CharSequence text = verb + " " + color + " " + noun + " " + recipient + ".";
        int duration = Toast.LENGTH_SHORT;

        mToast = Toast.makeText(context, text, duration);
        mToast.show();
    }

}