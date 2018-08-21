package com.mongodb.todosample.model.objects;

/*
 * Copyright 2018-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

/**
 * TodoItem represents a task that would exist in a TodoList. MongoDB POJO annotations are included
 * so TodoItem objects can be directly inserted and retrieved from a MongoDB collection instance.
 * See http://mongodb.github.io/mongo-java-driver/3.6/driver/getting-started/quick-start-pojo/
 */
public class TodoItem {
  public static final String ID_KEY = "_id";
  public static final String OWNER_KEY = "owner_id";
  public static final String TASK_KEY = "task";
  public static final String CHECKED_KEY = "checked";
  public static final String DONE_DATE_KEY = "done_date";

  @NonNull
  private ObjectId id;

  @Nullable
  private String ownerId;

  @NonNull
  private String task;

  @NonNull
  private Boolean checked;

  @Nullable
  private Date doneDate;

  /**
   * Constructor for TodoItem that is used when reading from BSON.
   */
  @BsonCreator
  public TodoItem(
          @NonNull @BsonId final ObjectId id,
          @BsonProperty(OWNER_KEY) final String ownerId,
          @BsonProperty(TASK_KEY) final String task,
          @BsonProperty(CHECKED_KEY) final Boolean checked,
          @BsonProperty(DONE_DATE_KEY) final Date doneDate) {
    this.id = id;
    this.ownerId = ownerId;
    this.task = task;
    if (checked == null) {
      this.checked = false;
    } else {
      this.checked = checked;
    }

    if (doneDate == null) {
      this.doneDate = new Date();
    } else {
      this.doneDate = doneDate;
    }
  }

  public TodoItem(
          @NonNull final String task) {
    this.id = ObjectId.get();
    this.task = task;
    this.checked = false;
    this.doneDate = new Date();
  }

  // Getters

  @NonNull @BsonId
  public ObjectId getId() {
    return id;
  }

  @Nullable @BsonProperty(OWNER_KEY)
  public String getOwnerId() { return ownerId; }

  @NonNull @BsonProperty(CHECKED_KEY)
  public Boolean getChecked() {
    return checked;
  }

  @NonNull @BsonProperty(TASK_KEY)
  public String getTask() {
    return task;
  }

  @Nullable @BsonProperty(DONE_DATE_KEY)
  public Date getDoneDate() {
    return new Date(doneDate.getTime());
  }

  // Setters

  @BsonIgnore
  public void setOwnerId(final String ownerId) {
    this.ownerId = ownerId;
  }
}
