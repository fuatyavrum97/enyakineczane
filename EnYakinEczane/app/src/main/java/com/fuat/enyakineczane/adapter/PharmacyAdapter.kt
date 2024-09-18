package com.fuat.enyakineczane.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fuat.enyakineczane.retrofit.Pharmacy
import com.fuat.enyakineczane.R

class PharmacyAdapter(
    private val pharmacyList: List<Pharmacy>,
    private val onItemClicked: (Pharmacy) -> Unit
) : RecyclerView.Adapter<PharmacyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.pharmacy_name)
        val phoneNumberTextView: TextView = view.findViewById(R.id.pharmacy_phone_number)
        val iconImageView: ImageView = view.findViewById(R.id.pharmacy_icon)
        val phoneIconImageView: ImageView = view.findViewById(R.id.phone_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.pharmacy_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pharmacy = pharmacyList[position]
        holder.nameTextView.text = pharmacy.name
        holder.phoneNumberTextView.text = pharmacy.phoneNumber ?: "Telefon numarası yok"

        holder.iconImageView.setImageResource(R.drawable.ic_pharmacy)
        holder.phoneIconImageView.setImageResource(R.drawable.ic_phone)

        holder.phoneIconImageView.setOnClickListener {
            pharmacy.phoneNumber?.let { phoneNumber ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                }
                holder.itemView.context.startActivity(intent)
            }
        }

        holder.itemView.setOnClickListener {
            val uri = Uri.parse("google.navigation:q=${pharmacy.latitude},${pharmacy.longitude}&mode=w")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(holder.itemView.context.packageManager) != null) {
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = pharmacyList.size
}
