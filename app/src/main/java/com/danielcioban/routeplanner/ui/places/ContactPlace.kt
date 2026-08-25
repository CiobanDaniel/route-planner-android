package com.danielcioban.routeplanner.ui.places

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class PickedContact(
    val name: String,
    val address: String,
    val phone: String,
)

object ContactPlace {
    fun read(context: Context, contactUri: Uri): PickedContact? {
        val resolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
        )
        val parsed = runCatching {
            resolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val id = cursor.getLong(0)
                val name = cursor.getString(1).orEmpty()
                id to name
            }
        }.getOrNull() ?: return null
        val (contactId, name) = parsed
        val address = queryFirst(
            context = context,
            uri = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            column = ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
            contactId = contactId,
        )
        val phone = queryFirst(
            context = context,
            uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            column = ContactsContract.CommonDataKinds.Phone.NUMBER,
            contactId = contactId,
        )
        if (name.isBlank() && address.isBlank()) return null
        return PickedContact(
            name = name.ifBlank { address.substringBefore(',').trim() },
            address = address,
            phone = phone,
        )
    }

    private fun queryFirst(
        context: Context,
        uri: Uri,
        column: String,
        contactId: Long,
    ): String {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(column),
                "${ContactsContract.Data.CONTACT_ID}=?",
                arrayOf(contactId.toString()),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
            }.orEmpty()
        }.getOrDefault("")
    }
}
