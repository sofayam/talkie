package com.example.mark.talkie;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Handler;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//import org.apache.http.impl.client.HttpClientBuilder;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    //private String NLPURL = "http://localhost:3000/poster";
   private String NLPURL = "http://192.168.178.25:3000/say";

    private  TextView spokenText, answerText, headerText ;
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


        autoTXRXButton = (ToggleButton) findViewById(R.id.toggleButton);

        spokenText = (TextView) findViewById(R.id.requestText);
        answerText = (TextView) findViewById(R.id.answerTextView);
        headerText = (TextView) findViewById(R.id.headerText);

        headerText.setText(getCurrentTimeStamp()); // + BuildConfig.buildTime);

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
        Log.d("TALKIE", "activity result");
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
        Log.d("TALKIE", "sendSpoken: " + spoken);
        sendSocket("start", spoken );
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
        Date now = new Date();
        String strDate = sdfDate.format(now);
        return strDate;
    }
    private static String getStringResponse(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        // Read Server Response
        try {
            while ((line = reader.readLine()) != null) {
                // Append server response in string
                sb.append(line + "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private static JSONObject parseJSONReply (String json) throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(json);
        JSONObject jsonObject = (JSONObject) obj;
        return jsonObject;

    }

    private static String buildJSONRequest(String state, String text)  {
        JSONObject obj = new JSONObject();
        obj.put("state", state);
        obj.put("text", text);

        StringWriter out = new StringWriter();
        try {
            obj.writeJSONString(out);
        } catch (Exception e) {
        }

        String jsonText = out.toString();
        return jsonText;
    }

    void sendSocket(final String state, final String text) {

        // Socket code inspired by Santosh
        // http://cr-scm01.de.bosch.com:7990/projects/UPA/repos/smart-navigation/browse/client/src/main/java/com/bosch/smartnavigation/network/PostTask.java

        Runnable socketRunnable = new Runnable() {
            @Override
            public void run() {

                String JSONPayload = buildJSONRequest(state,text);

                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(NLPURL);
                    urlConnection = (HttpURLConnection)url.openConnection();

                    urlConnection.setReadTimeout(10000);
                    urlConnection.setConnectTimeout(15000);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/json");



                    urlConnection.connect();

                    //urlConnection.setRequestProperty("jsonpayload", JSONPayload);

                    OutputStreamWriter out = new OutputStreamWriter(
                            urlConnection.getOutputStream());
                    out.write(JSONPayload);
                    out.close();

                    if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Log.e("TALKIE", "Status Code: " + urlConnection.getResponseCode());
                    } else {
                        Log.d("TALKIE", "Connection established!!");
                        String resp = getStringResponse(urlConnection.getInputStream());
                        JSONObject jsonResponse = parseJSONReply(resp);

                        Log.d("TALKIE", (String)jsonResponse.get("state"));
                        Log.d("TALKIE", (String)jsonResponse.get("text"));

                    }
                } catch (Exception e) {
                    Log.e("TALKIE", "Oh NOOO... socket exception", e);
                    myHandler.post(new Runnable() {
                        public void run() {
                            sayIt("You have a socket problem, I cannot talk to the assistant");
                        }
                    });

                } finally {
                    urlConnection.disconnect();
                }

            }
        };
        new Thread(socketRunnable).start();
    }
}
