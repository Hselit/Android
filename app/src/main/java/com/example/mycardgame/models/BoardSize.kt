package com.example.mycardgame.models

enum class BoardSize(val numcards: Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24);

    companion object{
        fun getByValue(value: Int) = values().first{ it.numcards == value }
    }
    fun getwidth(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
        }
    }

    fun getheight(): Int {
        return numcards / getwidth()
    }

    fun getNumPair(): Int {
        return numcards / 2
    }
}