package com.mongodb.todosample.model;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.StitchAppClientConfiguration;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;

import com.mongodb.todosample.R;
import com.mongodb.todosample.model.objects.TodoItem;

import org.bson.Document;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * This is the model for the application. It contains no Android UI-specific logic, but it contains
 * all the logic of an authenticated task list that can be accessed via Stitch. It should only
 * expose logic that the Activity (controller) needs to see. For that reason, it does not publicly
 * expose any Stitch-specific classes.
 */
public class TodoList {
  public static final String TODO_LIST_DATABASE = "todo";
  public static final String TODO_LIST_COLLECTION = "items";

  // Stitch specific fields
  private String                          _stitchClientAppId;
  private StitchAppClient                 _stitchClient;
  private RemoteMongoCollection<TodoItem> _remoteTodoListCollection;

  // General fields
  private final List<TodoItem> _cachedList;
  private final List<Listener> _listeners;
  // in the future, there might be also be a local MongoDB collection, or synced collection,
  // and this class would be responsible for keeping a local and remote collection in sync.


  public TodoList(final Context context) {
    this._stitchClientAppId = context.getString(R.string.todo_list_stitch_client_app_id);
    this._initializeStitch(context);
    this._cachedList = new ArrayList<>();
    this._listeners = new ArrayList<>();
  }

  /**
   * Initializes the _stitchClient field, calling Stitch's static client initialization functions
   * if the client has never been initialized before.
   */
  private synchronized void _initializeStitch(Context context) {
    Stitch.initialize(context);

    if (!Stitch.hasAppClient(_stitchClientAppId)) {

      // Set up codecs that will allow us to create a MongoDB collection of TodoItem objects.
      CodecProvider todoListCodecProvider = PojoCodecProvider
              .builder()
              .register("com.mongodb.todosample.model.objects")
              .build();

      // Initialize the Stitch app client for the first time.
      Stitch.initializeAppClient(
              _stitchClientAppId,
              new StitchAppClientConfiguration.Builder()
                .withCodecRegistry(fromProviders(todoListCodecProvider))
                .build()
              );
    }

    _stitchClient = Stitch.getAppClient(_stitchClientAppId);
    _remoteTodoListCollection = _stitchClient
            .getServiceClient(RemoteMongoClient.factory, "mongodb-atlas")
            .getDatabase(TODO_LIST_DATABASE)
            .getCollection(TODO_LIST_COLLECTION, TodoItem.class);
  }

  /**
   * Returns whether or not a user is currently logged into the TodoList.
   * @return whether or not a user is currently logged into the TodoList.
   */
  public Boolean isLoggedIn() {
    return _stitchClient.getAuth().isLoggedIn();
  }


  /**
   * Adds an item to the task list.
   * @param item A TodoItem to add to the task list. id and ownerId in the item do not need to be
   *             set as they will be automatically set when inserted into the MongoDB collection.
   * @return A Task that completes when the item is successfully added. Triggers a refresh (and
   * subsequent listener notification, but that refresh and listener notification may happen
   * after the task is completed.
   */
  public Task<Void> addItem(final TodoItem item) {
    final StitchUser authedUser = _stitchClient.getAuth().getUser();
    if(!isLoggedIn() || authedUser == null) {
      return Tasks.forException(new IllegalStateException("Must be logged in to add task."));
    }

    item.setOwnerId(authedUser.getId());

    return _executeThenRefresh(_remoteTodoListCollection.insertOne(item));
  }

  public Task<Void> updateItemChecked(final ObjectId itemId, final boolean isChecked) {
    if(!isLoggedIn()) {
      return Tasks.forException(new IllegalStateException("Must be logged in to add task."));
    }

    final Document updateDoc =
            new Document("$set", new Document(TodoItem.CHECKED_KEY, isChecked));

    if (isChecked) {
      updateDoc.append("$currentDate", new Document(TodoItem.DONE_DATE_KEY, true));
    } else {
      updateDoc.append("$unset", new Document(TodoItem.DONE_DATE_KEY, ""));
    }

    return _executeThenRefresh(
            _remoteTodoListCollection.updateOne(new Document(TodoItem.ID_KEY, itemId), updateDoc));
  }

  public Task<Void> updateItemTask(final ObjectId itemId, final String newTask) {
    if(!isLoggedIn()) {
      return Tasks.forException(new IllegalStateException("Must be logged in to add task."));
    }

    final Document updateDoc =
            new Document("$set", new Document(TodoItem.TASK_KEY, newTask));

    return _executeThenRefresh(
            _remoteTodoListCollection.updateOne(new Document(TodoItem.ID_KEY, itemId), updateDoc));
  }

