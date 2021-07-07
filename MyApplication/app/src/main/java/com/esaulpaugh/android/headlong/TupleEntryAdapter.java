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
import com.esaulpaugh.headlong.abi.ArrayType;
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

        holder.type.setText(canonical + ", " + MainActivity.friendlyClassName(triple.abiType.clazz(), null));

        holder.typeableValue.setText("");
        ArrayEntryAdapter.setEditTextAttributes(holder.typeableValue, triple.abiType);

        if(canonical.startsWith("(") && canonical.endsWith(")")) {

            if(canonical.equals("()")) {
                list.set(holder.getAdapterPosition(), new Triple(triple.abiType, Tuple.EMPTY));
            } else {
                holder.editableValue.setOnClickListener(v -> {
                    elementUnderEditPosition = holder.getAdapterPosition();
                    EditorActivity.startSubtupleActivity(activity, canonical, false);
                });
            }
            validate(list.get(holder.getAdapterPosition()), holder.editableValue);
            holder.typeableValue.setVisibility(View.INVISIBLE);
            holder.editableValue.setVisibility(View.VISIBLE);
        } else if (canonical.endsWith("]")) {

            holder.editableValue.setOnClickListener(v -> {
                elementUnderEditPosition = holder.getAdapterPosition();
                EditorActivity.startArrayActivity(activity, canonical, false);
            });
            validate(triple, holder.editableValue);
            holder.typeableValue.setVisibility(View.INVISIBLE);
            holder.editableValue.setVisibility(View.VISIBLE);
        } else {

            System.out.println("CANONICAL = " + canonical);

//            final int code = triple.abiType.typeCode();
//            if(code != ABIType.TYPE_CODE_BOOLEAN
//                    && code != ABIType.TYPE_CODE_ARRAY
//                    && code != ABIType.TYPE_CODE_TUPLE) {
//                holder.typeableValue.setInputType(InputType.TYPE_CLASS_NUMBER);
//            } else {
//                holder.typeableValue.setInputType(InputType.TYPE_CLASS_TEXT);
//            }

            holder.typeableValue.setOnFocusChangeListener((v, hasFocus) -> {
                if(hasFocus) {
                    int pos = holder.getAdapterPosition();
                    Triple ttt = list.get(pos);
                    validate(ttt, holder.typeableValue);
                }
            });

            holder.typeableValue.removeTextChangedListener(holder.textWatcher);
            holder.textWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {

                    final String argString = s.toString();

                    Triple newTriple = list.get(holder.getAdapterPosition());

                    Object obj;
                    final boolean isArray = newTriple.abiType.typeCode() == ABIType.TYPE_CODE_ARRAY;
                    if (isArray && ((ArrayType<ABIType<?>, ?>) newTriple.abiType).isString()) {
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
                                obj = newTriple.abiType.parseArgument(argString);
                            }
                        } catch (IllegalArgumentException | UnsupportedOperationException iae) {
                            obj = null;
                        }
                    }

                    Triple newNewTriple = new Triple(newTriple.abiType, obj);

                    list.set(holder.getAdapterPosition(), newNewTriple);

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

    private void validate(Triple triple, View valueView) {
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
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
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

    synchronized void returnEditedObject(Object obj) {
        try {
            Triple existing = list.get(elementUnderEditPosition);
            list.set(elementUnderEditPosition, new Triple(existing.abiType, obj));
            notifyItemChanged(elementUnderEditPosition);
        } catch (IndexOutOfBoundsException ioobe) {
            Toast.makeText(activity, ioobe.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}