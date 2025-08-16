package com.paraiso.database.util

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.conversions.Bson
import kotlin.reflect.KProperty1

fun <T : Any> fieldsEq(
    field1: KProperty1<T, *>,
    field2: KProperty1<T, *>
): Bson = Filters.expr(Document("\$eq", listOf("\$${field1.name}", "\$${field2.name}")))