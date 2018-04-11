package com.tachyonlabs.practicetodoapp.activities;

import com.tachyonlabs.practicetodoapp.R;
import com.tachyonlabs.practicetodoapp.data.TodoListContract;
import com.tachyonlabs.practicetodoapp.databinding.ActivityAddOrEditTaskBinding;
import com.tachyonlabs.practicetodoapp.models.TodoTask;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Calendar;

public class AddOrEditTaskActivity extends AppCompatActivity {
    private static final String TAG = AddOrEditTaskActivity.class.getSimpleName();
    private ActivityAddOrEditTaskBinding mBinding;
    private int todoId = -1;
    private String mAddOrEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_add_or_edit_task);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        long dueDate;

        Bundle bundle = getIntent().getExtras();
        mAddOrEdit = bundle.getString(getString(R.string.intent_adding_or_editing_key));

        setTitle(mAddOrEdit);
        if (savedInstanceState == null) {
            if (mAddOrEdit.equals(getString(R.string.add_new_task))) {
                // when adding a task, default to high priority and no due date
                mBinding.rbHighPriority.setChecked(true);
                mBinding.rbNoDueDate.setChecked(true);
                mBinding.btnAddOrUpdateTask.setText(R.string.add_task);
            } else {
                mBinding.btnAddOrUpdateTask.setText(R.string.update_task);
                TodoTask todoTaskToAddOrEdit = bundle.getParcelable(getString(R.string.intent_todo_key));
                todoId = todoTaskToAddOrEdit.getId();
                mBinding.etTaskDescription.setText(todoTaskToAddOrEdit.getDescription());

                selectPriorityRadioButton(todoTaskToAddOrEdit.getPriority());

                dueDate = todoTaskToAddOrEdit.getDueDate();
                Log.d(TAG, "Due date in millis " + dueDate);
                if (dueDate == Long.MAX_VALUE) {
                    mBinding.rbNoDueDate.setChecked(true);
                } else {
                    mBinding.rbSelectDueDate.setChecked(true);
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(dueDate);
                    mBinding.dpDueDate.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
                }
            }
        } else {
            mBinding.etTaskDescription.setText(savedInstanceState.getString(getString(R.string.task_description_key)));
            selectPriorityRadioButton(savedInstanceState.getInt(getString(R.string.priority_key)));
            boolean noDueDate = savedInstanceState.getBoolean(getString(R.string.no_due_date_key));
            if (noDueDate) {
                mBinding.rbNoDueDate.setChecked(true);
            } else {
                mBinding.rbSelectDueDate.setChecked(true);
            }
            mBinding.dpDueDate.updateDate(savedInstanceState.getInt(getString(R.string.year_key)),
                    savedInstanceState.getInt(getString(R.string.month_key)),
                    savedInstanceState.getInt(getString(R.string.day_key)));
        }
    }

    private void selectPriorityRadioButton(int priority) {
        switch (priority) {
            case 0:
                mBinding.rbHighPriority.setChecked(true);
                break;
            case 1:
                mBinding.rbMediumPriority.setChecked(true);
                break;
            case 2:
                mBinding.rbLowPriority.setChecked(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // save values on device rotation
        outState.putString(getString(R.string.task_description_key), mBinding.etTaskDescription.getText().toString());
        int priority = 0;
        if (mBinding.rbMediumPriority.isChecked()) {
            priority = 1;
        } else if (mBinding.rbLowPriority.isChecked()) {
            priority = 2;
        }

        outState.putInt(getString(R.string.priority_key), priority);
        outState.putBoolean(getString(R.string.no_due_date), mBinding.rbNoDueDate.isChecked());
        outState.putInt(getString(R.string.year_key), mBinding.dpDueDate.getYear());
        outState.putInt(getString(R.string.month_key), mBinding.dpDueDate.getMonth());
        outState.putInt(getString(R.string.day_key), mBinding.dpDueDate.getDayOfMonth());
        super.onSaveInstanceState(outState);
    }

    public void addOrUpdateTask(View view) {
        String description = mBinding.etTaskDescription.getText().toString().trim();
        int priority = 0;
        long dueDate = Long.MAX_VALUE;

        if (description.equals("")) {
            Toast.makeText(this, getString(R.string.description_cannot_be_empty), Toast.LENGTH_LONG).show();
        } else {
            // get the priority setting
            if (mBinding.rbMediumPriority.isChecked()) {
                priority = 1;
            } else if (mBinding.rbLowPriority.isChecked()) {
                priority = 2;
            }

            // get the due date, if one has been selected
            if (mBinding.rbSelectDueDate.isChecked()) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(mBinding.dpDueDate.getYear(), mBinding.dpDueDate.getMonth(), mBinding.dpDueDate.getDayOfMonth());
                dueDate = calendar.getTimeInMillis();
            }
            TodoTask todoTask = new TodoTask(description, priority, dueDate, todoId);

            insertOrUpdate(todoTask);

            Intent returnIntent = new Intent();
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
    }

    private void insertOrUpdate(TodoTask todoTask) {
        // I used to have this functionality in TodoListActivity's onActivityResult method, but
        // then I couldn't reach it when editing a task directly from the App Widget
        String id = String.valueOf(todoTask.getId());
        ContentValues contentValues = new ContentValues();
        contentValues.put(TodoListContract.TodoListEntry.COLUMN_DESCRIPTION, todoTask.getDescription());
        contentValues.put(TodoListContract.TodoListEntry.COLUMN_PRIORITY, todoTask.getPriority());
        contentValues.put(TodoListContract.TodoListEntry.COLUMN_DUE_DATE, todoTask.getDueDate());

        if (mAddOrEdit.equals(getString(R.string.add_new_task))) {
            getContentResolver().insert(TodoListContract.TodoListEntry.CONTENT_URI, contentValues);
        } else {
            Uri uri = TodoListContract.TodoListEntry.CONTENT_URI.buildUpon().appendPath(id).build();
            getContentResolver().update(uri, contentValues, "_id=?", new String[]{id});
        }
    }
}