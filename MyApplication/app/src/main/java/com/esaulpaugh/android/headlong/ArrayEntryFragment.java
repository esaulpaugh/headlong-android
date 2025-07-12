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

import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.Address;
import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.BigDecimalType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TypeFactory;
import com.esaulpaugh.headlong.util.Strings;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class ArrayEntryFragment extends EntryFragment {

    private static final String ARG_FOR_DEFAULT_VAL = "for_default_val";
    static final String ARG_ARRAY_TYPE_STRING = "array_type_string";

    private boolean forDefaultVal;
    private String arrayTypeString;

    private ABIType<Object> elementType;

    private List<Object> listElements;

    private int length;
    private Object defaultVal;
    private String defaultValString;

    static final int CATEGORY_TYPEABLE = 0, CATEGORY_TUPLE = 1, CATEGORY_ARRAY = 2;

    private int elementCategory;

    private ArrayEntryAdapter adapter;

    private View defaultValView;

    public ArrayEntryFragment() {
        // Required empty public constructor
    }

    public static ArrayEntryFragment newInstance(boolean forDefaultVal, String arrayTypeString) {
        ArrayEntryFragment fragment = new ArrayEntryFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FOR_DEFAULT_VAL, forDefaultVal);
        args.putString(ARG_ARRAY_TYPE_STRING, arrayTypeString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            forDefaultVal = getArguments().getBoolean(ARG_FOR_DEFAULT_VAL, false);
            arrayTypeString = getArguments().getString(ARG_ARRAY_TYPE_STRING);
        }
    }

    static void setHint(EditText editText, ABIType<?> type) {
        if(type.typeCode() == ABIType.TYPE_CODE_ARRAY) {
            editText.setHint(((ArrayType<?, ?, ?>) type).isString() ? "UTF-8" : "Hex");
        } else {
            editText.setHint("Value");
        }
    }

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_array_entry, container, false);

        final ArrayType<ABIType<?>, ?, ?> arrayType;
        try {
            arrayType = (ArrayType<ABIType<?>, ?, ?>) TypeFactory.create(arrayTypeString);
        } catch (IllegalArgumentException iae) {
            Toast.makeText(getActivity(), iae.getMessage(), Toast.LENGTH_LONG).show();
            return view;
        }

        elementType = (ABIType<Object>) arrayType.getElementType();
        listElements = new ArrayList<>();

        final String elementCanonical = elementType.getCanonicalType();

        elementCategory = elementCanonical.startsWith("(") && elementCanonical.endsWith(")")
                ? CATEGORY_TUPLE
                : elementCanonical.endsWith("]") ? CATEGORY_ARRAY : CATEGORY_TYPEABLE;

        final TextView arrayTypeStringView = (TextView) view.findViewById(R.id.array_type_string);
        arrayTypeStringView.setText(arrayTypeString);

        final EditText lengthView = (EditText) view.findViewById(R.id.length);
        if (arrayType.getLength() != -1) {
            length = arrayType.getLength();
            view.findViewById(R.id.enter_length).setVisibility(View.GONE);
        } else {
            lengthView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {

                    String lengthString = editable.toString();

                    if (lengthString.isEmpty()) {
                        return;
                    }

                    boolean valid;
                    try {
                        length = Integer.parseInt(editable.toString());
                        valid = length >= 0;
                    } catch (NumberFormatException nfe) {
                        valid = false;
                    }
                    if (valid) {
                        setAllToDefault();
                    } else {
                        Toast.makeText(getActivity(), length < 0 ? "Error: negative length" : "Error: invalid length", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        final TextView type = (TextView) view.findViewById(R.id.type);

        final View invisibleView;
        if (elementCategory == CATEGORY_TYPEABLE) {
            invisibleView = view.findViewById(R.id.edit_button);
            defaultValView = view.findViewById(R.id.typeable_value);
            ArrayEntryAdapter.setEditTextAttributes((EditText) defaultValView, elementType);

            setHint((EditText) defaultValView, elementType);
        } else {
            invisibleView = (EditText) view.findViewById(R.id.typeable_value);
            defaultValView = view.findViewById(R.id.edit_button);
        }

        invisibleView.setVisibility(View.INVISIBLE);
        defaultValView.setVisibility(View.VISIBLE);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.array_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new ArrayEntryAdapter(getActivity(), elementType, elementCategory, listElements);
        recyclerView.setAdapter(adapter);

        type.setText("Set all, " + elementCanonical + ", " + MainActivity.friendlyClassName(elementType));

        switch (elementCategory) {
        case CATEGORY_TUPLE:
            if (elementCanonical.equals("()")) {
                defaultVal = Tuple.EMPTY;
                setAllToDefault();
            } else {
                defaultValView.setOnClickListener(v -> EditorActivity.startSubtupleActivity(getActivity(), elementCanonical, true));
            }
            break;
        case CATEGORY_ARRAY:
            defaultValView.setOnClickListener(v -> {
                EditorActivity.startArrayActivity(getActivity(), elementCanonical, true);
            });
            break;
        case CATEGORY_TYPEABLE: {
            final EditText box = (EditText) defaultValView;
            switch (arrayType.getElementType().typeCode()) {
                case ABIType.TYPE_CODE_INT:
                case ABIType.TYPE_CODE_LONG:
                case ABIType.TYPE_CODE_BIG_INTEGER:
                case ABIType.TYPE_CODE_BIG_DECIMAL: box.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            box.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                    final String argString = s.toString();

                    boolean valid = true;
                    try {
                        final boolean isArray = elementType.typeCode() == ABIType.TYPE_CODE_ARRAY;
                        final boolean isString = isArray && ((ArrayType<?, ?, ?>) elementType).isString();
                        defaultVal = parseElement(elementType, argString, isString, isArray);
                        defaultValString = argString;
                    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                        valid = false;
                        defaultVal = null;
                        defaultValString = null;
                    }

                    setAllToDefault();

                    int colorInt = getResources().getColor(valid ? R.color.colorPrimary : R.color.colorAccent);
                    defaultValView.setBackgroundColor(colorInt);
                }
            });
            break;
        }
        default:
            throw new Error();
        }

        final Button returnArray = (Button) view.findViewById(R.id.return_array);
        returnArray.setOnClickListener(v -> {

// TODO handle race conditions

            Intent intent = new Intent();
            try {

                Object array = createArray(arrayType, length);

                intent.putExtra(EditorActivity.FOR_DEFAULT_VAL, forDefaultVal);
                intent.putExtra(ArrayEntryFragment.ARG_ARRAY_TYPE_STRING, arrayTypeString);

                byte[] arrayBytes = parseArrayType(arrayTypeString).encode(array).array();

                intent.putExtra(EditorActivity.ENCODED_ARRAY_BYTES, arrayBytes);

                getActivity().setResult(EditorActivity.CODE_ARRAY, intent);
                getActivity().finish();

            } catch (Exception e) {
                Toast.makeText(getActivity(), e.getClass().getSimpleName() + " " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }

    static ArrayType<ABIType<Object>, Object, Object> parseArrayType(String typeStr) {
        return TypeFactory.create(typeStr);
    }

    private void setAllToDefault() {
        System.out.println("setAllToDefault()");
        listElements.clear();
        if (elementCategory == CATEGORY_TYPEABLE) {
            for (int i = 0; i < length; i++) {
                listElements.add(defaultValString);
            }
        } else {
            for (int i = 0; i < length; i++) {
                listElements.add(defaultVal);
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public synchronized void returnEditedObject(Object obj, boolean forDefaultVal) {
        if (forDefaultVal) {
            defaultValView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            defaultVal = obj;
            setAllToDefault();
        } else {
            try {
                adapter.returnEditedObject(obj);
            } catch (NullPointerException npe) {
                Toast.makeText(getActivity(), npe.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Object createArray(ArrayType<?, ?, ?> arrayType, int length) {
        int i = 0;
        switch (arrayType.getElementType().clazz().getName()) {
        case "java.lang.Byte":
        case "B": {
            byte[] arr = new byte[length];
            for (Object e : listElements) {
                arr[i++] = Byte.parseByte((String) e);
            }
            return arr;
        }
        case "java.lang.Short":
        case "S": {
            short[] arr = new short[length];
            for (Object e : listElements) {
                arr[i++] = Short.parseShort((String) e);
            }
            return arr;
        }
        case "java.lang.Integer":
        case "I": {
            int[] arr = new int[length];
            for (Object e : listElements) {
                Integer val = Integer.parseInt((String) e);
                elementType.validate(val);
                arr[i++] = val;
            }
            return arr;
        }
        case "java.lang.Long":
        case "J": {
            long[] arr = new long[length];
            for (Object e : listElements) {
                Long val = Long.parseLong((String) e);
                elementType.validate(val);
                arr[i++] = val;
            }
            return arr;
        }
        case "java.lang.Float":
        case "F": {
            float[] arr = new float[length];
            for (Object e : listElements) {
                Float f = Float.parseFloat((String) e);
                elementType.validate(f);
                arr[i++] = f;
            }
            return arr;
        }
        case "java.lang.Double":
        case "D": {
            double[] arr = new double[length];
            for (Object e : listElements) {
                Double d = Double.parseDouble((String) e);
                elementType.validate(d);
                arr[i++] = d;
            }
            return arr;
        }
        case "java.lang.Character":
        case "C": {
            char[] arr = new char[length];
            for (Object e : listElements) {
                Character c = ((String) e).charAt(0);
                elementType.validate(c);
                arr[i++] = c;
            }
            return arr;
        }
        case "java.lang.Boolean":
        case "Z": {
            boolean[] arr = new boolean[length];
            for (Object e : listElements) {
                arr[i++] = Boolean.parseBoolean((String) e);
            }
            return arr;
        }
        default: {
//            if (elementClassName.startsWith("L")
//                    && elementClassName.endsWith(";")) {
//                return "[" + elementClassName;
//            }

            return createObjectArray(arrayType);
        }
        }
    }

    private Object[] createObjectArray(ArrayType<?, ?, ?> arrayType) {
        Object[] array = (Object[]) Array.newInstance(arrayType.getElementType().clazz(), length);

        int i = 0;
        if(elementCategory == CATEGORY_TYPEABLE) {
            final boolean isArray = elementType.typeCode() == ABIType.TYPE_CODE_ARRAY;
            final boolean isString = isArray && ((ArrayType<?, ?, ?>) elementType).isString();
            for (Object e : listElements) {
                array[i++] = parseElement(elementType, (String) e, isString, isArray);
            }
        } else {
            for (Object e : listElements) {
                array[i++] = e;
            }
        }

        return array;
    }

    static Object parseElement(ABIType<?> elementType, String val, boolean isString, boolean isArray) {
        return isString
                ? val
                : isArray
                    ? Strings.decode(val, Strings.HEX)
                    : parseArgument(elementType, val);
    }

    @SuppressWarnings("unchecked")
    static Object parseArgument(ABIType<?> elementType, String val) {
        final Object x;
        switch (elementType.typeCode()) {
            case ABIType.TYPE_CODE_BOOLEAN: x = Boolean.parseBoolean(val); break;
            case ABIType.TYPE_CODE_BYTE: x = Byte.parseByte(val); break;
            case ABIType.TYPE_CODE_INT: x = Integer.parseInt(val); break;
            case ABIType.TYPE_CODE_LONG: x = Long.parseLong(val); break;
            case ABIType.TYPE_CODE_BIG_INTEGER: x = new BigInteger(val); break;
            case ABIType.TYPE_CODE_BIG_DECIMAL: x = new BigDecimal(new BigInteger(val, 10), ((BigDecimalType) elementType).getScale()); break;
            case ABIType.TYPE_CODE_ADDRESS: x = Address.wrap(val); break;
            default: throw new AssertionError();
        }
        ((ABIType<Object>) elementType).validate(x);
        return x;
    }
}
