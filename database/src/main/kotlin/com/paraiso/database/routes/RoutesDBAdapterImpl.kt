package com.paraiso.database.routes

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.paraiso.domain.routes.Route
import com.paraiso.domain.routes.RoutesDBAdapter

class RoutesDBAdapterImpl(database: MongoDatabase): RoutesDBAdapter {
    private val collection = database.getCollection("routes", Route::class.java)
}