package com.paraiso.database.util

import com.mongodb.client.model.Filters.expr
import com.paraiso.domain.util.Constants.ID
import org.bson.Document
import org.bson.conversions.Bson
import kotlin.reflect.KProperty1

fun <T : Any> eqId(
    field1: KProperty1<T, *>
): Bson = expr(Document("\$eq", listOf("\$${field1.name}", "\$$ID")))

fun <T : Any> fieldsEq(
    field1: KProperty1<T, *>,
    field2: KProperty1<T, *>
): Bson = expr(Document("\$eq", listOf("\$${field1.name}", "\$${field2.name}")))
