package com.mahmutgunduz.adettakip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mahmutgunduz.adettakip.R
import com.mahmutgunduz.adettakip.model.NoteItem
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onEditClick: (Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private var notesList = listOf<NoteItem>()
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    fun updateList(newList: List<NoteItem>) {
        notesList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notesList[position], position)
    }

    override fun getItemCount(): Int = notesList.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNoteTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvNoteContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        private val tvNoteDate: TextView = itemView.findViewById(R.id.tvNoteDate)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(note: NoteItem, position: Int) {
            tvNoteTitle.text = note.title
            tvNoteContent.text = note.content
            tvNoteDate.text = dateFormat.format(note.timestamp)

            btnEdit.setOnClickListener {
                onEditClick(position)
            }

            btnDelete.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }
}