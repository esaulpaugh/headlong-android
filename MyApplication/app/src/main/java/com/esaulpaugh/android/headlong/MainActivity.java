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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.abi.TypeFactory;

import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TupleEntryFragment tupleEntryFragment;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("headlong demo");

        output = (TextView) findViewById(R.id.output);
        Button gogo = (Button) findViewById(R.id.gogo);

        tupleEntryFragment = TupleEntryFragment.newInstance();

        FragmentManager fm = getFragmentManager(); // getSupportFragmentManager
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
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
            String formatted = Function.formatCall(arr, 0, arr.length);

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

    static String friendlyClassName(Class<?> clazz, Integer arrayLength) {
        final String className = clazz.getName();

        final int split = className.lastIndexOf('[') + 1;
        final boolean hasArraySuffix = split > 0;
        final String base = hasArraySuffix ? className.substring(split) : className;
        final StringBuilder sb = new StringBuilder();
        switch (base) {
        case "B": sb.append("byte"); break;
        case "S": sb.append("short"); break;
        case "I": sb.append("int"); break;
        case "J": sb.append("long"); break;
        case "F": sb.append("float"); break;
        case "D": sb.append("double"); break;
        case "C": sb.append("char"); break;
        case "Z": sb.append("boolean"); break;
        default: {
            int lastDotIndex = base.lastIndexOf('.');
            if(lastDotIndex != -1) {
                sb.append(base, lastDotIndex + 1, base.length() - (base.charAt(0) == 'L' ? 1 : 0));
            }
        }
        }
        if(hasArraySuffix) {
            int i = 0;
            if(arrayLength != null && arrayLength >= 0) {
                sb.append('[').append(arrayLength).append(']');
                i++;
            }
            for ( ; i < split; i++) {
                sb.append("[]");
            }
        }
        return sb.toString();
    }
}
