package com.mongodb.todosample;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.mongodb.todosample.adapters.TodoListAdapter;
import com.mongodb.todosample.model.TodoList;
import com.mongodb.todosample.model.objects.TodoItem;

public class MainActivity extends AppCompatActivity {

  private TodoList _todoList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    // Set up recycler view for to-do items
    final RecyclerView todoRecyclerView = findViewById(R.id.rv_todo_items);
    final RecyclerView.LayoutManager todoLayoutManager = new LinearLayoutManager(this);
    todoRecyclerView.setLayoutManager(todoLayoutManager);

    // Set up model and its adapter
    _todoList = new TodoList(this);
    final TodoListAdapter todoListAdapter = new TodoListAdapter(this, _todoList);

    // Register the adapter to listen for changes to the task list, and set the adapter of the
    // recycler view.
    _todoList.registerListener(todoListAdapter);
    todoRecyclerView.setAdapter(todoListAdapter);

    if(!_todoList.isLoggedIn()) {
      // TODO: In the future, you could attach the model's login functionality to a login page.
      Utils.displayToastIfTaskFails(
              this,
              _todoList.loginAnonymously(),
              "Failed to login. Try again later."
      );
    } else {
      Utils.displayToastIfTaskFails(
              this,
              _todoList.refresh(),
              "Failed to refresh items. Try again later."
      );
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    final MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.todo_menu, menu);

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    switch (item.getItemId()) {
      case R.id.add_todo_item_action:
        _showAddItemDialog();
        return true;
      case R.id.clear_checked_action:
        _todoList.clearCheckedItems();
        return true;
      case R.id.clear_all_action:
        _todoList.clearAllItems();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void _showAddItemDialog() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Add Item");

    final View view = getLayoutInflater().inflate(R.layout.edit_item_dialog, null);
    final EditText input = view.findViewById(R.id.et_todo_item_task);

    builder.setView(view);

    // Set up the buttons
    builder.setPositiveButton(
            "Add",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(final DialogInterface dialog, final int which) {
                Utils.displayToastIfTaskFails(
                        MainActivity.this,
                        _todoList.addItem(new TodoItem(input.getText().toString())),
                        "Failed to add task. Try again later."
                );
              }
            });
    builder.setNegativeButton(
            "Cancel",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(final DialogInterface dialog, final int which) {
                dialog.cancel();
              }
            });

    builder.show();
  }
}
