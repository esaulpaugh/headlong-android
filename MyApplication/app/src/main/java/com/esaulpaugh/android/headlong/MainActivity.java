/*
Copyright 2019 Evan Saulpaugh

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.esaulpaugh.android.headlong;

import static com.esaulpaugh.android.headlong.ArrayEntryFragment.parseArrayType;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TupleEntryFragment tupleEntryFragment;
    private TextView output;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        postWave(findViewById(R.id.relative), getContentResolver(), handler);

        setTitle("headlong demo");

        output = (TextView) findViewById(R.id.output);
        Button gogo = (Button) findViewById(R.id.gogo);

        tupleEntryFragment = TupleEntryFragment.newInstance();

        // getSupportFragmentManager
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frag_frame, tupleEntryFragment);
        fragmentTransaction.commit();

        gogo.setOnClickListener(v -> {
            final String signature = tupleEntryFragment.getFunctionSignature();
            if(signature == null) {
                output.setText("signature is null");
                Toast.makeText(MainActivity.this, "signature is null", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    gogo(tupleEntryFragment.getFunctionSignature());
                } catch (IllegalArgumentException iae) {
                    output.setText(iae.getMessage());
                }
            }
        });
    }

    private void gogo(String signature) {
        Tuple masterTuple = tupleEntryFragment.getMasterTuple();

        try {
            Function f = new Function(signature);

            ByteBuffer bb = f.encodeCall(masterTuple);

            byte[] arr = bb.array();
            String formatted = f.annotateCall(arr);

            output.setText(formatted);

        } catch (RuntimeException re) {
            output.setText(re.getMessage());
            Toast.makeText(MainActivity.this, re.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data == null) {
            Log.d(TAG, "back presssed?, " + requestCode + ", " + resultCode);
        } else if(requestCode == EditorActivity.CODE_SUBTUPLE && resultCode == EditorActivity.CODE_SUBTUPLE) {
            String subtupleTypeString = data.getStringExtra(TupleEntryFragment.ARG_SUBTUPLE_TYPE_STRING);

            byte[] encodedTupleBytes = data.getByteArrayExtra(EditorActivity.ENCODED_TUPLE_BYTES);
            Tuple subtuple = TupleType.parse(subtupleTypeString).decode(encodedTupleBytes);

            tupleEntryFragment.returnEditedObject(subtuple, false);
        } else if(requestCode == EditorActivity.CODE_ARRAY && resultCode == EditorActivity.CODE_ARRAY) {

            String arrayTypeString = data.getStringExtra(ArrayEntryFragment.ARG_ARRAY_TYPE_STRING);
            byte[] encodedArrayBytes = data.getByteArrayExtra(EditorActivity.ENCODED_ARRAY_BYTES);
            tupleEntryFragment.returnEditedObject(parseArrayType(arrayTypeString).decode(encodedArrayBytes), false);
        } else {
            Log.e(TAG, "????????????????????????????????????????????????????????????????");
        }
    }

    private static final Pattern BRACKETS = Pattern.compile(Pattern.quote("[]"));

    static String friendlyClassName(ABIType<?> type) {
        final int arrayLen = type instanceof ArrayType ? type.asArrayType().getLength() : -1;
        return BRACKETS.matcher(type.clazz().getSimpleName()).replaceFirst("[" + (arrayLen == ArrayType.DYNAMIC_LENGTH ? "" : "" + arrayLen) + "]");
    }

    private static void postWave(View view, ContentResolver cr, Handler handler) {
        try {
            if (Settings.Secure.getInt(cr, "navigation_mode", -1) != 2) return;
        } catch (Exception ignored) {
        }
        final int color1 = Color.parseColor("#797979");
        final int color2 = Color.parseColor("#8c8c8c");
        final int sineMillis = 6000;
        final int frameMillis = 1000 / 60;

        final Runnable fadeCycle = new Runnable() {
            final long startTime = System.currentTimeMillis();

            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                float fraction = (elapsed % sineMillis) / (float) sineMillis; // 0..1
                float sineFrac = (float) ((Math.sin(fraction * 2 * Math.PI) + 1) / 2);

                int r = (int) (Color.red(color1) * (1 - sineFrac) + Color.red(color2) * sineFrac);
                int g = (int) (Color.green(color1) * (1 - sineFrac) + Color.green(color2) * sineFrac);
                int b = (int) (Color.blue(color1) * (1 - sineFrac) + Color.blue(color2) * sineFrac);

                view.setBackgroundColor(Color.rgb(r, g, b));
                handler.postDelayed(this, frameMillis);
            }
        };

        handler.post(fadeCycle);
    }
}
