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
import static com.esaulpaugh.android.headlong.TupleEntryFragment.Triple;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.util.Strings;

import java.util.List;

public class TupleEntryAdapter extends RecyclerView.Adapter<TupleEntryAdapter.ViewHolder> {

    private final Activity activity;
    private final List<Triple> list;

    private final int colorRed;
    private final int colorGreen;

    private int elementUnderEditPosition;

    public TupleEntryAdapter(Activity activity, List<Triple> list) {
        this.activity = activity;
        this.list = list;
        this.colorRed = activity.getResources().getColor(R.color.colorAccent);
        this.colorGreen = activity.getResources().getColor(R.color.colorPrimary);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.argument_row, parent, false);

        return new ViewHolder(itemView);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        final Triple triple = list.get(position);

        final String canonical = triple.abiType.getCanonicalType();

        holder.type.setText(canonical + ", " + MainActivity.friendlyClassName(triple.abiType));

        ArrayEntryAdapter.setEditTextAttributes(holder.typeableValue, triple.abiType);

        final View.OnClickListener startSubtuple = v -> {
            final int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;
            elementUnderEditPosition = adapterPos;
            EditorActivity.startSubtupleActivity(activity, canonical, false);
        };
        final View.OnClickListener startArray = v -> {
            final int adapterPos = holder.getBindingAdapterPosition();
            if (adapterPos == RecyclerView.NO_POSITION) return;
            elementUnderEditPosition = adapterPos;
            EditorActivity.startArrayActivity(activity, canonical, false);
        };

        if (canonical.startsWith("(") && canonical.endsWith(")")) {
            validate(triple, holder.editableValue);
            if(canonical.equals("()")) {
                list.set(position, new Triple(triple.abiType, Tuple.EMPTY));
                holder.editableValue.setBackgroundColor(colorGreen);
            } else {
                holder.editableValue.setOnClickListener(startSubtuple);
            }
            holder.typeableValue.setVisibility(View.INVISIBLE);
            holder.editableValue.setVisibility(View.VISIBLE);
        } else if (canonical.endsWith("]")) {
            holder.editableValue.setOnClickListener(startArray);
            validate(triple, holder.editableValue);
            holder.typeableValue.setVisibility(View.INVISIBLE);
            holder.editableValue.setVisibility(View.VISIBLE);
        } else {

            System.out.println("CANONICAL = " + canonical);

            switch (triple.abiType.typeCode()) {
            case ABIType.TYPE_CODE_BYTE:
            case ABIType.TYPE_CODE_INT:
            case ABIType.TYPE_CODE_LONG:
            case ABIType.TYPE_CODE_BIG_INTEGER:
            case ABIType.TYPE_CODE_BIG_DECIMAL: holder.typeableValue.setInputType(InputType.TYPE_CLASS_NUMBER); break;
            default: holder.typeableValue.setInputType(InputType.TYPE_CLASS_TEXT);
            }

//            holder.typeableValue.setOnFocusChangeListener((v, hasFocus) -> {
//                if (hasFocus) {
//                    final int adapterPos = holder.getBindingAdapterPosition();
//                    if (adapterPos == RecyclerView.NO_POSITION) return;
//                    Triple tt = list.get(adapterPos);
//                    validate(tt, holder.typeableValue);
//                }
//            });

            holder.typeableValue.removeTextChangedListener(holder.textWatcher);

            Object val = triple.object;
            String text = val == null ? "" : val.toString();
            holder.typeableValue.setText(text);

            holder.textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                    final int adapterPos = holder.getBindingAdapterPosition();
                    if (adapterPos == RecyclerView.NO_POSITION) return;

                    final String argString = s.toString();

                    Triple newTriple = list.get(adapterPos);

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

                    list.set(adapterPos, newNewTriple);

                    validate(newNewTriple, holder.typeableValue);
                }
            };
            holder.typeableValue.addTextChangedListener(holder.textWatcher);
            validate(triple, holder.typeableValue);
            holder.editableValue.setVisibility(View.INVISIBLE);
            holder.typeableValue.setVisibility(View.VISIBLE);
            setHint(holder.typeableValue, triple.abiType);
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

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView type;
        final EditText typeableValue;
        final View editableValue;
        TextWatcher textWatcher;

        ViewHolder(View view) {
            super(view);
            type = (TextView) view.findViewById(R.id.type);
            typeableValue = (EditText) view.findViewById(R.id.typeable_value);
            editableValue = view.findViewById(R.id.edit_button);
        }
    }

    synchronized void returnEdited(Object obj) {
        try {
            Triple existing = list.get(elementUnderEditPosition);
            list.set(elementUnderEditPosition, new Triple(existing.abiType, obj));
            notifyItemChanged(elementUnderEditPosition, null);
        } catch (IndexOutOfBoundsException ioobe) {
            Toast.makeText(activity, ioobe.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}