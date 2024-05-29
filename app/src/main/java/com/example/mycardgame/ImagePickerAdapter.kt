package com.example.mycardgame

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.mycardgame.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(private val context: Context,private val imageUris: List<Uri>,private val boardSize: BoardSize,private val imageClickListener: ImageClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface ImageClickListener {
        fun onPlaceholderClicked()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardWidth: Int = parent.width / boardSize.getwidth()
        val cardHeight: Int = parent.height / boardSize.getheight()
        val cardSideLength: Int = min(cardWidth, cardHeight)
        val layoutparams: ViewGroup.LayoutParams =
            view.findViewById<ImageView>(R.id.iv_custom_img).layoutParams
        layoutparams.width = cardWidth
        layoutparams.height = cardWidth
        return ViewHolder(view)
    }

    override fun getItemCount() = boardSize.getNumPair()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < imageUris.size) {
            holder.bind(imageUris[position])
        } else {
            holder.bind()
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {}

    private fun RecyclerView.ViewHolder.bind(uri: Uri) {
        val ivCustomImage = itemView.findViewById<ImageView>(R.id.iv_custom_img)
        ivCustomImage.setImageURI(uri)
        ivCustomImage.setOnClickListener(null)
    }

    private fun RecyclerView.ViewHolder.bind() {
        val ivCustomImage = itemView.findViewById<ImageView>(R.id.iv_custom_img)
        ivCustomImage.setOnClickListener{
            imageClickListener.onPlaceholderClicked()
        }
    }

}