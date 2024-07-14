package com.dmgritsan.pockettelemetry

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RaceListAdapter(private val raceSummaries: MutableList<RaceSummary>) :
    RecyclerView.Adapter<RaceListAdapter.RaceViewHolder>() {

    var currentPosition: Int = -1

    inner class RaceViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnCreateContextMenuListener {
        val textView: TextView = view.findViewById(R.id.text_view_race_summary)


        init {
            view.setOnCreateContextMenuListener(this)
            view.setOnLongClickListener {
                currentPosition = adapterPosition
                false
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu.add(Menu.NONE, 1, Menu.NONE, "Open")
            menu.add(Menu.NONE, 2, Menu.NONE, "Send")
            menu.add(Menu.NONE, 3, Menu.NONE, "Delete")
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.race_summary_item, parent, false)
        return RaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: RaceViewHolder, position: Int) {
        val summary = raceSummaries[position]
        holder.textView.text = "File: ${summary.fileName}\nEntries: ${summary.dataCount}"
    }

    override fun getItemCount() = raceSummaries.size


}
