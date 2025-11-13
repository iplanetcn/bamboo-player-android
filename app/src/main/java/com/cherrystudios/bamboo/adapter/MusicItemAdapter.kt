package com.cherrystudios.bamboo.adapter

import android.R.attr.data
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.cherrystudios.bamboo.databinding.ItemMusicBinding
import com.cherrystudios.bamboo.ui.main.AudioFile

/**
 * MusicItemAdapter
 *
 * @author john
 * @since 2025-11-13
 */
class MusicItemAdapter(
    val data: MutableList<AudioFile> = mutableListOf()
): RecyclerView.Adapter<MusicItemViewHodler>() {
    private var onItemClick: ((Int) -> Unit)? = null

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClick = listener
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MusicItemViewHodler {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicItemViewHodler(binding)
    }

    override fun onBindViewHolder(
        holder: MusicItemViewHodler,
        position: Int
    ) {
        holder.bind(data[position])
        holder.binding.root.setOnClickListener {
            onItemClick?.invoke(position)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}

class MusicItemViewHodler(val binding: ItemMusicBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(file: AudioFile) {
        binding.tvTitle.text = file.displayName
        binding.tvSubtitle.text = file.artist
    }
}