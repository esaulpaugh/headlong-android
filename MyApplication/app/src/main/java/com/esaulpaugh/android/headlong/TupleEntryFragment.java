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

import static com.esaulpaugh.android.headlong.ArrayEntryFragment.setHint;
import static com.esaulpaugh.android.headlong.EditorActivity.ENCODED_TUPLE_BYTES;
import static com.esaulpaugh.android.headlong.EditorActivity.FOR_DEFAULT_VAL;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.util.Strings;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class TupleEntryFragment extends EntryFragment {

    private int colorRed;
    private int colorGreen;
    private int elementUnderEditPosition = -1;

    public static class Triple {

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

//    private TupleEntryAdapter adapter;

    private EditText enterSignature;

    private LinearLayout listLayout;
    private List<Triple> list;

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
        list = new ArrayList<>();
        if (getArguments() != null) {
            forDefaultVal = getArguments().getBoolean(FOR_DEFAULT_VAL);
            forSubtuple = getArguments().getBoolean(ARG_FOR_SUBTUPLE);
            subtupleTypeString = getArguments().getString(ARG_SUBTUPLE_TYPE_STRING);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_tuple_entry, container, false);

        colorRed = getActivity().getResources().getColor(R.color.colorAccent);
        colorGreen = getActivity().getResources().getColor(R.color.colorPrimary);

        Button returnTuple = (Button) view.findViewById(R.id.return_tuple);
        ScrollView scrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        listLayout = (LinearLayout) view.findViewById(R.id.list_layout);
        enterSignature = (EditText) view.findViewById(R.id.enter_signature);
        TextView tupleTypeString = (TextView) view.findViewById(R.id.tuple_type_string);

//        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

//        adapter = new TupleEntryAdapter(getActivity(), list);

//        recyclerView.setAdapter(adapter);

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
        list.clear();
        listLayout.removeAllViews();
        try {
            final LayoutInflater inflater = LayoutInflater.from(getActivity());
            int position = 0;
            for(ABIType<?> abiType : new Function(signature).getInputs()) {

                final Triple tripl = new Triple(abiType, null);

                list.add(tripl);

                View itemView = inflater.inflate(R.layout.argument_row, listLayout, false);

                bind(itemView, tripl, position++);

                listLayout.addView(itemView);
            }
        } catch (RuntimeException re) {
            // do nothing
        } finally {
//            adapter.notifyDataSetChanged();
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
//        adapter.returnEdited(obj);
        try {
            Triple existing = list.get(elementUnderEditPosition);
            Triple replacement = new Triple(existing.abiType, obj);
            list.set(elementUnderEditPosition, replacement);
            validate(replacement, listLayout.getChildAt(elementUnderEditPosition).findViewById(R.id.edit_button));
//            notifyItemChanged(elementUnderEditPosition, null);
        } catch (IndexOutOfBoundsException ioobe) {
            Toast.makeText(getActivity(), ioobe.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        complete();
    }

    private synchronized void complete() {
        Object[] args = new Object[list.size()];
        int i = 0;
        for (Triple triple : list) {
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

    private void bind(View rowView, Triple triple, int position) {
        TextView type = (TextView) rowView.findViewById(R.id.type);
        EditText typeableValue = (EditText) rowView.findViewById(R.id.typeable_value);
        View editableValue = rowView.findViewById(R.id.edit_button);

        final String canonical = triple.abiType.getCanonicalType();

        type.setText(canonical + ", " + MainActivity.friendlyClassName(triple.abiType));

        ArrayEntryFragment.setEditTextAttributes(typeableValue, triple.abiType);

        final View.OnClickListener startSubtuple = v -> {
//            final int adapterPos = getBindingAdapterPosition();
//            if (adapterPos == RecyclerView.NO_POSITION) return;
            elementUnderEditPosition = position;
            EditorActivity.startSubtupleActivity(getActivity(), canonical, false);
        };
        final View.OnClickListener startArray = v -> {
//            final int adapterPos = getBindingAdapterPosition();
//            if (adapterPos == RecyclerView.NO_POSITION) return;
            elementUnderEditPosition = position;
            EditorActivity.startArrayActivity(getActivity(), canonical, false);
        };

        if (canonical.startsWith("(") && canonical.endsWith(")")) {
            validate(triple, editableValue);
            if(canonical.equals("()")) {
                list.set(position, new Triple(triple.abiType, Tuple.EMPTY));
                editableValue.setBackgroundColor(colorGreen);
            } else {
                editableValue.setOnClickListener(startSubtuple);
            }
            typeableValue.setVisibility(View.INVISIBLE);
            editableValue.setVisibility(View.VISIBLE);
        } else if (canonical.endsWith("]")) {
            editableValue.setOnClickListener(startArray);
            validate(triple, editableValue);
            typeableValue.setVisibility(View.INVISIBLE);
            editableValue.setVisibility(View.VISIBLE);
        } else {

            System.out.println("CANONICAL = " + canonical);

            switch (triple.abiType.typeCode()) {
                case ABIType.TYPE_CODE_BYTE:
                case ABIType.TYPE_CODE_INT:
                case ABIType.TYPE_CODE_LONG:
                case ABIType.TYPE_CODE_BIG_INTEGER:
                case ABIType.TYPE_CODE_BIG_DECIMAL: typeableValue.setInputType(InputType.TYPE_CLASS_NUMBER); break;
                default: typeableValue.setInputType(InputType.TYPE_CLASS_TEXT);
            }

//            typeableValue.setOnFocusChangeListener((v, hasFocus) -> {
//                if (hasFocus) {
//                    final int adapterPos = getBindingAdapterPosition();
//                    if (adapterPos == RecyclerView.NO_POSITION) return;
//                    Triple tt = list.get(adapterPos);
//                    validate(tt, typeableValue);
//                }
//            });

//            typeableValue.removeTextChangedListener(textWatcher);

            Object val = triple.object;
            String text = val == null ? "" : val.toString();
            typeableValue.setText(text);

            TextWatcher textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {

//                    final int adapterPos = getBindingAdapterPosition();
//                    if (adapterPos == RecyclerView.NO_POSITION) return;

                    final String argString = s.toString();

                    Triple newTriple = list.get(position);

                    Object obj;
                    final boolean isArray = newTriple.abiType.typeCode() == ABIType.TYPE_CODE_ARRAY;
                    if (isArray && newTriple.abiType.asArrayType().isString()) {
                        obj = argString;
                    } else {
                        try {
                            if(isArray || newTriple.abiType.typeCode() == ABIType.TYPE_CODE_TUPLE) {
                                if(newTriple.abiType.clazz() == (Object) byte[].class) {
                                    obj = Strings.decode(argString, Strings.HEX);
                                } else {
                                    obj = null;
                                }
                            } else {
                                obj = ArrayEntryFragment.parseArgument(newTriple.abiType, argString);
                            }
                        } catch (IllegalArgumentException | UnsupportedOperationException iae) {
                            obj = null;
                        }
                    }

                    Triple newNewTriple = new Triple(newTriple.abiType, obj);

                    list.set(position, newNewTriple);

                    validate(newNewTriple, typeableValue);
                }
            };
            typeableValue.addTextChangedListener(textWatcher);
            validate(triple, typeableValue);
            editableValue.setVisibility(View.INVISIBLE);
            typeableValue.setVisibility(View.VISIBLE);
            setHint(typeableValue, triple.abiType);
        }
    }

    private boolean validate(Triple triple, View valueView) {
        boolean valid = triple.object != null;
        if(valid) {
            try {
                triple.abiType.validate(triple.object);
            } catch (IllegalArgumentException iae) {
                valid = false;
            }
        }

        System.out.println("T validate() = " + valid);

        valueView.setBackgroundColor(valid ? colorGreen : colorRed);

        return valid;
    }
}
