package ru.tattelecom.scanner.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_device.view.*
import ru.tattelecom.scanner.R
import ru.tattelecom.scanner.model.LeDevice

class LeDeviceAdapter(private val leDevices : List<LeDevice>) : RecyclerView.Adapter<LeDeviceAdapter.DeviceViewHolder>() {

    override fun getItemCount(): Int {
        return leDevices.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        return DeviceViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false))
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val leDevice = leDevices[position]
        holder.bind(leDevice)
    }

    class DeviceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceAddress = view.address
        private val deviceRssi = view.rssi

        fun bind(leDevice: LeDevice) {
            deviceAddress.text = leDevice.device.address
            deviceRssi.text = "${leDevice.rssi} dBm"
        }
    }
}