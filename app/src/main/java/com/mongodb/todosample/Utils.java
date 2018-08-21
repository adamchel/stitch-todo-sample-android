package com.mongodb.todosample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;

public class Utils {
  public static void displayToastIfTaskFails(
          final Context context,
          final Task task,
          final String errorMessage
  ) {
    task.addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        Log.d(Utils.class.getName(), errorMessage + ": " + e.getMessage());
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
      }
    });
  }
}
