package com.example.mycardgame

import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mycardgame.models.BoardSize
import com.example.mycardgame.models.MemoryCard
//import com.squareup.picasso.Picasso
import kotlin.math.min

class MemoryAdaptor(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<MemoryCard>,
    private val cardClickListener: CardClickListener
) : RecyclerView.Adapter<MemoryAdaptor.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryAdaptor"
    }

    interface CardClickListener {
        fun onCardClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val cardWidth = parent.width / boardSize.getwidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getheight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)

        val view: View = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams: ViewGroup.MarginLayoutParams = view.findViewById<CardView>(R.id.cardview).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return boardSize.numcards
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton: ImageButton = itemView.findViewById(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard: MemoryCard = cards[position]
            if (memoryCard.isFaceUp) {
                if (memoryCard.imageUrl != null) {
//                    Picasso.get().load(memoryCard.imageUrl).into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            } else {
                imageButton.setImageResource(R.drawable.bamboo)
            }
            imageButton.alpha = if (memoryCard.isMatched) 0.4f else 1.0f
            val colorStateList = if (memoryCard.isMatched) ContextCompat.getColorStateList(context, R.color.color_grey) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            imageButton.setOnClickListener {
                Log.i(TAG, "Clicked on position $position")
                cardClickListener.onCardClicked(position)
            }
        }
    }
}
