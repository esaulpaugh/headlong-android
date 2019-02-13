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

import com.esaulpaugh.headlong.abi.ArrayType;
import com.esaulpaugh.headlong.abi.StackableType;
import com.esaulpaugh.headlong.abi.Tuple;

import java.util.List;

import static com.esaulpaugh.android.headlong.TupleEntryFragment.Triple;

public class TupleEntryAdapter extends RecyclerView.Adapter<TupleEntryAdapter.ViewHolder> {

    private Activity activity;
    private List<Triple> list;

    private int colorRed, colorGreen;

    private int elementUnderEditPosition;

    public TupleEntryAdapter(Activity activity, List<Triple> list) {
        this.activity = activity;
        this.list = list;
        this.colorRed = activity.getResources().getColor(R.color.colorAccent);
        this.colorGreen = activity.getResources().getColor(R.color.colorPrimary);
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

        final Triple triple = list.get(position);

        final String canonical = triple.stackableType.getCanonicalType();

        holder.type.setText(canonical);

        holder.typeableValue.setText("");

        if(canonical.startsWith("(") && canonical.endsWith(")")) {

            if(canonical.equals("()")) {
                list.set(holder.getAdapterPosition(), new Triple(triple.stackableType, Tuple.EMPTY));
            } else {
                holder.editableValue.setOnClickListener(v -> {
                    elementUnderEditPosition = holder.getAdapterPosition();
                    EditorActivity.startSubtupleActivity(activity, canonical);
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

                @SuppressWarnings("unchecked")
                @Override
                public void afterTextChanged(Editable s) {

                    final String argString = s.toString();

                    Triple newTriple = list.get(holder.getAdapterPosition());

                    Object obj;
                    if (newTriple.stackableType instanceof ArrayType && ((ArrayType<StackableType<?>, ?>) newTriple.stackableType).isString()) {
                        obj = argString;
                    } else {
                        try {
                            obj = newTriple.stackableType.parseArgument(argString);
                        } catch (IllegalArgumentException iae) {
                            obj = null;
                        } catch (UnsupportedOperationException uoe) {
                            obj = null;
                        }
                    }

                    Triple newNewTriple = new Triple(newTriple.stackableType, obj);

                    list.set(holder.getAdapterPosition(), newNewTriple);

                    validate(newNewTriple, holder.typeableValue);
                }
            };
            holder.typeableValue.addTextChangedListener(holder.textWatcher);
            validate(triple, holder.typeableValue);
            holder.editableValue.setVisibility(View.INVISIBLE);
            holder.typeableValue.setVisibility(View.VISIBLE);
        }
    }

    private void validate(Triple triple, View valueView) {
        boolean valid = triple.object != null;
        if(valid) {
            try {
                triple.stackableType.validate(triple.object);
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

    public void returnEditedObject(Object obj) {

        Triple existing = list.get(elementUnderEditPosition);
        list.set(elementUnderEditPosition, new Triple(existing.stackableType, obj));
        notifyItemChanged(elementUnderEditPosition);
    }
}