package com.healthdispatch.data.cloud

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreHealthRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun pushRecords(collection: String, jsonPayloads: List<String>): Result<Int> {
        if (jsonPayloads.isEmpty()) return Result.success(0)

        val userId = firebaseAuth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            val batch = firestore.batch()
            val collectionRef = firestore.collection("users/$userId/$collection")

            for (payload in jsonPayloads) {
                val docRef = collectionRef.document()
                val data = jsonToMap(payload)
                batch.set(docRef, data)
            }

            batch.commit().await()
            Result.success(jsonPayloads.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun jsonToMap(json: String): Map<String, Any> {
        val element = Json.parseToJsonElement(json).jsonObject
        return element.mapValues { (_, value) ->
            val primitive = value.jsonPrimitive
            when {
                primitive.longOrNull != null -> primitive.long
                primitive.doubleOrNull != null -> primitive.double
                primitive.booleanOrNull != null -> primitive.boolean
                else -> primitive.content
            }
        }
    }
}
