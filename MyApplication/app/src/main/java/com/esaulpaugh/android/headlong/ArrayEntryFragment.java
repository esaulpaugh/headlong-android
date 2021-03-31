package com.esaulpaugh.android.headlong;

import android.annotation.SuppressLint;
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
import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TypeFactory;
import com.esaulpaugh.headlong.util.Strings;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ArrayEntryFragment extends Fragment implements EntryFragment {
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

    @SuppressLint("SetTextI18n")
    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_array_entry, container, false);

        final ArrayType<ABIType<?>, ?> arrayType;
        try {
            arrayType = (ArrayType<ABIType<?>, ?>) TypeFactory.create(arrayTypeString);
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

        TextView arrayTypeStringView = (TextView) view.findViewById(R.id.array_type_string);
        arrayTypeStringView.setText(arrayTypeString);

        EditText lengthView = (EditText) view.findViewById(R.id.length);

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
                        refreshList();
                    } else {
                        Toast.makeText(getActivity(), length < 0 ? "Error: negative length" : "Error: invalid length", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        TextView type = (TextView) view.findViewById(R.id.type);

        final View invisibleView;
        if (elementCategory == CATEGORY_TYPEABLE) {
            invisibleView = view.findViewById(R.id.edit_button);
            defaultValView = view.findViewById(R.id.typeable_value);
            ArrayEntryAdapter.setEditTextAttributes((EditText) defaultValView, elementType);
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

        type.setText("Set all, " + elementCanonical + ", " + MainActivity.friendlyClassName(elementType.clazz(), null));

        switch (elementCategory) {
        case CATEGORY_TUPLE:
            if (elementCanonical.equals("()")) {
                defaultVal = Tuple.EMPTY;
                refreshList();
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
            ((EditText) defaultValView).addTextChangedListener(new TextWatcher() {
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
                        final boolean isString = isArray && ((ArrayType) elementType).isString();
                        defaultVal = parseElement(elementType, argString, isString, isArray);
                        defaultValString = argString;
                    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                        valid = false;
                        defaultVal = null;
                        defaultValString = null;
                    }

                    refreshList();

                    int colorInt = getResources().getColor(valid ? R.color.colorPrimary : R.color.colorAccent);
                    defaultValView.setBackgroundColor(colorInt);
                }
            });
            break;
        }
        default:
            throw new Error();
        }

        Button returnArray = (Button) view.findViewById(R.id.return_array);

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

    static ArrayType<?, Object> parseArrayType(String typeStr) {
        return (ArrayType<?, Object>) TypeFactory.create(typeStr, Object.class);
    }

    private void refreshList() {
        System.out.println("refreshList()");
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
    public void returnEditedObject(Object obj, boolean forDefaultVal) {
        if (forDefaultVal) {
            defaultValView.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            defaultVal = obj;
            refreshList();
        } else {
            try {
                adapter.returnEditedObject(obj);
            } catch (NullPointerException npe) {
                Toast.makeText(getActivity(), npe.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private Object createArray(ArrayType arrayType, int length) {
        int i = 0;
        switch (arrayType.getElementType().clazz().getName()) {
        case "java.lang.Byte":
        case "B": {
            byte[] arr = new byte[length];
            for (Object e : listElements) {
                arr[i++] = (Byte) elementType.parseArgument((String) e);
            }
            return arr;
        }
        case "java.lang.Short":
        case "S": {
            short[] arr = new short[length];
            for (Object e : listElements) {
                arr[i++] = (Short) elementType.parseArgument((String) e);
            }
            return arr;
        }
        case "java.lang.Integer":
        case "I": {
            int[] arr = new int[length];
            for (Object e : listElements) {
                arr[i++] = (Integer) elementType.parseArgument((String) e);
            }
            return arr;
        }
        case "java.lang.Long":
        case "J": {
            long[] arr = new long[length];
            for (Object e : listElements) {
                arr[i++] = (Long) elementType.parseArgument((String) e);
            }
            return arr;
        }
        case "java.lang.Float":
        case "F": {
            float[] arr = new float[length];
            for (Object e : listElements) {
                arr[i++] = (Float) elementType.parseArgument((String) e);
            }
            return arr;
        }
        case "java.lang.Double":
        case "D": {
            double[] arr = new double[length];
            for (Object e : listElements) {
                arr[i++] = (Double) elementType.parseArgument((String) e);
            }
            return arr;
        }
        case "java.lang.Character":
        case "C": {
            char[] arr = new char[length];
            for (Object e : listElements) {
                arr[i++] = (Character) elementType.parseArgument((String) e);
            }
            return arr;
        }
        case "java.lang.Boolean":
        case "Z": {
            boolean[] arr = new boolean[length];
            for (Object e : listElements) {
                arr[i++] = (Boolean) elementType.parseArgument((String) e);
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

    private Object[] createObjectArray(ArrayType arrayType) {
        Object[] array = (Object[]) Array.newInstance(arrayType.getElementType().clazz(), length);

        int i = 0;
        if(elementCategory == CATEGORY_TYPEABLE) {
            final boolean isArray = elementType.typeCode() == ABIType.TYPE_CODE_ARRAY;
            final boolean isString = isArray && ((ArrayType) elementType).isString();
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
                    : elementType.parseArgument(val);
    }
}
