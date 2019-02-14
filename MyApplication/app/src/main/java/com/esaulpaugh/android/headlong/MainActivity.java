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

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;

import java.nio.ByteBuffer;
import java.text.ParseException;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TupleEntryFragment tupleEntryFragment;
    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                } catch (ParseException pe) {
                    output.setText(pe.getMessage());
                }
            }
        });
    }

    private void gogo(String signature) throws ParseException {
        Tuple masterTuple = tupleEntryFragment.getMasterTuple();

        Function f = new Function(signature);

        try {
            ByteBuffer bb = f.encodeCall(masterTuple);

            output.setText(Function.formatCall(bb.array()));

        } catch (IllegalArgumentException iae) {
            output.setText(iae.getMessage());
            Toast.makeText(MainActivity.this, iae.getMessage(), Toast.LENGTH_SHORT).show();
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
            try {
                Tuple subtuple = TupleType.parse(subtupleTypeString).decode(encodedTupleBytes);

                tupleEntryFragment.returnEditedObject(subtuple, false);

            } catch (ParseException pe) {
                throw new RuntimeException(pe);
            }
        } else if(requestCode == EditorActivity.CODE_ARRAY && resultCode == EditorActivity.CODE_ARRAY) {

            String arrayTypeString = data.getStringExtra(ArrayEntryFragment.ARG_ARRAY_TYPE_STRING);

            byte[] encodedArrayBytes = data.getByteArrayExtra(EditorActivity.ENCODED_ARRAY_BYTES);
            try {
                Tuple wrapper = TupleType.parse("(" + arrayTypeString + ")").decode(encodedArrayBytes);

                tupleEntryFragment.returnEditedObject(wrapper.get(0), false);

            } catch (ParseException pe) {
                throw new RuntimeException(pe);
            }
        } else {
            Log.e(TAG, "????????????????????????????????????????????????????????????????");
        }
    }
}
