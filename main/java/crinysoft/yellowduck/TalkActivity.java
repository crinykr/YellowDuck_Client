package crinysoft.yellowduck;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class TalkActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView ivBack;
    TextView tvTalkerInfo;

    ListView listView;
    ArrayList<ListViewItem> listData;
    ListViewAdapter adapter;

    EditText etText;
    ImageView ivSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_talk);

        ivBack = (ImageView) findViewById(R.id.ivBack);
        tvTalkerInfo = (TextView) findViewById(R.id.tvTalkerInfo);
        listView = (ListView) findViewById(R.id.lvTalkList);
        etText = (EditText) findViewById(R.id.etMsg);
        ivSend = (ImageView) findViewById(R.id.ivSend);

        ivBack.setOnClickListener(this);
        ivSend.setOnClickListener(this);

        String talkerInfo = getResources().getString(R.string.disp_country) + ":" + MyApplication.hisCountry + " / " + getResources().getString(R.string.disp_language) + ":" + MyApplication.hisLanguage;
        tvTalkerInfo.setText(talkerInfo);

        listData = new ArrayList<>();
        adapter = new ListViewAdapter(this, listData);
        listView.setAdapter(adapter);

        etText.setHint(R.string.hint_text_writing);
        ivSend.setAlpha(0.3f);

        etText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    ivSend.setAlpha(1.0f);
                } else {
                    ivSend.setAlpha(0.3f);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        TcpUtil.getInstance().setHandler(uiHandler);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.ivSend) {
            String text = etText.getText().toString().trim();
            if (text.length() > 0)
                TcpUtil.getInstance().SendMessage(text);
            else
                ivSend.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake));
        } else if (view.getId() == R.id.ivBack) {
            MyApplication.changeStatus(MyApplication.STATUS_WAITING);

            TcpUtil.getInstance().CloseConnection(false);
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(etText.getWindowToken(), 0);
            super.onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        ivBack.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake));
    }

    @SuppressLint("HandlerLeak")
    Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TcpUtil.HANDLER_ID_SEND_MSG_SUCCESS) {
                String text = etText.getText().toString().trim();
                listData.add(new ListViewItem(ListViewItem.ITEM_TYPE_SEND, text));
                adapter.notifyDataSetChanged();

                etText.setText("");
                etText.setHint(R.string.hint_text_waiting);
                etText.setEnabled(false);
                ivSend.setAlpha(0.3f);
                ivSend.setEnabled(false);
            } else if (msg.what == TcpUtil.HANDLER_ID_RESV_MSG) {
                String text = (String) msg.obj;
                listData.add(new ListViewItem(ListViewItem.ITEM_TYPE_RESV, text));
                adapter.notifyDataSetChanged();

                etText.setHint(R.string.hint_text_writing);
                etText.setEnabled(true);
                ivSend.setEnabled(true);

                etText.requestFocus();
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(etText, 0);
            } else if (msg.what == TcpUtil.HANDLER_ID_DISCONNECTED) {
                Log.e("yellowduck-ui", "TalkActivity-handleMessage : TcpUtil.HANDLER_ID_RESV_BYE");
                MyApplication.changeStatus(MyApplication.STATUS_WAITING);

                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(etText.getWindowToken(), 0);
                TalkActivity.super.onBackPressed();
            }
        }
    };
}