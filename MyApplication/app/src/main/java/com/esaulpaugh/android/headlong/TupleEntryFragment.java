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

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;

import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.android.headlong.EditorActivity.ENCODED_TUPLE_BYTES;
import static com.esaulpaugh.android.headlong.EditorActivity.FOR_DEFAULT_VAL;

@SuppressWarnings("deprecation")
public class TupleEntryFragment extends Fragment implements EntryFragment {

    static class Triple {

        final ABIType<Object> abiType;
        final Object object;

        @SuppressWarnings("unchecked")
        Triple(ABIType<?> abiType, Object object) {
            this.abiType = (ABIType<Object>) abiType;
            this.object = object;
        }
    }

    static final String ARG_FOR_SUBTUPLE = "for_subtuple";
    static final String ARG_SUBTUPLE_TYPE_STRING = "subtuple_type_string";

    private boolean forDefaultVal;

    private boolean forSubtuple;
    private String subtupleTypeString;

    private String functionSignature;
    private Tuple masterTuple;

    private TupleEntryAdapter adapter;

    private EditText enterSignature;

    private List<Triple> listElements;

    public TupleEntryFragment() {
        // Required empty public constructor
    }

    public static TupleEntryFragment newInstance() {
        return newInstance(false, null, false);
    }

    public static TupleEntryFragment newInstance(boolean subtuple, String subtupleTypeString, boolean forDefaultVal) {
        TupleEntryFragment fragment = new TupleEntryFragment();
        Bundle args = new Bundle();
        args.putBoolean(FOR_DEFAULT_VAL, forDefaultVal);
        args.putBoolean(ARG_FOR_SUBTUPLE, subtuple);
        args.putString(ARG_SUBTUPLE_TYPE_STRING, subtupleTypeString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listElements = new ArrayList<>();
        if (getArguments() != null) {
            forDefaultVal = getArguments().getBoolean(FOR_DEFAULT_VAL);
            forSubtuple = getArguments().getBoolean(ARG_FOR_SUBTUPLE);
            subtupleTypeString = getArguments().getString(ARG_SUBTUPLE_TYPE_STRING);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_tuple_entry, container, false);

        Button returnTuple = (Button) view.findViewById(R.id.return_tuple);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        enterSignature = (EditText) view.findViewById(R.id.enter_signature);
        TextView tupleTypeString = (TextView) view.findViewById(R.id.tuple_type_string);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new TupleEntryAdapter(getActivity(), listElements);

        recyclerView.setAdapter(adapter);

        if (forSubtuple) {
            enterSignature.setVisibility(View.GONE);
            tupleTypeString.setVisibility(View.VISIBLE);
            tupleTypeString.setText(subtupleTypeString);

            replaceData(subtupleTypeString);
        } else {
            enterSignature.setVisibility(View.VISIBLE);
            tupleTypeString.setVisibility(View.GONE);

            enterSignature.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    replaceData(s.toString());
                }
            });
        }

        returnTuple.setOnClickListener(v -> complete());

        return view;
    }

    @SuppressWarnings("NotifyDataSetChanged")
    private void replaceData(String signature) {
        listElements.clear();
        try {
            for(ABIType<?> abiType : new Function(signature).getInputs()) {
                listElements.add(new Triple(abiType, null));
            }
        } catch (RuntimeException re) {
            // do nothing
        } finally {
            adapter.notifyDataSetChanged();
        }
    }

    public String getFunctionSignature() {
        return functionSignature;
    }

    public Tuple getMasterTuple() {
        return masterTuple;
    }

    @Override
    public synchronized void returnEditedObject(Object obj, boolean defaultVal) {
        adapter.returnEdited(obj);
    }

    @Override
    public void onPause() {
        super.onPause();
        complete();
    }

    private synchronized void complete() {
        Object[] args = new Object[listElements.size()];
        int i = 0;
        for (Triple triple : listElements) {
            Object arg = triple.object;
            if(arg == null) {
                System.err.println("null arg" + i);
                return;
            }
            args[i++] = arg;
        }

        if (forSubtuple) {
            final Tuple subtuple = Tuple.from(args);
            final Intent intent = new Intent();
            try {
                intent.putExtra(TupleEntryFragment.ARG_SUBTUPLE_TYPE_STRING, subtupleTypeString);

                TupleType<Tuple> tt = TupleType.parse(subtupleTypeString);
                byte[] tupleBytes = tt.encode(subtuple).array();

                intent.putExtra(FOR_DEFAULT_VAL, forDefaultVal);
                intent.putExtra(ENCODED_TUPLE_BYTES, tupleBytes);

                getActivity().setResult(EditorActivity.CODE_SUBTUPLE, intent);
                getActivity().finish();

            } catch (Exception e) {
//                Toast.makeText(getActivity(), e.getClass().getSimpleName() + " " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            System.out.println("setting masterTuple");
            this.masterTuple = Tuple.from(args);
            this.functionSignature = enterSignature.getText().toString();
        }
    }
}
