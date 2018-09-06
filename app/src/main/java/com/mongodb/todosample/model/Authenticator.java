package com.mongodb.todosample.model;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.mongodb.stitch.core.auth.providers.userpassword.UserPasswordCredential;
import com.mongodb.todosample.Utils;

public class Authenticator {

  // Stitch specific fields
  private StitchAppClient _stitchClient;

  public Authenticator(final Context context) {
    this._stitchClient = Utils.getStitchAppClient(context);
  }

  /**
   * Logs into this TodoList anonymously, via Stitch under the hood.
   * @return
   */
  public Task<Void> loginAnonymously() {
    if(_stitchClient.getAuth().isLoggedIn()) {
      return Tasks.forException(new IllegalStateException("Must be logged out first."));
    }

    return _stitchClient.getAuth().loginWithCredential(
            new AnonymousCredential()
    ).continueWith(new Continuation<StitchUser, Void>() {
      @Override
      public Void then(@NonNull Task<StitchUser> task) throws Exception {
        return null;
      }
    });
  }

  /**
   * Logs into this TodoList anonymously, via Stitch under the hood.
   * @return
   */
  public Task<Void> login(String email, String password) {
    if(_stitchClient.getAuth().isLoggedIn()) {
      return Tasks.forException(new IllegalStateException("Must be logged out first."));
    }

    return _stitchClient.getAuth().loginWithCredential(
            new UserPasswordCredential(email, password)
    ).continueWith(new Continuation<StitchUser, Void>() {
      @Override
      public Void then(@NonNull Task<StitchUser> task) throws Exception {
        if (task.isSuccessful()) {
          return null;
        } else if (task.getException() != null) {
          throw task.getException();
        }
        throw new IllegalStateException();
      }
    });
  }
}