  public Task<Void> clearCheckedItems() {
    if(!isLoggedIn()) {
      return Tasks.forException(new IllegalStateException("Must be logged in to clear items."));
    }

    return _executeThenRefresh(
            _remoteTodoListCollection.deleteMany(new Document(TodoItem.CHECKED_KEY, true)));
  }

  public Task<Void> clearAllItems() {
    if(!isLoggedIn()) {
      return Tasks.forException(new IllegalStateException("Must be logged in to add task."));
    }

    return _executeThenRefresh(_remoteTodoListCollection.deleteMany(new Document()));
  }

  /**
   * Private helper method that runs a Task, ignores its result on success, and refreshes the
   * cached list, returning a Task that completes when the original Task completes. If the original
   * task fails, this returns a failing task with the reason for the failure.
   *
   * @param task The task to execute
   * @param <T> The result type of the original task.
   * @return a Task that completes when the original task completes, but before a refresh completes
   */
  private <T> Task<Void> _executeThenRefresh(Task<T> task) {
    return task.continueWithTask(new Continuation<T, Task<Void>>() {
      @Override
      public Task<Void> then(@NonNull Task<T> task) throws Exception {
        if (!task.isSuccessful()) {
          if (task.getException() != null) {
            throw task.getException();
          }
          throw new IllegalStateException();
        }

        TodoList.this.refresh();

        return Tasks.forResult(null);
      }
    });
  }

  /**
   * A listener interface that can be implemented to react to changes made to the task list,
   * remotely or otherwise. In the future, this listener could be called whenever an update to
   * the list is recognized via MongoDB Stitch Mobile Sync
   */
  public interface Listener {
    /**
     * The method to call when the list has been successfully modified. This is not guaranteed
     * to be called on the main thread.
     */
    void onListModified();
  }

  /**
   * Notifies all the registered listeners that something about the list may have changed.
   */
  private void _notifyListeners() {
    for (Listener listener : this._listeners) {
      listener.onListModified();
    }
  }

  /**
   * Registers a TodoList.Listener with this TodoList.
   * @param listener The listener to register with this list.
   */
  public void registerListener(final Listener listener) {
    this._listeners.add(listener);
  }

  /**
   * Refreshes the list
   * @return A task that completes when the list is refreshed. Any registered listeners are
   * automatically notified.
   */
  public Task<Void> refresh() {
    final StitchUser authedUser = _stitchClient.getAuth().getUser();
    if(!_stitchClient.getAuth().isLoggedIn() || authedUser == null) {
      return Tasks.forException(new IllegalStateException("Must be logged in to refresh list."));
    }

    final List<TodoItem> findResult = new ArrayList<>();
    return _remoteTodoListCollection.find(
            new Document(TodoItem.OWNER_KEY, authedUser.getId())
    ).into(findResult).continueWithTask(new Continuation<List<TodoItem>, Task<Void>>() {
      @Override
      public Task<Void> then(@NonNull Task<List<TodoItem>> task) throws Exception {
        if (!task.isSuccessful()) {
          if (task.getException() != null) {
            throw task.getException();
          }
          throw new IllegalStateException("Refreshing todo list failed for unknown reason.");
        }

        TodoList.this._cachedList.clear();
        TodoList.this._cachedList.addAll(findResult);
        TodoList.this._notifyListeners();

        return Tasks.forResult(null);
      }
    });
  }

  /**
   * Logs into this TodoList anonymously, via Stitch under the hood.
   * @return
   */
  public Task<Void> loginAnonymously() {
    if(_stitchClient.getAuth().isLoggedIn()) {
      return Tasks.forException(new IllegalStateException("Must be logged out first."));
    }

    return _executeThenRefresh(
            _stitchClient.getAuth().loginWithCredential(new AnonymousCredential()));
  }

  // You could now add login methods for other authentication providers, abstracting away the Stit

  /**
   * "Logs out" this task list by clearing the cached list of tasks, triggering a logout in Stitch,
   * and notifying the listeners of the
   */
  public void logout() {
    this._cachedList.clear();
    this._stitchClient.getAuth().logout().continueWith(new Continuation<Void, Void>() {
      @Override
      public Void then(@NonNull Task<Void> task) {
        TodoList.this._notifyListeners();
        return null;
      }
    });
  }

  /**
   * Retrieves the cached list of TodoItem objects that this TodoList holds.
   * @return a list of TodoItem objects
   */
  public List<TodoItem> getItems() {
    return _cachedList;
  }
}
