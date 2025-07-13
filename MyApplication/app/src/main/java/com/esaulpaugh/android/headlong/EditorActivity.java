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
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;

import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;

@SuppressWarnings("deprecation")
public class EditorActivity extends Activity {

    static final String ARG_ACTIVITY_FOR_SUBTUPLE = "activity_for_subtuple";
    static final String ARG_TYPE_STRING = "type_string";
    static final String ENCODED_TUPLE_BYTES = "encoded_tuple_bytes";
    static final String ENCODED_ARRAY_BYTES = "encoded_array_bytes";
    static final String FOR_DEFAULT_VAL = "for_default_val";

    static final int CODE_SUBTUPLE = 1;
    static final int CODE_ARRAY = 2;

    private EntryFragment entryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        setTitle("edit");

        System.out.println("===== " + EditorActivity.class.getSimpleName());

        Bundle extras = getIntent().getExtras();
        final boolean forDefaultVal = extras.getBoolean(FOR_DEFAULT_VAL, false);
        final boolean activityForSubtuple = extras.getBoolean(ARG_ACTIVITY_FOR_SUBTUPLE);
        final String typeString = extras.getString(ARG_TYPE_STRING);

        if(activityForSubtuple) {
            entryFragment = TupleEntryFragment.newInstance(true, typeString, forDefaultVal);
        } else {
            entryFragment = ArrayEntryFragment.newInstance(forDefaultVal, typeString);
        }

        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.editor_frame, entryFragment);
        fragmentTransaction.commit();
    }

    static void startSubtupleActivity(Activity activity, String subtupleTypeString, boolean forDefaultVal) {
        Intent i = new Intent(activity, EditorActivity.class);
        i.putExtra(FOR_DEFAULT_VAL, forDefaultVal);
        i.putExtra(ARG_ACTIVITY_FOR_SUBTUPLE, true);
        i.putExtra(ARG_TYPE_STRING, subtupleTypeString);
        activity.startActivityForResult(i, CODE_SUBTUPLE);
    }

    static void startArrayActivity(Activity activity, String arrayTypeString, boolean forDefaultVal) {
        Intent i = new Intent(activity, EditorActivity.class);
        i.putExtra(FOR_DEFAULT_VAL, forDefaultVal);
        i.putExtra(ARG_ACTIVITY_FOR_SUBTUPLE, false);
        i.putExtra(ARG_TYPE_STRING, arrayTypeString);
        activity.startActivityForResult(i, CODE_ARRAY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(data != null) {
            if(requestCode == CODE_SUBTUPLE && resultCode == CODE_SUBTUPLE) {
                String subtupleTypeString = data.getStringExtra(TupleEntryFragment.ARG_SUBTUPLE_TYPE_STRING);

                boolean forDefaultVal = data.getBooleanExtra(FOR_DEFAULT_VAL, false);
                byte[] encodedTupleBytes = data.getByteArrayExtra(ENCODED_TUPLE_BYTES);
                Tuple subtuple = TupleType.parse(subtupleTypeString).decode(encodedTupleBytes);

                entryFragment.returnEditedObject(subtuple, forDefaultVal);
            } else if (requestCode == CODE_ARRAY && resultCode == CODE_ARRAY) {
                String arrayTypeString = data.getStringExtra(ArrayEntryFragment.ARG_ARRAY_TYPE_STRING);

                boolean forDefaultVal = data.getBooleanExtra(FOR_DEFAULT_VAL, false);
                byte[] encodedArrayBytes = data.getByteArrayExtra(ENCODED_ARRAY_BYTES);
                Object array = parseArrayType(arrayTypeString).decode(encodedArrayBytes);
                entryFragment.returnEditedObject(array, forDefaultVal);
            }
        }
    }
}
