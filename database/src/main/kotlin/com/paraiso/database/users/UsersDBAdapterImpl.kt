package com.paraiso.database.users

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.users.User
import com.paraiso.domain.users.UsersDBAdapter

class UsersDBAdapterImpl(database: MongoDatabase): UsersDBAdapter {
    private val collection = database.getCollection("users", User::class.java)
}