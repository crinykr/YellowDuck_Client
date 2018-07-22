package crinysoft.yellowduck;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Locale;
import java.util.StringTokenizer;

/*
 * Todo List
 * L) 모든 Padding/Margin/Weight 를 동적 재분배 할지 결정
 * L) 모든 레이아웃 배경색 명시 지정
 * L) Layout 중 Match Content 해야하는 것 찾기 (텍스트박스, 메시지 박스 등)
 * L) 키보드 열리면 텍스트박스 찌그러짐
 * L) 글로벌 스트링 리소스 정리
 * B) 접속 후 터킹액티비티로 넘어갈대 애니메이션+특수문자효과 꼬이는 경우 있음
 * SSL 적용
 * 오리 교체
 * 네트워크 도메인 통해 접속하는 부분 재검증
 * finish()로 완전한 종료가 안됨. 액티비티 히스토리에서 완전한 제거 가능한 방법?
 */

public class MyApplication extends Application {
    public static final int STATUS_WAITING = 1;
    public static final int STATUS_CONNECTING = 2;
    public static final int STATUS_CONNECTED = 3;

    static Context context;
    static int curStatus;

    public static String hisCountry;
    public static String hisLanguage;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext() {
        return context;
    }

    public static void changeStatus(int status) {
        String statusStr[] = {"(empty)", "WAITING", "CONNECTING", "CONNECTED"};
        Log.e("yellowduck-ui", "MyApplication-changeStatus : state changing (" + statusStr[curStatus] + ") → (" + statusStr[status] + ")");
        curStatus = status;
    }

    public static int getStatus() {
        return curStatus;
    }
}
