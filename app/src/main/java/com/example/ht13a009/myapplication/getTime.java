package com.example.ht13a009.myapplication;

import android.app.Notification;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

class GetTime extends AsyncTask<Object, Void, String> {

    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> diasArr;
    private Context context;
    private String route;

    private int hour;
    private int minute;
    private int second;

    private boolean allButton = false;
    //通知時間
    private int noticeTime;

    // リストの数を確認する変数
    private int listLength = -1;
    private ArrayList<Integer> clickIdArr = new ArrayList<Integer>();
    // クリックしたリストのIDと同じものがclickIdArrにあるかどうかの判定する変数
    private boolean same = false;
    private boolean same2 = false;

    // 通知判定
    private boolean notice = false;

    private Runnable restTime = null;

    // パラメータ[1] - コンテキスト
    private Context mContext = null;
    // パラメータ[2] - UI制御用ハンドラ
    //private Handler mHandler = null;
    // パラメータ[3] - 完了処理として実行したいRunnable
    private Runnable finallyRunnable = null;
    // パラメータ[4] - 忍ケ丘に到着する時間を表示
    private boolean ShinobuArrivalTime;
    // パラメータ[5] - 忍ケ丘だけ表示
    private boolean ShinobuOnly;

    public GetTime(ArrayAdapter<String> adapter, Context applicationContext, String Route) {
        super();
        mAdapter = adapter;
        context = applicationContext;
        route = Route;
    }

