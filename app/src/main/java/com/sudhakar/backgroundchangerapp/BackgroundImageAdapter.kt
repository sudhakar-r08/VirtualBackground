package com.sudhakar.backgroundchangerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sudhakar.backgroundchangerapp.BackgroundImageAdapter.MyViewHolder
import java.util.ArrayList

class BackgroundImageAdapter(var images: ArrayList<Any>, var listener: BackgroundClickListener) :
    RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.background_list_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.setImage(images[position] as Int)

    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var imageView: ImageView? = itemView.findViewById(R.id.imgBackground)
        init {
            itemView.setOnClickListener {
                listener.onImageClick(adapterPosition)
            }
        }
        fun setImage(image: Int) {
            imageView?.let {
                Glide
                    .with(itemView.context)
                    .load(image)
                    .centerCrop()
                    .into(it)
            }
        }

    }
}