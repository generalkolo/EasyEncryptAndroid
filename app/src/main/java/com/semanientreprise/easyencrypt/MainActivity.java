package com.semanientreprise.easyencrypt;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.materialspinner.MaterialSpinner;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.encDec_et)
    EditText encDecEt;
    @BindView(R.id.key)
    EditText key;
    @BindView(R.id.encDec_tv)
    TextView encDecTv;
    @BindView(R.id.spinner)
    MaterialSpinner spinner;
    @BindView(R.id.share_btn)
    ImageView shareBtn;
    private String algorithm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initSpinner();
    }

    private void initSpinner() {
        //method to initialize the spinner
        //used to display the supported coins
        spinner.setItems(getString(R.string.select), getString(R.string.aes), getString(R.string.des));
        algorithm = getString(R.string.select);

        spinner.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {

            @Override
            public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                algorithm = item;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_help:
                showInfo(1);
                break;
            case R.id.action_info:
                showInfo(2);
                break;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInfo(int dialogNumber) {
            final Dialog dialog = new Dialog(MainActivity.this, R.style.DialogSlideAnimCart);
            dialog.setContentView(R.layout.about_us_layout);
            dialog.getWindow().setGravity(Gravity.CENTER);
            Button dialogButton = dialog.findViewById(R.id.dialogButtonOK);
            RelativeLayout info_container = dialog.findViewById(R.id.info_container);
            RelativeLayout help_container = dialog.findViewById(R.id.help_container);
            switch (dialogNumber){
                case 1:
                    dialog.setTitle("How To");
                    help_container.setVisibility(View.VISIBLE);
                    info_container.setVisibility(View.GONE);
                    break;
                case 2:
                    dialog.setTitle("Info");
                    help_container.setVisibility(View.GONE);
                    info_container.setVisibility(View.VISIBLE);
                    break;
            }
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            dialog.setCancelable(true);
            dialog.show();
    }

    @OnClick({R.id.encrypt_btn, R.id.decrypt_btn, R.id.share_btn})
    public void onViewClicked(View view) {
        String keyEntered = key.getText().toString();
        String plainCipher = encDecEt.getText().toString();

        switch (view.getId()) {
            case R.id.encrypt_btn:
                if (checkKeyAndAlgorithm()) {
                    encryptDecryptText(algorithm, keyEntered, "encrypt", plainCipher);
                    shareBtn.setVisibility(View.VISIBLE);
                } else {
                    showToast("Please ensure that all details are fully correct!!");
                }
                break;
            case R.id.decrypt_btn:
                if (checkKeyAndAlgorithm()) {
                    encryptDecryptText(algorithm, keyEntered, "decrypt", plainCipher);
                    shareBtn.setVisibility(View.VISIBLE);
                } else {
                    showToast("Please ensure that all details are fully correct!!");
                }
                break;
            case R.id.share_btn:
                shareText();
                break;
        }
    }

    //Method to share the encrypted text
    private void shareText() {
        String textToShare = encDecTv.getText().toString();
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "SHARE TEXT");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
            startActivity(Intent.createChooser(sharingIntent, "Share Via: "));
    }

    private void encryptDecryptText(String algorithm, String keyEntered, String instruction, String plainCipher) {
        if (algorithm.equals(getString(R.string.aes))) {
            if (keyEntered.length() < 16) {
                keyEntered = getPaddedKeyVersion(keyEntered, 16);
            }
            switch (instruction) {
                case "encrypt":
                    encryptText(keyEntered, plainCipher, algorithm);
                    break;
                case "decrypt":
                    decryptText(keyEntered, plainCipher, algorithm);
                    break;
            }
        }
        else if (algorithm.equals(getString(R.string.des))) {
            if (keyEntered.length() < 8) {
                keyEntered = getPaddedKeyVersion(keyEntered, 8);
            }
            switch (instruction) {
                case "encrypt":
                    encryptText(keyEntered, plainCipher, algorithm);
                    break;
                case "decrypt":
                    decryptText(keyEntered, plainCipher, algorithm);
                    break;
            }
        }
    }

    private void decryptText(String keyEntered, String plainCipher, String algorithm) {

        try {
            byte[] k = keyEntered.getBytes();
            byte[] dec = Base64.decode(plainCipher, 0);
            SecretKeySpec secretkey = new SecretKeySpec(k, algorithm);
            Cipher enc = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
            enc.init(Cipher.DECRYPT_MODE, secretkey, new IvParameterSpec(k));
            byte[] utf8 = enc.doFinal(dec);
            String decrypted = new String(utf8, "UTF8");

            encDecTv.setText(decrypted);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    private void encryptText(String keyEntered, String plainCipher, String algorithm) {
        try {
            byte[] k = keyEntered.getBytes();
            byte[] utf8 = plainCipher.getBytes("UTF8");
            Cipher enc = Cipher.getInstance(algorithm + "/CBC/PKCS5Padding");
            SecretKeySpec secretkey = new SecretKeySpec(k, algorithm);
            enc.init(Cipher.ENCRYPT_MODE, secretkey, new IvParameterSpec(k));
            byte[] encr = enc.doFinal(utf8);

            String encrypted = Base64.encodeToString(encr, Base64.DEFAULT);
            encDecTv.setText(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPaddedKeyVersion(String keyEntered, int length) {
        StringBuilder newKey = new StringBuilder(keyEntered);
        for (int i = keyEntered.length(); i < length; i++) {
            newKey.append(" ");
        }
        return newKey.toString();
    }

    private boolean checkKeyAndAlgorithm() {
        String keyString = key.getText().toString();
        String plainCipher = encDecEt.getText().toString();
        if (keyString.isEmpty()) {
            showToast("Key cannot be empty");
            return false;
        }
        if (algorithm.equals(getString(R.string.select))) {
            showToast("Please, select an Algorithm to be used");
            return false;
        }
        if (algorithm.equals(getString(R.string.aes)) && keyString.length() > 16){
            showToast("For AES Algorithm: Key cannot be more than 16");
        }
        if (algorithm.equals(getString(R.string.des)) && keyString.length() > 8){
            showToast("For DES Algorithm: Key cannot be more than 8");
        }
        if (plainCipher.isEmpty()) {
            showToast("Please, enter a text to be encrypted or decrypted!");
            return false;
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    //fraction of code to display press the back button again to exist the application
    //based on the back button pressed.
    boolean twice = false;

    @Override
    public void onBackPressed() {
            exitApp();
    }

    private void exitApp() {
        if (twice) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            System.exit(0);
        }
        twice = true;
        Toast.makeText(this, "Tap again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                twice = false;
            }
        }, 3000);
    }

}