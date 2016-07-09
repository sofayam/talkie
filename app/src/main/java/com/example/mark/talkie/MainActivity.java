package com.example.mark.talkie;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupInterface();
    }

    private void setupInterface() {

        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText spokenText = (EditText) findViewById(R.id.requestText);
                // read contents of text field and send to socket
                String foo = spokenText.getText().toString();
                final int foo1 = Log.d("FOO", foo);
            }
        });
    }
}
