package ru.tattelecom.scanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_beacon.view.*
import kotlinx.android.synthetic.main.item_device.view.rssi
import org.altbeacon.beacon.Beacon
import ru.tattelecom.scanner.R

class BeaconAdapter(private val beacons : List<Beacon>, val clickListener: (Beacon) -> Unit) : RecyclerView.Adapter<BeaconAdapter.BeaconViewHolder>() {

    override fun getItemCount(): Int {
        return beacons.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        return BeaconViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_beacon, parent, false))
    }

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        val beacon = beacons[position]
        holder.itemView.setOnClickListener {
            clickListener(beacon)
        }
        holder.bind(beacon)
    }

    class BeaconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val beaconUuid = view.uuid
        private val beaconMajor = view.major
        private val beaconMinor = view.minor
        private val beaconRssi = view.rssi
        private val beaconDistance = view.distance

        fun bind(beacon: Beacon) {
            beaconUuid.text = "${beacon.id1}"
            beaconMajor.text = "${beacon.id2}"
            beaconMinor.text = "${beacon.id3}"
            beaconRssi.text = "${beacon.rssi} dBm"
            beaconDistance.text = "${"%.2f".format(beacon.distance)} m"
        }
    }
}