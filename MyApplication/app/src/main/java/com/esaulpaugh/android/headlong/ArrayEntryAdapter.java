package com.esaulpaugh.android.headlong;

import static com.esaulpaugh.android.headlong.ArrayEntryFragment.setHint;

import android.app.Activity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;

import com.esaulpaugh.headlong.abi.ABIType;
import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.UnitType;

import java.util.List;

public class ArrayEntryAdapter extends RecyclerView.Adapter<TupleEntryAdapter.ViewHolder> {

    private final Activity activity;
    private final List<Object> list;

    private final int colorRed;
    private final int colorGreen;

    private final ABIType<Object> elementType;
    private final String elementCanonicalTypeString;

    private final int elementCategory;

    private Integer elementUnderEditPosition;

    public ArrayEntryAdapter(Activity activity, ABIType<Object> elementType, int elementCategory, List<Object> list) {
        this.activity = activity;
        this.colorRed = activity.getResources().getColor(R.color.colorAccent);
        this.colorGreen = activity.getResources().getColor(R.color.colorPrimary);
        this.elementType = elementType;
        this.elementCanonicalTypeString = elementType.getCanonicalType();
        this.elementCategory = elementCategory;
        this.list = list;
    }

    @Override
    public TupleEntryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.argument_row, parent, false);

        setEditTextAttributes((EditText) itemView.findViewById(R.id.typeable_value), elementType);

        return new TupleEntryAdapter.ViewHolder(itemView);
    }

    static void setEditTextAttributes(EditText editText, ABIType<?> elementType) {
        switch (elementType.typeCode()) {
            case ABIType.TYPE_CODE_BYTE:
            case ABIType.TYPE_CODE_INT:
            case ABIType.TYPE_CODE_LONG:
            case ABIType.TYPE_CODE_BIG_INTEGER:
            case ABIType.TYPE_CODE_BIG_DECIMAL: {
                UnitType<?> ut = (UnitType<?>) elementType;
                editText.setInputType(ut.isUnsigned() ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_NUMBER_FLAG_SIGNED);
            }
            default: /* skip */
        }
    }

    @Override
    public void onBindViewHolder(TupleEntryAdapter.ViewHolder holder, int position) {

        final Object element = list.get(position);

        holder.type.setText(String.valueOf(position));

        switch (elementCategory) {
        case ArrayEntryFragment.CATEGORY_TUPLE:
            if(elementCanonicalTypeString.equals("()")) {
                list.set(holder.getAdapterPosition(), Tuple.EMPTY);
            } else {
                holder.editableValue.setOnClickListener(v -> {
                    elementUnderEditPosition = holder.getAdapterPosition();
                    EditorActivity.startSubtupleActivity(activity, elementCanonicalTypeString, false);
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
                    validateTypeable((String) list.get(position), holder.typeableValue, true);
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

                    validateTypeable(argString, holder.typeableValue, false);

                    list.set(holder.getAdapterPosition(), argString);
                }
            };
            holder.typeableValue.addTextChangedListener(holder.textWatcher);

            holder.editableValue.setVisibility(View.INVISIBLE);
            holder.typeableValue.setVisibility(View.VISIBLE);

            setHint(holder.typeableValue, elementType);
            break;
        default:
            throw new Error();
        }

        if(elementCategory == ArrayEntryFragment.CATEGORY_TYPEABLE) {
            validateTypeable((String) element, holder.typeableValue, true);
        } else {
            validateEditable(element, holder.editableValue);
        }
    }

    private void validateTypeable(String valString, EditText typeableValueView, boolean setText) {
        boolean valid = valString != null;
        if(valid) {
            try {
                final boolean isArray = elementType.typeCode() == ABIType.TYPE_CODE_ARRAY;
                final boolean isString = isArray && ((ArrayType<?, ?>) elementType).isString();
                Object val = ArrayEntryFragment.parseElement(elementType, valString, isString, isArray);
                elementType.validate(val);
                if(setText) {
                    typeableValueView.setText(valString);
                }
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException iae) {
                valid = false;
            }
        }

        System.out.println("validateTypeable(" + setText + ") = " + valid);

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

    synchronized void returnEditedObject(Object array) {
        list.set(elementUnderEditPosition, array);
        notifyItemChanged(elementUnderEditPosition);
    }
}