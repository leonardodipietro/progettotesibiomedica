package com.example.myapplication2.model

import android.os.Parcel
import android.os.Parcelable


data class Utente(
    val id: String? = null,
    val username: String? = null,
    val email: String? = null,
    val name: String? = null,
    val address: String? = null,
    val password: String? = null,
    val ruolo: String? = null,
    val phoneNumber: String? = null
) : Parcelable {

    // Costruttore  Firebase
    constructor() : this(null, null, null, null, null, null, null, null)

    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(username)
        parcel.writeString(email)
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeString(password)
        parcel.writeString(ruolo)
        parcel.writeString(phoneNumber)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Utente> {
        override fun createFromParcel(parcel: Parcel): Utente = Utente(parcel)
        override fun newArray(size: Int): Array<Utente?> = arrayOfNulls(size)
    }

}

