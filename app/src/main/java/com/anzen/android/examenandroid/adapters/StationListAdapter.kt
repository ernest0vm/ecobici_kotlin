package com.anzen.android.examenandroid.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anzen.android.examenandroid.R
import com.anzen.android.examenandroid.models.Station
import java.math.RoundingMode

class StationListAdapter(context: Context, myDataSet: List<Station>) :
    RecyclerView.Adapter<StationListAdapter.MyViewHolder>() {
    private val mDataSet: List<Station> = myDataSet
    private val context: Context = context

    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var tvStation: TextView = view.findViewById(R.id.tvStation)
        var tvName: TextView = view.findViewById(R.id.tvName)
        var tvAddress: TextView = view.findViewById(R.id.tvAddress)
        var tvSlots: TextView = view.findViewById(R.id.tvSlots)
        var tvBikes: TextView = view.findViewById(R.id.tvBikes)
        var tvDistance: TextView = view.findViewById(R.id.tvDistance)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder { // create a new view
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_component, parent, false)
        return MyViewHolder(v)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        var text = "${context.getString(R.string.stationLabel)} ${mDataSet[position].id}"
        holder.tvStation.text = text
        text = mDataSet[position].name
        holder.tvName.text = text
        text = "${context.getString(R.string.addressLabel)} ${mDataSet[position].address}"
        holder.tvAddress.text = text
        text = "${mDataSet[position].bikes} ${context.getString(R.string.availableLabel)}"
        holder.tvBikes.text = text
        text = "${context.getString(R.string.availableSlotLabel)} ${mDataSet[position].slots}"
        holder.tvSlots.text = text
        val distanceInKilometers = (mDataSet[position].distance / 1000).toBigDecimal().setScale(2, RoundingMode.CEILING).toFloat()
        text = "$distanceInKilometers km"
        holder.tvDistance.text = text
    }

    override fun getItemCount(): Int {
        return mDataSet.size
    }

}