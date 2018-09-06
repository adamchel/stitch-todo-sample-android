package com.mongodb.todosample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.core.StitchAppClientConfiguration;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

public class Utils {
  public static <T> Task<T> displayToastIfTaskFails(
          final Context context,
          final Task<T> task,
          final String errorMessage
  ) {
    return task.continueWithTask(new Continuation<T, Task<T>>() {
      @Override
      public Task<T> then(@NonNull Task<T> task) {
        if (!task.isSuccessful()) {
          if (task.getException() != null) {
            Log.d(Utils.class.getName(), errorMessage + ": " + task.getException().getMessage());
            Toast.makeText(context, errorMessage + ": " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            return Tasks.forException(task.getException());
          }
          throw new IllegalStateException();
        } else {
          return Tasks.forResult(task.getResult());
        }
      }
    });
  }

  public static StitchAppClient getStitchAppClient(final Context context){
    Stitch.initialize(context);

    final String stitchClientAppId = context.getString(R.string.todo_list_stitch_client_app_id);

    if (!Stitch.hasAppClient(stitchClientAppId)) {

      // Set up codecs that will allow us to create a MongoDB collection of TodoItem objects.
      CodecProvider todoListCodecProvider = PojoCodecProvider
              .builder()
              .register("com.mongodb.todosample.model.objects")
              .build();

      // Initialize the Stitch app client for the first time.
      Stitch.initializeAppClient(
              stitchClientAppId,
              new StitchAppClientConfiguration.Builder()
                      .withCodecRegistry(fromProviders(todoListCodecProvider))
                      .withBaseUrl("https://c89dff89.ngrok.io")
                      .build()
      );
    }

    return Stitch.getAppClient(stitchClientAppId);
  }
}
