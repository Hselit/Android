package com.example.mycardgame.models

import com.google.firebase.firestore.PropertyName

data class UserImage (
    @PropertyName("images") val images: List<String>? = null
)
