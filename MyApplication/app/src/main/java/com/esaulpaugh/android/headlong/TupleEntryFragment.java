package com.esaulpaugh.android.headlong;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.StackableType;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.android.headlong.EditorActivity.ENCODED_TUPLE_BYTES;

public class TupleEntryFragment extends Fragment implements EntryFragment {

    static class Triple {

        final StackableType<?> stackableType;
        final Object object;

        Triple(StackableType<?> stackableType, Object object) {
            this.stackableType = stackableType;
            this.object = object;
        }
    }

    static final String ARG_FOR_SUBTUPLE = "for_subtuple";
    static final String ARG_SUBTUPLE_TYPE_STRING = "subtuple_type_string";

    private boolean forSubtuple;
    private String subtupleTypeString;

    private String functionSignature;
    private Tuple masterTuple, subtuple;

    private TupleEntryAdapter adapter;

    public TupleEntryFragment() {
        // Required empty public constructor
    }

    public static TupleEntryFragment newInstance() {
        return newInstance(false, null);
    }

    public static TupleEntryFragment newInstance(boolean subtuple, String subtupleTypeString) {
        TupleEntryFragment fragment = new TupleEntryFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_FOR_SUBTUPLE, subtuple);
        args.putString(ARG_SUBTUPLE_TYPE_STRING, subtupleTypeString);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            forSubtuple = getArguments().getBoolean(ARG_FOR_SUBTUPLE);
            subtupleTypeString = getArguments().getString(ARG_SUBTUPLE_TYPE_STRING);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_tuple_entry, container, false);

        Button returnTuple = (Button) view.findViewById(R.id.return_tuple);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        EditText enterSignature = (EditText) view.findViewById(R.id.enter_signature);
        TextView tupleTypeString = (TextView) view.findViewById(R.id.tuple_type_string);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        List<Triple> listElements = new ArrayList<>();

        adapter = new TupleEntryAdapter(getActivity(), listElements);

        recyclerView.setAdapter(adapter);

        if (forSubtuple) {
            enterSignature.setVisibility(View.GONE);
            tupleTypeString.setVisibility(View.VISIBLE);
            tupleTypeString.setText(subtupleTypeString);

            try {
                TupleType tt = TupleType.parse(subtupleTypeString);
                listElements.clear();
                for(StackableType<?> stackableType : tt.getElementTypes()) {
                    listElements.add(new Triple(stackableType, null));
                }
                adapter.notifyDataSetChanged();

            } catch (ParseException pe) {
                Toast.makeText(getActivity(), pe.getMessage(), Toast.LENGTH_SHORT).show();
            }
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
                    try {
                        Function f = new Function(s.toString());

                        TupleType tt = f.getTupleType();

                        listElements.clear();

                        for(StackableType<?> stackableType : tt.getElementTypes()) {
                            listElements.add(new Triple(stackableType, null));
                        }

                        adapter.notifyDataSetChanged();

                    } catch (ParseException pe) {
                        listElements.clear();
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        returnTuple.setOnClickListener(v -> {

// TODO handle race conditions, button spam

            Object[] args = new Object[listElements.size()];
            int i = 0;
            for (Triple triple : listElements) {
                args[i++] = triple.object;
            }

            if (forSubtuple) {
                this.subtuple = new Tuple(args);
                Intent intent = new Intent();
                try {
                    intent.putExtra(TupleEntryFragment.ARG_SUBTUPLE_TYPE_STRING, subtupleTypeString);

                    TupleType tt = TupleType.parse(subtupleTypeString);
                    byte[] tupleBytes = tt.encode(subtuple).array();

                    intent.putExtra(ENCODED_TUPLE_BYTES, tupleBytes);

                    getActivity().setResult(EditorActivity.CODE_SUBTUPLE, intent);
                    getActivity().finish();

                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getClass().getSimpleName() + " " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                this.masterTuple = new Tuple(args);
                this.functionSignature = enterSignature.getText().toString();
            }
        });

        return view;
    }

    public String getFunctionSignature() {
        return functionSignature;
    }

    public Tuple getMasterTuple() {
        return masterTuple;
    }

    @Override
    public void returnEditedObject(Object obj, boolean defaultVal) {
        adapter.returnEditedObject(obj);
    }
}