    @Override
    protected String doInBackground(Object... objects) {
        String result = "";
        HttpURLConnection conn = null;
        URL url = (URL) objects[0];
        mContext = (Context) objects[1];
        //mHandler = (Handler) objects[2];
        finallyRunnable = (Runnable) objects[3];
        ShinobuArrivalTime = (Boolean) objects[4];
        ShinobuOnly = (Boolean) objects[5];

        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            //int resp = conn.getResponseCode();
            // respを使っていろいろ
            result = readIt(conn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return result;
    }

    public String readIt(InputStream stream) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    @Override
    protected void onPostExecute(final String resp) {

        final int notificationId = 001;

        final Handler mHandler = new Handler();

        // テキストオブジェクト
        restTime = new Runnable() {

            @Override
            public void run() {

                // System.out.println("noticetime : " + noticeTime);
                diasArr = new ArrayList<>();
                // 例: 08:30:00を08:30に変更するためのsplit用配列
                String[] time;

                try {

                    JSONObject json = new JSONObject(resp);

                    JSONArray datas = json.getJSONArray("Dia");

                    for (int i = 0; i < datas.length(); i++) {
                        JSONObject data = datas.getJSONObject(i);
                        time = data.getString("departureTime").split(":", 0);

                        // 時間差を計算するclass
                        String[] timeLag = new TimeLag(time).TimeCalculation();

                        hour = Integer.parseInt(timeLag[0]);
                        minute = Integer.parseInt(timeLag[1]);
                        second = Integer.parseInt(timeLag[2]);

                        // リストの時間と現在時刻の差が0のときdiasArrに追加しない
                        if ( hour == 0 && minute == 0 && second == 0) {
                            continue;
                        }

                        /*
                        System.out.println("第一歩notice: " + notice);
                        System.out.println("allButton: " + allButton);
                        */

                        // allのボタンが押された時
                        if (allButton) {
                            notice = true;
                        } else if (!clickIdArr.isEmpty()) {
                            // clickしたIdを通知
                            for (int j = 0; j < clickIdArr.size(); j++) {
                                for (int k = 0; k < data.length(); k++) {
                                    if (k == clickIdArr.get(j)) {
                                        notice = true;
                                    }
                                }
                            }
                        }

                        // System.out.println("notice : " + notice);

                        // trueなら通知する
                        if (notice) {

                            // System.out.println("noticeTime.hour: " + (noticeTime/60) + " noticeTime.minute: " + (noticeTime % 60) );

                            // ○○前に通知
                            if (hour == (noticeTime / 60) && minute == (noticeTime % 60) && second == 0) {
                            //if (hour == 7 && minute == 27 && second == 0) {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                                builder.setSmallIcon(R.mipmap.oecu_bus_background_transmission);
                                builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.oecu_bus));
                                // アイコンの背景色
                                builder.setColor(ContextCompat.getColor(context, R.color.colorPrimary));

                                if (Integer.parseInt(timeLag[0]) == 0) {
                                    builder.setContentTitle(route);
                                    builder.setContentText("バス発車まで残り" + minute + "分です．");
                                } else {
                                    builder.setContentTitle(route);
                                    builder.setContentText("バス発車まで残り" + hour + "時間です．");
                                }

                                // builder.setSubText("SubText");
                                // builder.setContentInfo("Info");
                                // builder.setWhen(14000000000001);
                                // 通知到着時に通知バーに表示(4.4まで,5.0から表示されない)
                                // builder.setTicker("Ticker");

                                // 通知時の音・バイブ・ライト
                                builder.setDefaults(Notification.DEFAULT_SOUND
                                        | Notification.DEFAULT_VIBRATE
                                        | Notification.DEFAULT_LIGHTS);

                                // タップするとキャンセル(消える)
                                builder.setAutoCancel(true);

                                NotificationManagerCompat manager = NotificationManagerCompat.from(context);
                                manager.notify(notificationId, builder.build());
                            }
                        }

                        // 通知の判定をリセット
                        notice = false;

                        if(ShinobuOnly){
                            if(data.getString("note").equals("忍ケ丘経由")) {
                                if (ShinobuArrivalTime) {
                                    //System.out.println("11111");
                                    diasArr.add(time[0] + ":" + time[1] + "  " + "@" + timeLag[0] + ":" + timeLag[1] + ":" + timeLag[2]
                                            + "  忍  " + "忍着 " + time[0] + ":" + String.valueOf((Integer.parseInt(time[1]) + 10)));
                                } else {
                                    //System.out.println("22222");
                                    diasArr.add(time[0] + ":" + time[1] + "  " + "@" + timeLag[0] + ":"
                                            + timeLag[1] + ":" + timeLag[2] + "  忍  ");
                                }
                            }
                        }else if(data.getString("note").equals("忍ケ丘経由")) {
                            if(ShinobuArrivalTime){
                                //System.out.println("33333");
                                diasArr.add(time[0] + ":" + time[1] + "  " + "@" + timeLag[0] + ":" + timeLag[1] + ":" + timeLag[2]
                                        + "  忍  " + "忍着 " + time[0] + ":" + String.valueOf((Integer.parseInt(time[1]) + 10)));
                            }else{
                                //System.out.println("44444");
                                diasArr.add(time[0] + ":" + time[1] + "  " + "@" + timeLag[0] + ":"
                                        + timeLag[1] + ":" + timeLag[2] + "  忍  ");
                            }
                        } else {
                            //System.out.println("55555");
                            diasArr.add(time[0] + ":" + time[1] + "  " + "@" + timeLag[0] + ":" + timeLag[1] + ":" + timeLag[2]);
                        }

                    }

                    /*
                    System.out.println("listLength: " + listLength);
                    System.out.println("diasArr.size(): " + diasArr.size());
                    System.out.println("clickIdArr.size():" + clickIdArr.size());
                    */

                    System.out.println("clickIdArr" + clickIdArr);

                    // リスト数が増減した時，クリックしたリストIDの番号を変更する
                    if (listLength == -1) {
                        listLength = diasArr.size();
                    } else if (listLength < diasArr.size()) {
                        listLength = diasArr.size();
                        clickIdArr.clear();
                    } else if (listLength > diasArr.size()) {
                        listLength = diasArr.size();
                        if (!clickIdArr.isEmpty()) {
                            for (int i = clickIdArr.size() - 1; i >= 0; i--) {
                                System.out.println("1回目 clickIdArr.get(" + i + "):" + clickIdArr.get(i));
                                clickIdArr.set(i, clickIdArr.get(i) - 1);
                                System.out.println("2回目 clickIdArr.get(" + i + "):" + clickIdArr.get(i));
                                if (clickIdArr.get(i) < 0) {
                                    clickIdArr.remove(i);
                                }
                            }
                        }
                    }

                    // diasArrが空の時
                    if (diasArr.isEmpty()) {
                        diasArr.add("本日の営業は終了しました");
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // アダプターでリストを表示
                mAdapter.clear();
                mAdapter.addAll(diasArr);;

                // 1秒ごとにrestTime関数を再起動
                mHandler.removeCallbacks(restTime);
                mHandler.postDelayed(restTime, 1000);

            }
        };

        if (finallyRunnable != null){
            mHandler.post(finallyRunnable);
        }

        // run関数を初めて呼び出す時に用いる
        mHandler.postDelayed(restTime, 1000);

    }

    // 全通知するか判定する関数
    public void setAllButton(boolean flag) {
        allButton = flag;
        if (!allButton) {
            clickIdArr.clear();
        }
    }

    // 通知時間
    public void setNoticeTime(int setMinute) {
        noticeTime = setMinute;
    }

    // クリックしたIDがclickIdArrにあったら削除，なかったら追加する関数
    public boolean setClickId(int id) {

        System.out.println("setClickId");

        same = false;
        for (int i = 0; i < clickIdArr.size(); i++) {
            if (id == clickIdArr.get(i)) {
                clickIdArr.remove(i);
                same = true;
            }
        }

        if (!same) {
            System.out.println("clickIdArr.add");
            clickIdArr.add(id);
        }
        return same;
    }

    // TimeActivityのallFlagがtrueの時，clickIdArrにlistViewのviewのidを全て格納する関数
    public void setClickIdAll() {

        if(clickIdArr.isEmpty()){
            System.out.println("clickIdArr.isEmpty()");
            for(int i=0; i < diasArr.size(); i++){
                clickIdArr.add(i);
            }
        } else {
            for(int i=0; i < diasArr.size(); i++){
                for(int j = 0; j < clickIdArr.size(); j++){
                    if( i == clickIdArr.get(j)){
                        same2 = true;
                    }
                }
                if(!same2){
                    clickIdArr.add(i);
                }
                same2 = false;
            }
        }
    }

}
