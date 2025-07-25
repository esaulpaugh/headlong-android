///*
//Copyright 2019 Evan Saulpaugh
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//*/
//package com.esaulpaugh.android.headlong;
//
//import static com.esaulpaugh.android.headlong.ArrayEntryFragment.setHint;
//
//import android.app.Activity;
//import android.text.Editable;
//import android.text.InputType;
//import android.text.TextWatcher;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.TextView;
//
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.esaulpaugh.headlong.abi.ABIType;
//import com.esaulpaugh.headlong.abi.ArrayType;
//import com.esaulpaugh.headlong.abi.Tuple;
//import com.esaulpaugh.headlong.abi.UnitType;
//
//import java.util.List;
//
//public class ArrayEntryAdapter extends RecyclerView.Adapter<ArrayEntryAdapter.ViewHolder> {
//
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        final TextView type;
//        final EditText typeableValue;
//        final View editableValue;
//        TextWatcher textWatcher;
//
//        ViewHolder(View view) {
//            super(view);
//            type = (TextView) view.findViewById(R.id.type);
//            typeableValue = (EditText) view.findViewById(R.id.typeable_value);
//            editableValue = view.findViewById(R.id.edit_button);
//        }
//    }
//
//    private final Activity activity;
//    private final List<Object> list;
//
//    private final int colorRed;
//    private final int colorGreen;
//
//    private final ABIType<Object> elementType;
//    private final String elementCanonicalTypeString;
//
//    private final int elementCategory;
//
//    private Integer elementUnderEditPosition;
//
//    public ArrayEntryAdapter(Activity activity, ABIType<Object> elementType, int elementCategory, List<Object> list) {
//        this.activity = activity;
//        this.colorRed = activity.getResources().getColor(R.color.colorAccent);
//        this.colorGreen = activity.getResources().getColor(R.color.colorPrimary);
//        this.elementType = elementType;
//        this.elementCanonicalTypeString = elementType.getCanonicalType();
//        this.elementCategory = elementCategory;
//        this.list = list;
//    }
//
//    @Override
//    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View itemView = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.argument_row, parent, false);
//
//        setEditTextAttributes((EditText) itemView.findViewById(R.id.typeable_value), elementType);
//
//        return new ViewHolder(itemView);
//    }
//
//    static void setEditTextAttributes(EditText editText, ABIType<?> elementType) {
//        int inputType = InputType.TYPE_CLASS_NUMBER;
//        switch (elementType.typeCode()) {
//            case ABIType.TYPE_CODE_BYTE:
//            case ABIType.TYPE_CODE_INT:
//            case ABIType.TYPE_CODE_LONG:
//            case ABIType.TYPE_CODE_BIG_INTEGER: {
//                if (!((UnitType<?>) elementType).isUnsigned()) {
//                    inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
//                }
//                break;
//            }
//            case ABIType.TYPE_CODE_BIG_DECIMAL: {
//                inputType |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
//                if (!((UnitType<?>) elementType).isUnsigned()) {
//                    inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
//                }
//                break;
//            }
//            default:
//                inputType = InputType.TYPE_CLASS_TEXT;
//        }
//        editText.setInputType(inputType);
//    }
//
//    @Override
//    public void onBindViewHolder(ViewHolder holder, int position) {
//
//        final Object element = list.get(position);
//
//        holder.type.setText(String.valueOf(position));
//
//        final View.OnClickListener startSubtuple = v -> {
//            final int adapterPos = holder.getBindingAdapterPosition();
//            if (adapterPos == RecyclerView.NO_POSITION) return;
//            elementUnderEditPosition = adapterPos;
//            EditorActivity.startSubtupleActivity(activity, elementCanonicalTypeString, false);
//        };
//        final View.OnClickListener startArray = v -> {
//            final int adapterPos = holder.getBindingAdapterPosition();
//            if (adapterPos == RecyclerView.NO_POSITION) return;
//            elementUnderEditPosition = adapterPos;
//            EditorActivity.startArrayActivity(activity, elementCanonicalTypeString, false);
//        };
//
//        switch (elementCategory) {
//        case ArrayEntryFragment.CATEGORY_TUPLE:
//            if(elementCanonicalTypeString.equals("()")) {
//                list.set(position, Tuple.EMPTY);
//            } else {
//                holder.editableValue.setOnClickListener(startSubtuple);
//            }
//            holder.typeableValue.setVisibility(View.INVISIBLE);
//            holder.editableValue.setVisibility(View.VISIBLE);
//            break;
//        case ArrayEntryFragment.CATEGORY_ARRAY:
//            holder.editableValue.setOnClickListener(startArray);
//            holder.typeableValue.setVisibility(View.INVISIBLE);
//            holder.editableValue.setVisibility(View.VISIBLE);
//            break;
//        case ArrayEntryFragment.CATEGORY_TYPEABLE:
//            holder.typeableValue.removeTextChangedListener(holder.textWatcher);
//
////            holder.typeableValue.setOnFocusChangeListener((v, hasFocus) -> {
////                if(hasFocus) {
////                    final int adapterPos = holder.getBindingAdapterPosition();
////                    if (adapterPos == RecyclerView.NO_POSITION) return;
////                    validateTypeable((String) list.get(adapterPos), holder.typeableValue, true);
////                }
////            });
//
//            holder.textWatcher = new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//                }
//
//                @Override
//                public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//                }
//
//                @Override
//                public void afterTextChanged(Editable s) {
//
//                    final String argString = s.toString();
//
//                    validateTypeable(argString, holder.typeableValue, false);
//
//                    final int adapterPos = holder.getBindingAdapterPosition();
//                    if (adapterPos == RecyclerView.NO_POSITION) return;
//
//                    list.set(adapterPos, argString);
//                }
//            };
//            holder.typeableValue.addTextChangedListener(holder.textWatcher);
//
//            holder.editableValue.setVisibility(View.INVISIBLE);
//            holder.typeableValue.setVisibility(View.VISIBLE);
//
//            setHint(holder.typeableValue, elementType);
//            break;
//        default:
//            throw new Error();
//        }
//
//        if(elementCategory == ArrayEntryFragment.CATEGORY_TYPEABLE) {
//            validateTypeable((String) element, holder.typeableValue, true);
//        } else {
//            validateEditable(element, holder.editableValue);
//        }
//    }
//
//    private void validateTypeable(String valString, EditText typeableValueView, boolean setText) {
//        boolean valid = valString != null;
//        if(valid) {
//            try {
//                final boolean isArray = elementType.typeCode() == ABIType.TYPE_CODE_ARRAY;
//                final boolean isString = isArray && ((ArrayType<?, ?, ?>) elementType).isString();
//                Object val = ArrayEntryFragment.parseElement(elementType, valString, isString, isArray);
//                elementType.validate(val);
//                if(setText) {
//                    typeableValueView.setText(valString);
//                }
//            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException iae) {
//                valid = false;
//            }
//        }
//
//        System.out.println("validateTypeable(" + setText + ") = " + valid);
//
//        typeableValueView.setBackgroundColor(valid ? colorGreen : colorRed);
//    }
//
//    private void validateEditable(Object val, View editableValueView) {
//
//        boolean valid = val != null;
//        if(valid) {
//            try {
//                elementType.validate(val);
//            } catch (IllegalArgumentException iae) {
//                valid = false;
//            }
//        }
//
//        System.out.println("validate() = " + valid);
//
//        editableValueView.setBackgroundColor(valid ? colorGreen : colorRed);
//    }
//
//    @Override
//    public int getItemCount() {
//        return list.size();
//    }
//
//    synchronized void returnEditedObject(Object array) {
//        list.set(elementUnderEditPosition, array);
//        notifyItemChanged(elementUnderEditPosition);
//    }
//}