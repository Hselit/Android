package com.example.mycardgame.models

import com.example.mycardgame.utils.DEFAULT_ICONS

class MemoryGame(private val boardSize: BoardSize, private val customImages: List<String>?) {

    val cards: List<MemoryCard>
    var numPairsfound = 0

    private var numCardFlips = 0
    private var indexOfSingleSelectedCard: Int? = null

    init {
        if (customImages == null) {
            val chosenImg: List<Int> = DEFAULT_ICONS.shuffled().take(boardSize.getNumPair())
            val randomizedImages: List<Int> = (chosenImg + chosenImg).shuffled()
            cards = randomizedImages.map { MemoryCard(it) }
        } else {
            val  randomizedImages: List<String> = (customImages + customImages).shuffled()
            cards = randomizedImages.map { MemoryCard(it.hashCode(),it) }
        }
    }

    fun flipcards(position: Int):Boolean {
        numCardFlips++
        var cards:MemoryCard = cards[position]
        var foundMatch = false

        if(indexOfSingleSelectedCard == null) {
            restoreCards()
            indexOfSingleSelectedCard = position
        }
        else {
            foundMatch = checkForMatch(indexOfSingleSelectedCard!!,position)
            indexOfSingleSelectedCard = null
        }
        cards.isFaceUp = !cards.isFaceUp
        return foundMatch
    }

    private fun checkForMatch(position1: Int, position2: Int): Boolean {
        if(cards[position1].identifier != cards[position2].identifier) {
            return false
        }
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        numPairsfound++
        return true
    }

    private fun restoreCards() {
        for (card:MemoryCard in cards){
            if(!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    fun haveWonGame(): Boolean {
        return numPairsfound == boardSize.getNumPair()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getNumMoves(): Int{
        return numCardFlips / 2
    }

}