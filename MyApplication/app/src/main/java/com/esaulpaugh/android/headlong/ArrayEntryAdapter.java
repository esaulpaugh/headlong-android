package com.esaulpaugh.android.headlong;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.esaulpaugh.headlong.abi.StackableType;
import com.esaulpaugh.headlong.abi.Tuple;

import java.util.List;

public class ArrayEntryAdapter extends RecyclerView.Adapter<ArrayEntryAdapter.ViewHolder> {

    private Activity activity;
    private List<Object> list;

    private int colorRed, colorGreen;

    private final StackableType<?> elementType;
    private final String elementCanonicalTypeString;

    private int elementCategory;

    private Integer elementUnderEditPosition;

    public ArrayEntryAdapter(Activity activity, StackableType<?> elementType, int elementCategory, List<Object> list) {
        this.activity = activity;
        this.colorRed = activity.getResources().getColor(R.color.colorAccent);
        this.colorGreen = activity.getResources().getColor(R.color.colorPrimary);
        this.elementType = elementType;
        this.elementCanonicalTypeString = elementType.getCanonicalType();
        this.elementCategory = elementCategory;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.argument_row, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        final Object element = list.get(position);

        holder.type.setText(String.valueOf(position));

        switch (elementCategory) {
        case ArrayEntryFragment.CATEGORY_TUPLE:
            if(elementCanonicalTypeString.equals("()")) {
                list.set(holder.getAdapterPosition(), Tuple.EMPTY);
            } else {
                holder.editableValue.setOnClickListener(v -> {
                    elementUnderEditPosition = holder.getAdapterPosition();
                    EditorActivity.startSubtupleActivity(activity, elementCanonicalTypeString);
                });
            }

            holder.typeableValue.setVisibility(View.INVISIBLE);
            holder.editableValue.setVisibility(View.VISIBLE);
            break;
        case ArrayEntryFragment.CATEGORY_ARRAY:
            holder.editableValue.setOnClickListener(v -> {
                elementUnderEditPosition = holder.getAdapterPosition();
                EditorActivity.startArrayActivity(activity, elementCanonicalTypeString, false);
            });

            holder.typeableValue.setVisibility(View.INVISIBLE);
            holder.editableValue.setVisibility(View.VISIBLE);
            break;
        case ArrayEntryFragment.CATEGORY_TYPEABLE:
            holder.typeableValue.removeTextChangedListener(holder.textWatcher);

            holder.typeableValue.setOnFocusChangeListener((v, hasFocus) -> {
                if(hasFocus) {
                    validateTypeable((String) list.get(position), holder.typeableValue);
                }
            });

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

                    boolean valid = true;
                    try {
                        Object obj = argString;
                        list.set(holder.getAdapterPosition(), obj);
                    } catch (IllegalArgumentException iae) {
                        valid = false;
                    }

                    int colorInt = activity.getResources().getColor(valid ? R.color.colorPrimary : R.color.colorAccent);
                    holder.typeableValue.setBackgroundColor(colorInt);
                }
            };
            holder.typeableValue.addTextChangedListener(holder.textWatcher);

            holder.editableValue.setVisibility(View.INVISIBLE);
            holder.typeableValue.setVisibility(View.VISIBLE);
            break;
        default:
            throw new Error();
        }

        if(elementCategory == ArrayEntryFragment.CATEGORY_TYPEABLE) {
            validateTypeable((String) element, holder.typeableValue);
        } else {
            validateEditable(element, holder.editableValue);
        }
    }

//    private void validate(String valString, View valueView) {
//
//    }

    private void validateTypeable(String valString, EditText typeableValueView) {
        boolean valid = valString != null;
        if(valid) {
            try {
                Object val = elementType.parseArgument(valString);
                typeableValueView.setText(valString);
            } catch (IllegalArgumentException iae) {
                valid = false;
            }
        }

        System.out.println("validateTypeable() = " + valid);

        typeableValueView.setBackgroundColor(valid ? colorGreen : colorRed);
    }

    private void validateEditable(Object val, View editableValueView) {

        boolean valid = val != null;
        if(valid) {
            try {
                elementType.validate(val);
            } catch (IllegalArgumentException iae) {
                valid = false;
            }
        }

        System.out.println("validate() = " + valid);

        editableValueView.setBackgroundColor(valid ? colorGreen : colorRed);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView type;
        private EditText typeableValue;
        private View editableValue;
        private TextWatcher textWatcher;

        private ViewHolder(View view) {
            super(view);
            type = (TextView) view.findViewById(R.id.type);
            typeableValue = (EditText) view.findViewById(R.id.typeable_value);
            editableValue = view.findViewById(R.id.editable_value);
        }
    }

    void returnEditedObject(Object array) {
        list.set(elementUnderEditPosition, array);
        notifyItemChanged(elementUnderEditPosition);
    }
}