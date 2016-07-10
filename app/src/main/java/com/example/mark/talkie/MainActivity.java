package com.example.mark.talkie;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Handler;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private String hostName = "192.168.178.25";
    private int portNumber = 3003;

    private  TextView spokenText, answerText ;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private TextToSpeech tts ;
    private ToggleButton autoTXRXButton;

    private Handler myHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupInterface();
    }

    private void setupInterface() {

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); // TBD MWA Filthy hack - use a thread you lazy sod!
        //StrictMode.setThreadPolicy(policy);

        autoTXRXButton = (ToggleButton) findViewById(R.id.toggleButton);

        spokenText = (TextView) findViewById(R.id.requestText);
        answerText = (TextView) findViewById(R.id.answerTextView);
        Button sendButton = (Button) findViewById(R.id.sendButton);
        Button readbackButton = (Button) findViewById(R.id.readbackButton);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendSpoken();
            }
        });
        Button speakButton = (Button) findViewById(R.id.speakButton);
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }

        });
        readbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readBack();
            }

        });
        tts = new TextToSpeech(this,this);

    }

    private void readBack() {
        String spoken = answerText.getText().toString();
        sayIt(spoken);
    }

    private void sayIt(String spoken) {
        tts.setLanguage(Locale.getDefault());
        tts.speak(spoken, TextToSpeech.QUEUE_ADD, null);
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("FOO", "activity result");
        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String request = result.get(0);
                    spokenText.setText(request);
                    if (autoTXRXButton.isChecked()) {
                        sendSpoken();
                    }
                }
                break;
            }

        }
    }

    @Override
    public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS){
            int result=tts.setLanguage(Locale.getDefault());
            if(result==TextToSpeech.LANG_MISSING_DATA ||
                    result==TextToSpeech.LANG_NOT_SUPPORTED){
                Log.e("error", "This Language is not supported");
            }
        }
        else
            Log.e("error", "Initialization Failed!");
    }

    void sendSpoken() {
        String spoken = spokenText.getText().toString();
        Log.d("FOO", "sendSpoken: " + spoken);
        sendSocket("REQ", spoken );
    }

    void sendSocket(final String reason, final String text) {

        Runnable socketRunnable = new Runnable() {
            @Override
            public void run() {


                try {
                    Socket echoSocket = new Socket(hostName, portNumber);
                    PrintWriter out =
                            new PrintWriter(echoSocket.getOutputStream(), true);
                    out.println(reason + ":" + text);
                    BufferedReader in =
                            new BufferedReader(
                                    new InputStreamReader(echoSocket.getInputStream()));
                    final String response = in.readLine();
                    Log.d("FOO", "response:" + response);
                    myHandler.post(new Runnable() {
                        public void run() {
                            answerText.setText(response);
                            readBack();
                        }
                    });

                } catch (Exception e) {
                    Log.e("FOO", "Oh NOOO... socket exception", e);
                    myHandler.post(new Runnable() {
                        public void run() {
                            sayIt("You have a socket problem, I cannot talk to the assistant");
                        }
                    });
                }
            }
        };
        new Thread(socketRunnable).start();
    }
}
