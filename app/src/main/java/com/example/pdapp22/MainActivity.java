package com.example.pdapp22;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcF;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // アダプタを扱うための変数
    private NfcAdapter mNfcAdapter;
    // GUI更新変数
    Handler guiThreadHandler;
    private String sid = "";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        guiThreadHandler = new Handler();
        // first view settings
        CollapsingToolbarLayout cl1 = findViewById(R.id.layout1);
        TextView tx4 = findViewById(R.id.text4);
        TextView txI = findViewById(R.id.textInfo);
        Button bt1 = findViewById(R.id.btn1);
        CheckBox cb1 = findViewById(R.id.chkB1);

        updateView(cl1, "お待ちください...");
        updateView(tx4,"ここに詳細が表示されます");
        updateView(txI,"(v 2.0) PD_app22 for team H2");

        updateView(cb1, "詳細を表示する");

        updateView(bt1,"カードを読ませてください");
        updateView(bt1,false);

        // listener settings
        bt1.setOnClickListener(this);
        findViewById(R.id.chkB1).setOnClickListener(this);

        // アダプタのインスタンスを取得
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        updateView(cl1, "読み込みできます");

    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onResume() {
        super.onResume();

        // NFCがかざされたときの設定
        Intent intent = new Intent(this, this.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // ほかのアプリを開かないようにする
        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_MUTABLE);
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onPause(){
        super.onPause();

        // Activityがバックグラウンドになったときは、受け取らない
        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        CollapsingToolbarLayout cl1 = findViewById(R.id.layout1);
        Button bt1 = findViewById(R.id.btn1);

        try{
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] dat = readTag(tag);
            sid = new String(dat, StandardCharsets.UTF_8).trim().substring(1,8);

            // 表示
            updateView(cl1, sid+" さん");
            updateView(bt1, sid+" さんを登録する");
            updateView(bt1, true);
            updateView(R.color.great);
        }catch (Exception e){
            updateView(cl1, "読み込みエラー");
            updateView(R.color.error);
            //Toast.makeText(getApplicationContext(), "NFC read failed", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn1:
                Snackbar.make(view, "送信しています...", Snackbar.LENGTH_LONG).show();
                new Thread(() -> {
                    Button bt1 = findViewById(R.id.btn1);
                    updateView(bt1,false);
                    try {
                        // http request
                        URL url = new URL("");
                        HttpURLConnection con = (HttpURLConnection)url.openConnection();
                        String str = InputStreamToString(con.getInputStream());
                        Snackbar.make(view, str, Snackbar.LENGTH_LONG).show();
                    } catch(Exception ex) {
                        Snackbar.make(view, "通信に失敗しました\n"+ex, Snackbar.LENGTH_SHORT).show();
                    } finally {
                        updateView(bt1,true);
                    }
                }).start();
                break;
            case R.id.chkB1:
                CheckBox cb1 = findViewById(R.id.chkB1);
                TextView tx4 = findViewById(R.id.text4);
                updateView(tx4,cb1.isChecked());
                break;
            default:
                Snackbar.make(view, "IDエラーが発生しました", Snackbar.LENGTH_LONG).show();
        }
    }
    // InputStream -> String
    static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    public void updateView(TextView tv,final String tx){
        guiThreadHandler.post(() -> tv.setText(tx));
    }
    public void updateView(TextView tv,final Boolean tb){
        int ti =  tb ? View.VISIBLE : View.GONE;
        guiThreadHandler.post(() -> tv.setVisibility(ti));
    }
    public void updateView(Button bt,final Boolean be){
        guiThreadHandler.post(() -> bt.setEnabled(be));
    }
    public void updateView(Button bt,final String bx){
        guiThreadHandler.post(() -> bt.setText(bx));
    }
    public void updateView(CheckBox cb,final String ct){
        guiThreadHandler.post(() -> cb.setText(ct));
    }
    public void updateView(CollapsingToolbarLayout cl, final String ct){
        guiThreadHandler.post(() -> cl.setTitle(ct));
    }
    public void updateView(final int ci){
        CollapsingToolbarLayout cl = findViewById(R.id.layout1);
        getWindow().setStatusBarColor(getColor(ci));
        guiThreadHandler.post(() -> cl.setBackgroundResource(ci));
    }

    public byte[] readTag(Tag tag) {
        TextView tx3= findViewById(R.id.text4);
        NfcF nfc = NfcF.get(tag);
        String resp = "";

        try {
            nfc.connect();
            // システムコード -> 0x93b1
            byte[] targetSystemCode = new byte[]{(byte) 0x93,(byte) 0xB1};
            // polling コマンドを作成
            byte[] polling = polling(targetSystemCode);
            resp += "pool="+Arrays.toString(polling)+"\n";
            // コマンドを送信して結果を取得
            byte[] pollingRes = nfc.transceive(polling);
            resp += "NFC TRANCE OK pRes="+Arrays.toString(pollingRes)+"\n";
            // System 0 のIDｍを取得(1バイト目はデータサイズ、2バイト目はレスポンスコード、IDmのサイズは8バイト)
            byte[] targetIDm = Arrays.copyOfRange(pollingRes, 2, 10);
            resp += "IDm= "+Arrays.toString(targetIDm)+"\n";
            // サービスに含まれているデータのサイズ
            int size = 1;
            // 対象のサービスコード -> byte[]{p1,p2} 40,0b -> 10,0b
            byte[] targetServiceCode = new byte[]{(byte)0x10,(byte) 0x0b};
            // Read Without Encryption コマンドを作成
            byte[] req = readWithoutEncryption(targetIDm, size, targetServiceCode);
            resp += "request="+Arrays.toString(req)+"\n";
            byte[] rer = requestService(targetIDm,targetServiceCode);
            byte[] res = nfc.transceive(rer);
            resp += "REQUEST Serv OK res="+Arrays.toString(res)+"\n";

            // コマンドを送信して結果を取得
            res = nfc.transceive(req);
            resp += "NFC REQUEST OK res="+Arrays.toString(res)+"\n";
            resp += "Read REQUEST OK res="+new String(res, StandardCharsets.UTF_8)+"\n";
            nfc.close();

            updateView(tx3,resp);

            // 結果をパースしてデータだけ取得
            return getSid(res);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage() , e);
            updateView(tx3,resp);
        }

        return null;
    }

    /**
     * Pollingコマンドの取得。
     * @param systemCode byte[] 指定するシステムコード
     * @return Pollingコマンド
     * @throws IOException
     */
    private byte[] polling(byte[] systemCode) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);

        bout.write(0x00);           // データ長バイトのダミー
        bout.write(0x00);           // コマンドコード
        bout.write(systemCode[0]);  // systemCode
        bout.write(systemCode[1]);  // systemCode
        bout.write(0x01);           // リクエストコード
        bout.write(0x0f);           // タイムスロット

        byte[] msg = bout.toByteArray();
        msg[0] = (byte) msg.length; // 先頭１バイトはデータ長
        return msg;
    }

    private byte[] requestService(byte[] idm,byte[] requestCode) throws IOException{
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);

        bout.write(0x00);              // データ長バイトのダミー
        bout.write(0x02);           // コマンドコード
        bout.write(idm);            // IDm 8byte
        bout.write(0x01);

        // リトルエンディアン、下位バイトから指定。
        bout.write(requestCode[1]);
        bout.write(requestCode[0]);

        byte[] msg = bout.toByteArray();
        msg[0] = (byte) msg.length; // 先頭１バイトはデータ長
        return msg;
    }

    /**
     * Read Without Encryptionコマンドの取得。
     * @param idm 指定するシステムのID
     * @param size 取得するデータの数
     * @return Read Without Encryptionコマンド
     * @throws IOException
     */
    private byte[] readWithoutEncryption(byte[] idm, int size, byte[] serviceCode) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(100);

        bout.write(0x00);              // データ長バイトのダミー
        bout.write(0x06);           // コマンドコード
        bout.write(idm);            // IDm 8byte
        bout.write(1);              // サービス数の長さ(以下２バイトがこの数分繰り返す)

        // サービスコードの指定はリトルエンディアンなので、下位バイトから指定します。
        bout.write(serviceCode[1]); // サービスコード下位バイト
        bout.write(serviceCode[0]); // サービスコード上位バイト
        bout.write(size);           // ブロック数

        // ブロック番号の指定
        for (int i = 0; i < size; i++) {
            bout.write(0x80);       // ブロックエレメント上位バイト 「Felicaユーザマニュアル抜粋」の4.3項参照
            bout.write(i);          // ブロック番号
        }

        byte[] msg = bout.toByteArray();
        msg[0] = (byte) msg.length; // 先頭１バイトはデータ長
        return msg;
    }

    /**
     * Read Without Encryption応答の解析。
     * @param res byte[]
     * @return 文字列表現
     * @throws Exception
     */
    private byte[][] parse(byte[] res) {
        // res[10] エラーコード。0x00の場合が正常
        if (res[10] != 0x00)
            throw new RuntimeException("Read Without Encryption Command Error");

        // res[12] 応答ブロック数
        // res[13 + n * 16] 実データ 16(byte/ブロック)の繰り返し
        int size = res[12];
        byte[][] data = new byte[size][16];
        for (int i = 0; i < size; i++) {
            byte[] tmp = new byte[16];
            int offset = 13 + i * 16;
            System.arraycopy(res, offset, tmp, 0, 16);

            data[i] = tmp;
        }
        return data;
    }

    private byte[] getSid(byte[] res) {
        // res[10] エラーコード。0x00の場合が正常
        if (res[10] != 0x00)
            throw new RuntimeException("Read Without Encryption Command Error");

        // res12  サイズ*16
        // res13~ データ
        int size = res[12];
        byte[] data = new byte[16*size];
        for (int i = 0; i < 16*size; i++) {
            int offset = 13 + i;
            data[i] = res[offset];
        }
        return data;
    }

}