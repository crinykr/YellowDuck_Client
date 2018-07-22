package crinysoft.yellowduck;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Locale;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //final int CON_TIMEOUT_SEC = 30;
    final int CON_TIMEOUT_SEC = 5;

    ImageView ivDuck;
    TextView tvConStatus;
    TextView tvConIcon;

    Thread conUiThread;
    Thread conTimeoutThread;
    Thread nextActivityThread;
    Animation conAni;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        ivDuck = (ImageView) findViewById(R.id.ivDuck);
        tvConStatus = (TextView) findViewById(R.id.tvConStatus);
        tvConIcon = (TextView) findViewById(R.id.tvConIcon);

        ivDuck.setOnClickListener(this);

        AnimationSet aniSet = (AnimationSet) AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate);
        for (Animation a : aniSet.getAnimations()) {
            if (a != null) {
                conAni = a;
                break;
            }
        }

        conAni.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                if (MyApplication.getStatus() == MyApplication.STATUS_WAITING) {
                    ivDuck.clearAnimation();
                    ivDuck.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake_big));
                } else if (MyApplication.getStatus() == MyApplication.STATUS_CONNECTED) {
                    Log.e("yellowduck-ui-dbg", "onAnimationRepeat");

                    ivDuck.clearAnimation();

                    nextActivityThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(700);
                                startActivity(new Intent(MainActivity.this, TalkActivity.class));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    nextActivityThread.start();
                }
            }
        });

        MyApplication.changeStatus(MyApplication.STATUS_WAITING);
        tvConStatus.setText(R.string.status_wait);

        //startActivity(new Intent(MainActivity.this, TalkActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (MyApplication.getStatus() != MyApplication.STATUS_WAITING) {
            MyApplication.changeStatus(MyApplication.STATUS_WAITING);
            tvConStatus.setText(R.string.status_wait_disconnected);
            ivDuck.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake));
        }

        TcpUtil.getInstance().setHandler(uiHandler);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        TcpUtil.getInstance().CloseConnection(false);

        ivDuck.clearAnimation();

        if (conUiThread != null)
            conUiThread.interrupt();

        if (conTimeoutThread != null)
            conTimeoutThread.interrupt();

        if (nextActivityThread != null)
            nextActivityThread.interrupt();

        finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.ivDuck) {
            if (MyApplication.getStatus() == MyApplication.STATUS_WAITING) {
                MyApplication.changeStatus(MyApplication.STATUS_CONNECTING);
                tvConStatus.setText(R.string.status_connecting);
                ivDuck.startAnimation(conAni);
                conUiThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                String[] chars = {"★", "♥", "♣", "♠", "♬"};
                                for (final String c : chars) {
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            tvConIcon.setText(c);
                                            Log.e("yellowduck-ui", "" + c);
                                        }
                                    });
                                    Thread.sleep(500);
                                }
                            } catch (InterruptedException e) {
                                Log.e("yellowduck-ui", "MainActivity-run : conUiThread Interrupted");
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        tvConIcon.setText("");
                                    }
                                });
                                break;
                            }
                        }
                    }
                });
                conUiThread.start();

                TcpUtil.getInstance().startWorker();
                conTimeoutThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(CON_TIMEOUT_SEC * 1000);
                            if (MyApplication.getStatus() == MyApplication.STATUS_CONNECTING) {
                                MyApplication.changeStatus(MyApplication.STATUS_WAITING);
                                tvConStatus.setText(R.string.status_wait_again);
                                TcpUtil.getInstance().CloseConnection(false);
                                if (conUiThread != null)
                                    conUiThread.interrupt();
                            }
                        } catch (InterruptedException e) {
                            Log.e("yellowduck-ui", "MainActivity-run : connectingTimeoutThread Interrupted");
                        }
                    }
                });
                conTimeoutThread.start();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TcpUtil.HANDLER_ID_RESV_HELLO) {
                Log.e("yellowduck-ui", "MainActivity-handleMessage : TcpUtil.HANDLER_ID_RESV_HELLO");
                Locale l = getApplicationContext().getResources().getConfiguration().locale;
                String userInfo = l.getDisplayCountry(Locale.ENGLISH) + "|" + l.getDisplayLanguage(Locale.ENGLISH);
                TcpUtil.getInstance().SendMessage(userInfo);
            } else if (msg.what == TcpUtil.HANDLER_ID_RESV_MSG) {
                MyApplication.changeStatus(MyApplication.STATUS_CONNECTED);
                tvConStatus.setText(R.string.status_connect_success);

                Log.e("yellowduck-ui", "MainActivity-handleMessage : TcpUtil.HANDLER_ID_RESV_MSG (Country/Language Info)");
                String text = (String) msg.obj;

                StringTokenizer tokens = new StringTokenizer(text);
                MyApplication.hisCountry = tokens.nextToken("|");
                MyApplication.hisLanguage = tokens.nextToken("|");

                if (conUiThread != null)
                    conUiThread.interrupt();

                if (conTimeoutThread != null)
                    conTimeoutThread.interrupt();
            } else if (msg.what == TcpUtil.HANDLER_ID_DISCONNECTED) {
                Log.e("yellowduck-ui-dbg", "HANDLER_ID_DISCONNECTED");
                MyApplication.changeStatus(MyApplication.STATUS_WAITING);
                tvConStatus.setText(R.string.status_wait_again);

                if (conUiThread != null)
                    conUiThread.interrupt();

                if (conTimeoutThread != null)
                    conTimeoutThread.interrupt();

                if (nextActivityThread != null)
                    nextActivityThread.interrupt();
            }

        }
    };
}