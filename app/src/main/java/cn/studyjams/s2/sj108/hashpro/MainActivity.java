package cn.studyjams.s2.sj108.hashpro;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.qqtheme.framework.picker.FilePicker;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback

{

    private static final int PERMISSION_ONE = 1;
    private static final int PERMISSION_TWO = 2;
    private static final String TAG = "MainActivityTAG";
    private static final String STARTUP_TRACE_NAME = "Trace_Begin";
    private static final String CRC32_VALUE = "crc32_tag";

    private ClipboardManager manager;
    @BindView(R.id.et_main_file_path)
    EditText etMainFilePath;
    @BindView(R.id.btn_main_open)
    ImageButton btnMainOpen;
    @BindView(R.id.tv_main_generate)
    TextView tvMainGenerate;
    @BindView(R.id.iv_main_md5_compare)
    ImageView ivMainMd5Compare;
    @BindView(R.id.iv_main_md5_copy)
    ImageView ivMainMd5Copy;
    @BindView(R.id.et_main_md5_value)
    EditText etMainMd5Value;
    @BindView(R.id.iv_main_sha1_compare)
    ImageView ivMainSha1Compare;
    @BindView(R.id.iv_main_sha1_copy)
    ImageView ivMainSha1Copy;
    @BindView(R.id.et_main_sha1_value)
    EditText etMainSha1Value;
    @BindView(R.id.iv_main_crc32_compare)
    ImageView ivMainCrc32Compare;
    @BindView(R.id.iv_main_crc32_copy)
    ImageView ivMainCrc32Copy;
    @BindView(R.id.et_main_crc32_value)
    EditText etMainCrc32Value;
    private AlertDialog alertDialog;
    private Toolbar toolbar;
    private Trace trace;
    private FirebaseAnalytics mFirebaseAnalytics;

    class HashCac extends AsyncTask<String, String, MyResult> {
        ProgressDialog pb = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            etMainMd5Value.setText("");
            etMainSha1Value.setText("");
            etMainCrc32Value.setText("");

            pb.setTitle(getString(R.string.pb_async_title));
            pb.setMessage(getString(R.string.pb_async_cac));
            pb.show();
        }

        @Override
        protected MyResult doInBackground(String... params) {
            //用于保存firebase analytics异常log
            Bundle analytic_Param = new Bundle();
            try {
                int i;
                InputStream fis = new FileInputStream(params[0]);
                byte[] buffer = new byte[1024];
                MessageDigest completeSHA1 = MessageDigest.getInstance("SHA1");
                MessageDigest completeMD5 = MessageDigest.getInstance("MD5");
                CRC32 crc32 = new CRC32();
                int numRead;
                do {
                    numRead = fis.read(buffer);
                    if (numRead > 0) {
                        completeSHA1.update(buffer, 0, numRead);
                        completeMD5.update(buffer, 0, numRead);
                        crc32.update(buffer, 0, numRead);
                    }
                } while (numRead != -1);
                byte[] bSHA1 = completeSHA1.digest();
                byte[] bMD5 = completeMD5.digest();

                String resultSHA1 = "";
                String resultMD5 = "";
                fis.close();
                for (i = 0; i < bSHA1.length; i += 1) {
                    resultSHA1 = resultSHA1 + Integer.toString((bSHA1[i] & 255) + 256, 16).substring(1);
                }
                for (i = 0; i < bMD5.length; i += 1) {
                    resultMD5 = resultMD5 + Integer.toString((bMD5[i] & 255) + 256, 16).substring(1);
                }
                MyResult r = new MyResult();
                r.MD5 = resultMD5;
                r.SHA1 = resultSHA1;
                System.err.println(r.MD5);
                System.err.println(r.SHA1);
                r.CRC32 = crc32.getValue() + "";
                return r;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "FNFE caught");
                FirebaseCrash.report(e);
                analytic_Param.putString("FileNotFound",e.toString());
                return null;
            } catch (NoSuchAlgorithmException e2) {
                e2.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "NSAE caught");
                FirebaseCrash.report(e2);
                analytic_Param.putString("NoSuchAlgorithm",e2.toString());
                return null;
            } catch (IOException e3) {
                e3.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, TAG, "IOE caught");
                FirebaseCrash.report(e3);
                analytic_Param.putString("IOException",e3.toString());
                return null;
            } finally {
                mFirebaseAnalytics.logEvent("Analytic_TAG",analytic_Param);
            }
        }

        @Override
        protected void onPostExecute(MyResult myResult) {
            Log.i(TAG, myResult.MD5);
            etMainMd5Value.setText(myResult.MD5);
            etMainSha1Value.setText(myResult.SHA1);
            etMainCrc32Value.setText(myResult.CRC32);
            pb.dismiss();
            trace.incrementCounter(CRC32_VALUE, Long.parseLong(myResult.CRC32));
        }
    }

    public class MyResult {
        public String MD5;
        public String SHA1;
        public String CRC32;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ONE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                // Permission request was denied.
                Snackbar.make(toolbar, R.string.result_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 1500);
            }
        }
        if (requestCode == PERMISSION_TWO) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileExplorer();
            } else {
                // Permission request was denied.
                Snackbar.make(toolbar, R.string.result_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                }, 1500);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ButterKnife.bind(this);
        manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        trace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        FirebaseCrash.log("trace begin");
        trace.start();


        checkReadPermission();

        // TODO: 2017/6/10 后续升级提供新功能再启用
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        initEvent();
        FirebaseCrash.log("Activity created");
    }

    private void checkReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(toolbar, R.string.string_quest_permission, Snackbar.LENGTH_LONG).setAction(R.string.snackbar_request_permission, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_ONE);
                    }
                }).show();
            }
        }
    }

    private void getReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(toolbar, R.string.string_quest_permission, Snackbar.LENGTH_LONG).setAction(R.string.snackbar_request_permission, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_TWO);
                    }
                }).show();
            } else {
                openFileExplorer();
            }
        } else {
            openFileExplorer();
        }
    }

    private void initEvent() {
        btnMainOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getReadPermission();
                FirebaseCrash.logcat(Log.INFO, TAG, "Open File button clicked");
            }
        });
        etMainFilePath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileExplorer();
            }
        });
        tvMainGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHash();
            }
        });
        ivMainCrc32Copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyHash(etMainCrc32Value.getText().toString().trim());
            }
        });
        ivMainMd5Copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyHash(etMainMd5Value.getText().toString().trim());
            }
        });
        ivMainSha1Copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyHash(etMainSha1Value.getText().toString().trim());
            }
        });
        ivMainMd5Compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compareDialog(etMainMd5Value.getText().toString().trim());
            }
        });
        ivMainSha1Compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compareDialog(etMainSha1Value.getText().toString().trim());
            }
        });
        ivMainCrc32Compare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compareDialog(etMainCrc32Value.getText().toString().trim());
            }
        });
    }

    private void compareDialog(final String s) {
        mFirebaseAnalytics.setCurrentScreen(this,"current_screen",null);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null);
        EditText etDialogValue1 = (EditText) view.findViewById(R.id.et_dialog_value1);
        final EditText etDialogValue2 = (EditText) view.findViewById(R.id.et_dialog_value2);
        final TextInputLayout textInputDialog2 = (TextInputLayout) view.findViewById(R.id.text_input_dialog2);
        Button btnComp = (Button) view.findViewById(R.id.btn_dialog_comp);
        Button btnCancel = (Button) view.findViewById(R.id.btn_dialog_cancel);


        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_title);
        dialog.setView(view);
        etDialogValue1.setText(s);
        btnComp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value2 = etDialogValue2.getText().toString().trim();
                if (TextUtils.isEmpty(value2)) {
                    Snackbar.make(toolbar, R.string.snack_compare, Snackbar.LENGTH_SHORT).show();
                } else if (s.equals(value2)) {
                    textInputDialog2.setErrorTextAppearance(R.style.rightAppearance);
                    textInputDialog2.setError(getString(R.string.textinput_correct));
                } else {
                    textInputDialog2.setErrorTextAppearance(R.style.errorAppearance);
                    textInputDialog2.setError(getString(R.string.textinput_notice));
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog = dialog.show();

    }

    private void copyHash(String hash) {
        if (TextUtils.isEmpty(hash)) {
            Snackbar.make(toolbar, R.string.copy_no_data, Snackbar.LENGTH_SHORT).show();
        } else {
            ClipData data = ClipData.newPlainText("text", hash);
            manager.setPrimaryClip(data);
            Snackbar.make(toolbar, R.string.copy_get, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void doHash() {
        HashCac hashCac = new HashCac();
        String filePath = etMainFilePath.getText().toString().trim();
        if (TextUtils.isEmpty(filePath)) {
            Snackbar.make(toolbar, R.string.snackbar_no_file, Snackbar.LENGTH_SHORT).show();
        } else {
            hashCac.execute(filePath);
        }

    }


    private void openFileExplorer() {

        FilePicker picker = new FilePicker(MainActivity.this, FilePicker.FILE);
        picker.setShowHideDir(false);
        picker.setRootPath(Environment.getExternalStorageDirectory().getPath());
        picker.setOnFilePickListener(new FilePicker.OnFilePickListener() {
            @Override
            public void onFilePicked(String currentPath) {
                etMainFilePath.setText(currentPath);
            }
        });
        picker.show();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        trace.stop();
    }
}
