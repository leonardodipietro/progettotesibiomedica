package com.example.myapplication2.model

import android.os.Parcel
import android.os.Parcelable

data class Utente(
    val username: String? = null,
    val email: String? = null,
    val password: String? = null,
    val admin: Boolean? = null,
    val phoneNumber: String? = null,
    val id: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(username)
        parcel.writeString(email)
        parcel.writeString(password)
        parcel.writeValue(admin)
        parcel.writeString(phoneNumber)
        parcel.writeString(id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Utente> {
        override fun createFromParcel(parcel: Parcel): Utente {
            return Utente(parcel)
        }

        override fun newArray(size: Int): Array<Utente?> {
            return arrayOfNulls(size)
        }
    }
}
