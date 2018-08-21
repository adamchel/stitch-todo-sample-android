package com.mongodb.todosample.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.todosample.R;
import com.mongodb.todosample.Utils;
import com.mongodb.todosample.model.TodoList;
import com.mongodb.todosample.model.objects.TodoItem;

import org.bson.types.ObjectId;

import java.util.List;

/**
 * This is the adapter that uses the TodoList model to construct the necessary views in a recycler
 * view that lists the tasks.
 */
public class TodoListAdapter extends RecyclerView.Adapter<TodoListAdapter.TodoItemViewHolder> implements TodoList.Listener {

  private Context _context;
  private TodoList _todoList;

  public TodoListAdapter(final Context context, final TodoList todoList) {
    this._context = context;
    this._todoList = todoList;
  }

  /**
   * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
   * an item.
   * <p>
   * This new ViewHolder should be constructed with a new View that can represent the items
   * of the given type. You can either create a new View manually or inflate it from an XML
   * layout file.
   * <p>
   * The new ViewHolder will be used to display items of the adapter using
   * {@link #onBindViewHolder(ViewHolder, int, List)}. Since it will be re-used to display
   * different items in the data set, it is a good idea to cache references to sub views of
   * the View to avoid unnecessary {@link View#findViewById(int)} calls.
   *
   * @param parent   The ViewGroup into which the new View will be added after it is bound to
   *                 an adapter position.
   * @param viewType The view type of the new View.
   * @return A new ViewHolder that holds a View of the given view type.
   * @see #getItemViewType(int)
   * @see #onBindViewHolder(ViewHolder, int)
   */
  @NonNull
  @Override
  public TodoItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    final View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.todo_item, parent, false);

    return new TodoItemViewHolder(v);
  }

  /**
   * Called by RecyclerView to display the data at the specified position. This method should
   * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
   * position.
   * <p>
   * Note that unlike {@link ListView}, RecyclerView will not call this method
   * again if the position of the item changes in the data set unless the item itself is
   * invalidated or the new position cannot be determined. For this reason, you should only
   * use the <code>position</code> parameter while acquiring the related data item inside
   * this method and should not keep a copy of it. If you need the position of an item later
   * on (e.g. in a click listener), use {@link ViewHolder#getAdapterPosition()} which will
   * have the updated adapter position.
   * <p>
   * Override {@link #onBindViewHolder(ViewHolder, int, List)} instead if Adapter can
   * handle efficient partial bind.
   *
   * @param holder   The ViewHolder which should be updated to represent the contents of the
   *                 item at the given position in the data set.
   * @param position The position of the item within the adapter's data set.
   */
  @Override
  public void onBindViewHolder(@NonNull TodoItemViewHolder holder, int position) {
    final TodoItem item = _todoList.getItems().get(position);

    holder.taskTextView.setText(item.getTask());
    holder.taskCheckbox.setChecked(item.getChecked());
  }

  /**
   * Returns the total number of items in the data set held by the adapter.
   *
   * @return The total number of items in this adapter.
   */
  @Override
  public int getItemCount() {
    return _todoList.getItems().size();
  }

  /**
   * The method to call when the list has been successfully modified. This is not guaranteed
   * to be called on the main thread.
   */
  @Override
  public void onListModified() {
    this.notifyDataSetChanged();
  }

  class TodoItemViewHolder extends RecyclerView.ViewHolder
          implements View.OnClickListener,
          View.OnLongClickListener {
    final TextView taskTextView;
    final CheckBox taskCheckbox;

    TodoItemViewHolder(final View view) {
      super(view);
      taskTextView = view.findViewById(R.id.tv_task);
      taskCheckbox = view.findViewById(R.id.cb_todo_checkbox);
      taskCheckbox.setFocusable(false);
      taskCheckbox.setClickable(false);

      // Set listeners
      view.setOnClickListener(this);
      view.setOnLongClickListener(this);
    }

    @Override
    public void onClick(final View view) {
      final TodoItem item = _todoList.getItems().get(getAdapterPosition());

      _todoList.updateItemChecked(item.getId(), !item.getChecked()).addOnCompleteListener(new OnCompleteListener<Void>() {
        @Override
        public void onComplete(@NonNull Task<Void> task) {
          if(!task.isSuccessful()) {
            Toast.makeText(
                    TodoListAdapter.this._context,
                    "Could not update checked status.", Toast.LENGTH_SHORT
            ).show();
          } else {
            taskCheckbox.setChecked(!item.getChecked());
          }
        }
      });
    }

    @Override
    public boolean onLongClick(final View view) {
      final TodoItem item = _todoList.getItems().get(getAdapterPosition());
      _showEditItemDialog(item.getId(), item.getTask());
      return true;
    }
  }

  private void _showEditItemDialog(final ObjectId itemId, final String currentTask) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this._context);
    builder.setTitle("Edit Item");

    final LayoutInflater inflater =
            (LayoutInflater) this._context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    if (inflater == null) {
      Toast.makeText(_context, "Could not open item editing dialog.", Toast.LENGTH_SHORT).show();
    }

    final View view = inflater.inflate(R.layout.edit_item_dialog, null);
    final EditText input = view.findViewById(R.id.et_todo_item_task);

    input.setText(currentTask);
    input.setSelection(input.getText().length());

    builder.setView(view);

    // Set up the buttons
    builder.setPositiveButton(
            "Update",
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(final DialogInterface dialog, final int which) {
                Utils.displayToastIfTaskFails(
                        _context,
                        _todoList.updateItemTask(itemId, input.getText().toString()),
                        "Failed to update task. Try again later."
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
