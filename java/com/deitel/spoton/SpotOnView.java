// SpotOnView.java
// View that displays and manages the game
package com.deitel.spoton;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SpotOnView extends View {
    // constant for accessing the high score in SharedPreference
    private static final String HIGH_SCORE = "HIGH_SCORE";
    private SharedPreferences preferences; // stores the high score

    // variables for managing the game
    private int spotsTouched; // number of spots touched
    private int score; // current score
    private int level; // current level
    private int viewWidth; // stores the width of this View
    private int viewHeight; // stores the height of this view
    private long animationTime; // how long each spot remains on the screen
    private boolean gameOver; // whether the game has ended
    private boolean gamePaused; // whether the game has ended
    private boolean dialogDisplayed; // whether the game has ended
    private int highScore; // the game's all time high score

    // collections of spots (ImageViews) and Animators
    private final Queue<ImageView> spots =
            new ConcurrentLinkedQueue<ImageView>();
    private final Queue<Animator> animators =
            new ConcurrentLinkedQueue<Animator>();

    private TextView timeTextView; // displays high score
    private TextView levelTextView; // displays current level
    private TextView totalTimeTextView;
    private LinearLayout livesLinearLayout; // displays lives remaining
    private RelativeLayout relativeLayout; // displays spots
    private Resources resources; // used to load resources
    private LayoutInflater layoutInflater; // used to inflate GUIs

    // time in milliseconds for spot and touched spot animations
    private static final int INITIAL_ANIMATION_DURATION = 15000;
    private static final Random random = new Random(); // for random coords
    private static final int SPOT_DIAMETER = 100; // initial spot size
    private static final float SCALE_X = 0.5f; // end animation x scale
    private static final float SCALE_Y = 0.5f; // end animation y scale
    private static final int INITIAL_SPOTS = 10; // initial # of spots
    private static final int SPOT_DELAY = 1000; // delay in milliseconds
    private static final int LIVES = 3; // start with 3 lives
    private static final int MAX_LIVES = 7; // maximum # of total lives
    private static final int NEW_LEVEL = 10; // spots to reach new level
    private Handler spotHandler; // adds new spots to the game
    private Handler timeHandler;

    // sound IDs, constants and variables for the game's sounds
    private static final int HIT_SOUND_ID = 31;
    private static final int MISS_SOUND_ID = 32;
    private static final int DISAPPEAR_SOUND_ID = 33;
    private static final int UHOH_SOUND_ID = 34;
    private static final int APPLAUSE_SOUND_ID = 35;
    private static final int SOUND_PRIORITY = 1;
    private static final int SOUND_QUALITY = 100;
    private static final int MAX_STREAMS = 4;
    private SoundPool soundPool; // plays sound effects
    private int volume; // sound effect volume
    private Map<Integer, Integer> soundMap; // maps ID to soundpool
    private int numOn = 1;
    private int numGoal = 10;
    private int numSpotsMade = 1;
    private int idChanger = 100;
    private int timeLimit = 0;
    private int totalTime = 0;
    private Map<Integer, Integer> numMap;


    // constructs a new SpotOnView
    public SpotOnView(Context context, SharedPreferences sharedPreferences,
                      RelativeLayout parentLayout) {
        super(context);

        // load the high score
        preferences = sharedPreferences;
        highScore = preferences.getInt(HIGH_SCORE, 0);

        // save Resources for loading external values
        resources = context.getResources();

        // save LayoutInflater
        layoutInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        // get references to various GUI components
        relativeLayout = parentLayout;
        livesLinearLayout = (LinearLayout) relativeLayout.findViewById(
                R.id.lifeLinearLayout);
        timeTextView = (TextView) relativeLayout.findViewById(
                R.id.timeTextView);
        levelTextView = (TextView) relativeLayout.findViewById(
                R.id.levelTextView);
        totalTimeTextView = (TextView) relativeLayout.findViewById(
                R.id.totalTimeTextView);

        spotHandler = new Handler();// used to add spots when game starts
        timeHandler = new Handler();

        numMap = new HashMap<Integer, Integer>();
        numMap.put(1,R.drawable.one);
        numMap.put(2,R.drawable.two);
        numMap.put(3,R.drawable.three);
        numMap.put(4,R.drawable.four);
        numMap.put(5,R.drawable.five);
        numMap.put(6,R.drawable.six);
        numMap.put(7,R.drawable.seven);
        numMap.put(8,R.drawable.eight);
        numMap.put(9,R.drawable.nine);
        numMap.put(10,R.drawable.ten);
        numMap.put(11,R.drawable.eleven);
        numMap.put(12,R.drawable.twelve);
        numMap.put(13,R.drawable.thirteen);
        numMap.put(14,R.drawable.fourteen);
        numMap.put(15,R.drawable.fifteen);
        numMap.put(16,R.drawable.sixteen);
        numMap.put(17,R.drawable.seventeen);
        numMap.put(18,R.drawable.eighteen);
        numMap.put(19,R.drawable.nineteen);
        numMap.put(20,R.drawable.twenty);
        numMap.put(21,R.drawable.twentyone);
        numMap.put(22,R.drawable.twentytwo);
        numMap.put(23,R.drawable.twentythree);
        numMap.put(24,R.drawable.twentyfour);
        numMap.put(25,R.drawable.twentyfive);
        numMap.put(26,R.drawable.twentysix);
        numMap.put(27,R.drawable.twentyseven);
        numMap.put(28,R.drawable.twentyeight);
        numMap.put(29,R.drawable.twentynine);
        numMap.put(30,R.drawable.thirty);
    } // end SpotOnView constructor

    // store SpotOnView's width/height
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        viewWidth = width; // save the new width
        viewHeight = height; // save the new height
    } // end method onSizeChanged

    // called by the SpotOn Activity when it receives a call to onPause
    public void pause() {
        gamePaused = true;
        soundPool.release(); // release audio resources
        soundPool = null;
        cancelAnimations(); // cancel all outstanding animations
    } // end method pause

    // cancel animations and remove ImageViews representing spots
    private void cancelAnimations() {
        // cancel remaining animations
        for (Animator animator : animators)
            animator.cancel();

        // remove remaining spots from the screen
        for (ImageView view : spots)
            relativeLayout.removeView(view);

        spotHandler.removeCallbacks(addSpotRunnable);
        animators.clear();
        spots.clear();
    } // end method cancelAnimations

    // called by the SpotOn Activity when it receives a call to onResume
    public void resume(Context context) {
        gamePaused = false;
        initializeSoundEffects(context); // initialize app's SoundPool

        if (!dialogDisplayed)
            resetGame(); // start the game
    } // end method resume

    // start a new game
    public void resetGame() {
        spots.clear(); // empty the List of spots
        animators.clear(); // empty the List of Animators
        livesLinearLayout.removeAllViews(); // clear old lives from screen

        animationTime = INITIAL_ANIMATION_DURATION; // init animation length
        spotsTouched = 0; // reset the number of spots touched
        level = 1; // reset the level
        numOn = 1;
        numGoal = 10;
        numSpotsMade = 1;
        idChanger = 100;
        timeLimit = 60;
        totalTime = 0;
        gameOver = false; // the game is not over
        displayScores(); // display scores and level

        // add lives


        // add INITIAL_SPOTS new spots at SPOT_DELAY time intervals in ms
        for (int i = 1; i <= INITIAL_SPOTS; ++i)
            spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);

        timeHandler.post(timeCountRunnable);

    } // end method resetGame

    // create the app's SoundPool for playing game audio
    private void initializeSoundEffects(Context context) {
        // initialize SoundPool to play the app's three sound effects
        soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .build();

        // set sound effect volume
        AudioManager manager =
                (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // create sound map
        soundMap = new HashMap<Integer, Integer>(); // create new HashMap

        // add each sound effect to the SoundPool

        soundMap.put(1,
                soundPool.load(context, R.raw.onevoice, SOUND_PRIORITY));
        soundMap.put(2,
                soundPool.load(context, R.raw.twovoice, SOUND_PRIORITY));
        soundMap.put(3,
                soundPool.load(context, R.raw.threevoice, SOUND_PRIORITY));
        soundMap.put(4,
                soundPool.load(context, R.raw.fourvoice, SOUND_PRIORITY));
        soundMap.put(5,
                soundPool.load(context, R.raw.fivevoice, SOUND_PRIORITY));
        soundMap.put(6,
                soundPool.load(context, R.raw.sixvoice, SOUND_PRIORITY));
        soundMap.put(7,
                soundPool.load(context, R.raw.sevenvoice, SOUND_PRIORITY));
        soundMap.put(8,
                soundPool.load(context, R.raw.eightvoice, SOUND_PRIORITY));
        soundMap.put(9,
                soundPool.load(context, R.raw.ninevoice, SOUND_PRIORITY));
        soundMap.put(10,
                soundPool.load(context, R.raw.tenvoice, SOUND_PRIORITY));
        soundMap.put(11,
                soundPool.load(context, R.raw.elevenvoice, SOUND_PRIORITY));
        soundMap.put(12,
                soundPool.load(context, R.raw.twelvevoice, SOUND_PRIORITY));
        soundMap.put(13,
                soundPool.load(context, R.raw.thirteenvoice, SOUND_PRIORITY));
        soundMap.put(14,
                soundPool.load(context, R.raw.fourteenvoice, SOUND_PRIORITY));
        soundMap.put(15,
                soundPool.load(context, R.raw.fifteenvoice, SOUND_PRIORITY));
        soundMap.put(16,
                soundPool.load(context, R.raw.sixteenvoice, SOUND_PRIORITY));
        soundMap.put(17,
                soundPool.load(context, R.raw.seventeenvoice, SOUND_PRIORITY));
        soundMap.put(18,
                soundPool.load(context, R.raw.eighteenvoice, SOUND_PRIORITY));
        soundMap.put(19,
                soundPool.load(context, R.raw.nineteenvoice, SOUND_PRIORITY));
        soundMap.put(20,
                soundPool.load(context, R.raw.twentyvoice, SOUND_PRIORITY));
        soundMap.put(21,
                soundPool.load(context, R.raw.twentyonevoice, SOUND_PRIORITY));
        soundMap.put(22,
                soundPool.load(context, R.raw.twentytwovoice, SOUND_PRIORITY));
        soundMap.put(23,
                soundPool.load(context, R.raw.twentythreevoice, SOUND_PRIORITY));
        soundMap.put(24,
                soundPool.load(context, R.raw.twentyfourvoice, SOUND_PRIORITY));
        soundMap.put(25,
                soundPool.load(context, R.raw.twentyfivevoice, SOUND_PRIORITY));
        soundMap.put(26,
                soundPool.load(context, R.raw.twentysixvoice, SOUND_PRIORITY));
        soundMap.put(27,
                soundPool.load(context, R.raw.twentysevenvoice, SOUND_PRIORITY));
        soundMap.put(28,
                soundPool.load(context, R.raw.twentyeightvoice, SOUND_PRIORITY));
        soundMap.put(29,
                soundPool.load(context, R.raw.twentyninevoice, SOUND_PRIORITY));
        soundMap.put(30,
                soundPool.load(context, R.raw.thirtyvoice, SOUND_PRIORITY));

        soundMap.put(HIT_SOUND_ID,
                soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
        soundMap.put(MISS_SOUND_ID,
                soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
        soundMap.put(DISAPPEAR_SOUND_ID,
                soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));
        soundMap.put(UHOH_SOUND_ID,
                soundPool.load(context, R.raw.uhoh, SOUND_PRIORITY));
        soundMap.put(APPLAUSE_SOUND_ID,
                soundPool.load(context, R.raw.applause, SOUND_PRIORITY));


    } // end method initializeSoundEffect

    // display scores and level
    private void displayScores() {
        timeTextView.setText("Time: " + timeLimit );
        levelTextView.setText(
                resources.getString(R.string.level) + " " + level);
        totalTimeTextView.setText("Total Time: " + totalTime);
    } // end function displayScores

    // Runnable used to add new spots to the game at the start
    private Runnable addSpotRunnable = new Runnable() {
        public void run() {
            addNewSpot(); // add a new spot to the game
        } // end method run
    }; // end Runnable

    private Runnable timeCountRunnable = new Runnable(){
        public void run(){
            if (!gameOver){
                timeLimit -= 1;
                totalTime += 1;
                if (timeLimit < 0){
                    Builder dialogBuilder = new AlertDialog.Builder(getContext());
                    dialogBuilder.setTitle(R.string.game_over);
                    dialogBuilder.setMessage("You lose! Would you like to play again?");
                    dialogBuilder.setPositiveButton(R.string.reset_game,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    displayScores(); // ensure that score is up to date
                                    dialogDisplayed = false;
                                    resetGame(); // start a new game
                                } // end method onClick
                            } // end DialogInterface
                    ); // end call to dialogBuilder.setPositiveButton
                    dialogDisplayed = true;
                    dialogBuilder.show(); // display the reset game dialog
                }
                else{
                    displayScores();
                    timeHandler.postDelayed(timeCountRunnable, 1000);
                }
            }
        }
    };

    // adds a new spot at a random location and starts its animation
    public void addNewSpot() {
        // choose two random coordinates for the starting and ending points
        int x = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y = random.nextInt(viewHeight - SPOT_DIAMETER);
        int x2 = random.nextInt(viewWidth - SPOT_DIAMETER);
        int y2 = random.nextInt(viewHeight - SPOT_DIAMETER);

        // create new spot
        final ImageView spot =
                (ImageView) layoutInflater.inflate(R.layout.untouched, null);
        spot.setId(numSpotsMade);
        spots.add(spot); // add the new spot to our list of spots
        spot.setLayoutParams(new RelativeLayout.LayoutParams(
                SPOT_DIAMETER, SPOT_DIAMETER));
        spot.setImageResource(numMap.get(numSpotsMade));
        if(numSpotsMade == 30){
            numSpotsMade = 1;
        }
        else{
            numSpotsMade += 1;
        }
        spot.setX(x); // set spot's starting x location
        spot.setY(y); // set spot's starting y location
        spot.setOnClickListener( // listens for spot being clicked
                new OnClickListener() {
                    public void onClick(View v) {
                        touchedSpot(spot); // handle touched spot
                    } // end method onClick
                } // end OnClickListener
        ); // end call to setOnClickListener
        relativeLayout.addView(spot); // add spot to the screen

        // configure and start spot's animation
        spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y)
                .setDuration(animationTime).setListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        animators.add(animation); // save for possible cancel
                    } // end method onAnimationStart

                    public void onAnimationEnd(Animator animation) {
                        animators.remove(animation); // animation done, remove

                        if (!gamePaused && spots.contains(spot)) // not touched
                        {
                            missedSpot(spot); // lose a life
                        } // end if
                    } // end method onAnimationEnd
                } // end AnimatorListenerAdapter
        ); // end call to setListener
    } // end addNewSpot method

    // called when the user touches the screen, but not a spot
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // play the missed sound
        if (soundPool != null)
            soundPool.play(soundMap.get(MISS_SOUND_ID), volume, volume,
                    SOUND_PRIORITY, 0, 1f);
        displayScores(); // update scores/level on screen
        return true;
    } // end method onTouchEvent

    // called when a spot is touched
    private void touchedSpot(ImageView spot) {
        if(spot.getId() != numOn){
            soundPool.play(soundMap.get(UHOH_SOUND_ID), volume, volume, SOUND_PRIORITY, 0, 1f);
            return;
        }

        spot.setId(idChanger);
        idChanger += 1;
        relativeLayout.removeView(spot); // remove touched spot from screen
        spots.remove(spot); // remove old spot from list

        if (soundPool != null)
            soundPool.play(soundMap.get(numOn), volume, volume,
                    SOUND_PRIORITY, 0, 1f);
        numOn += 1;
        if (numOn > numGoal) {
            switch (level){
                case 1:
                    timeLimit = 55;
                    break;
                case 2:
                    timeLimit = 50;
                    break;
            }
            if(level == 3){
                timeLimit = 45;
                ++level;
                numGoal = random.nextInt(21) + 10;
                numSpotsMade = numGoal - 9;
                numOn = numGoal - 9;
                for (int i = 1; i <= INITIAL_SPOTS; ++i)
                    spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);
            }
            else if(level == 4){
                gameOver = true;
                soundPool.play(soundMap.get(APPLAUSE_SOUND_ID), volume, volume, SOUND_PRIORITY, 0, 1f);
                Builder dialogBuilder = new AlertDialog.Builder(getContext());
                dialogBuilder.setTitle(R.string.game_over);
                dialogBuilder.setMessage("You win! Would you like to play again?");
                dialogBuilder.setPositiveButton(R.string.reset_game,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                displayScores(); // ensure that score is up to date
                                dialogDisplayed = false;
                                resetGame(); // start a new game
                            } // end method onClick
                        } // end DialogInterface
                ); // end call to dialogBuilder.setPositiveButton
                dialogDisplayed = true;
                dialogBuilder.show(); // display the reset game dialog
            }
            else{
                numGoal += 10;
                ++level;
                for (int i = 1; i <= INITIAL_SPOTS; ++i)
                    spotHandler.postDelayed(addSpotRunnable, i * SPOT_DELAY);
            }
            displayScores();
        } // end if

    } // end method touchedSpot

    // called when a spot finishes its animation without being touched
    public void missedSpot(ImageView spot) {
        if (gameOver) // if the game is already over, exit
            return;

        // if the game has been lost
        // end else
    } // end method missedSpot
} // end class SpotOnView


