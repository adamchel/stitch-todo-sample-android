package com.mongodb.todosample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.mongodb.todosample.model.Authenticator;

public class LoginActivity extends AppCompatActivity {
  private static final String TAG = LoginActivity.class.getName();
  private static final int REQUEST_SIGNUP = 0;

  private Authenticator _authenticator;

  private EditText _emailText;
  private EditText _passwordText;
  private Button _loginButton;
  private TextView _anonLoginLink;


  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_login);

    _authenticator = new Authenticator(this);

    _emailText = findViewById(R.id.input_email);
    _passwordText = findViewById(R.id.input_password);
    _loginButton = findViewById(R.id.btn_login);
    _anonLoginLink = findViewById(R.id.link_anon_login);

    _loginButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        login();
      }
    });

    _anonLoginLink.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Utils.displayToastIfTaskFails(
                LoginActivity.this,
                _authenticator.loginAnonymously(),
                "Failed to log in anonymously, try again later."
        ).addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            onLoginSuccess();
          }
        });

      }
    });
  }

  @Override
  public void onBackPressed() {
    // disable going back to the MainActivity
    moveTaskToBack(true);
  }

  public void login() {
    Log.d(TAG, "Login");

    if (!validate()) {
      onLoginFailed();
      return;
    }

    _loginButton.setEnabled(false);

    final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage("Authenticating...");
    progressDialog.show();

    String email = _emailText.getText().toString();
    String password = _passwordText.getText().toString();

    this._authenticator.login(email, password).addOnCompleteListener(new OnCompleteListener<Void>() {
      @Override
      public void onComplete(@NonNull Task<Void> task) {
        progressDialog.dismiss();
        if (task.isSuccessful()) {
          onLoginSuccess();
        } else {
          onLoginFailed(task.getException().getMessage());
        }
      }
    });

//
//    new android.os.Handler().postDelayed(
//            new Runnable() {
//              public void run() {
//                // On complete call either onLoginSuccess or onLoginFailed
//                onLoginSuccess();
//                // onLoginFailed();
//                progressDialog.dismiss();
//              }
//            }, 3000);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_SIGNUP) {
      if (resultCode == RESULT_OK) {

        // TODO: Implement successful signup logic here
        // By default we just finish the Activity and log them in automatically
        this.finish();
      }
    }
  }

  public void onLoginSuccess() {
    _loginButton.setEnabled(true);
    finish();
  }

  public void onLoginFailed() {
    onLoginFailed(null);
  }

  public void onLoginFailed(final String message) {
    if (message == null || message == "") {
      Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

    _loginButton.setEnabled(true);
  }

  public boolean validate() {
    boolean valid = true;

    String email = _emailText.getText().toString();
    String password = _passwordText.getText().toString();

    if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      _emailText.setError("enter a valid email address");
      valid = false;
    } else {
      _emailText.setError(null);
    }

    if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
      _passwordText.setError("between 4 and 10 alphanumeric characters");
      valid = false;
    } else {
      _passwordText.setError(null);
    }

    return valid;
  }
}
