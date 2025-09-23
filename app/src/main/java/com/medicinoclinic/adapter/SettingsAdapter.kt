package com.medicinoclinic.adapter

import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.medicinoclinic.R
import com.medicinoclinic.home.doctor_selected_or_unselected
import com.medicinoclinic.model.DoctorsModel
import com.medicinoclinic.retrofit.APIService
import com.medicinoclinic.utils.BaseClass
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SettingsAdapter(private val mList: List<DoctorsModel>) :
    RecyclerView.Adapter<SettingsAdapter.ViewHolder>() {
    var baseClass =  BaseClass()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_settings, parent, false)

        var token = baseClass.getSharedPreferance(parent.context, "token", "")
        RetrofitClient.bearer_token = token

        view.isFocusable = true
        view.isFocusableInTouchMode = true

        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]
        holder.tvName.text = ItemsViewModel.doctor_name
        holder.tvRoomNo.text = ItemsViewModel.room_no
        holder.linMain.setOnClickListener(null)
        holder.tvSerialNo.text = ((position+1).toString())
        holder.checkBox.isEnabled = ItemsViewModel.isSelected



        if (ItemsViewModel.isSelected) {
            holder.checkBox.isChecked = true
//            holder.linMain.setBackgroundColor(Color.parseColor("#A692F3DE"))
            holder.linRoomNo.isVisible = true;
        } else {
            if(position == 0) {
                holder.checkBox.isChecked = true
            }
            else {
                holder.checkBox.isChecked = false
            }
            holder.checkBox.isChecked = false
//            holder.linMain.setBackgroundColor(Color.parseColor("#FFFFFF"));
            holder.linRoomNo.isVisible = false;
        }

      /*  holder.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if(isChecked) {
                addDoctors(buttonView.context,ItemsViewModel.id,"1","1")
            }else {
                addDoctors(buttonView.context,ItemsViewModel.id,"0","1")
            }
        }*/

        holder.itemView.setOnFocusChangeListener { view, b ->
            if(b) {
            }
            else {

            }

        }


        holder.itemView.setOnClickListener { v ->
            // addDoctors(v.context,ItemsViewModel.id)
            doctor_selected_or_unselected = true
            if(ItemsViewModel.isSelected){
                ItemsViewModel.isSelected = !ItemsViewModel.isSelected
                addDoctors(v.context,ItemsViewModel.id,"0","1")
                this.notifyItemChanged(position)

            }else{
                val count: Int = mList.count { c -> c.isSelected }
                addDoctors(v.context, ItemsViewModel.id, "1", "1")
                ItemsViewModel.isSelected = !ItemsViewModel.isSelected
                this.notifyItemChanged(position)
//               if(count<3) {
//                    addDoctors(v.context, ItemsViewModel.id, "1", "1")
//                    ItemsViewModel.isSelected = !ItemsViewModel.isSelected
//                    this.notifyItemChanged(position)
//                }
//                else {
//                    Toast.makeText(v.context,"Only 3 doctors are allowed",Toast.LENGTH_LONG).show()
//                }

            }
        }
    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvRoomNo: TextView = itemView.findViewById(R.id.tv_room_no)
        val linMain: LinearLayout = itemView.findViewById(R.id.lin_main)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        val tvSerialNo: TextView = itemView.findViewById(R.id.tv_serial_no)
        val linRoomNo: LinearLayout = itemView.findViewById(R.id.lin_room_no)


    }
    fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun addDoctors(context: Context,doctorID: String, type: String, room: String) {

        val progressDialog = ProgressDialog(context, R.style.MyTheme)
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Large)
        progressDialog.show()
        progressDialog.setCancelable(false)


        var mAPIService: APIService? = null
        mAPIService = RetrofitClient.ApiUtils.apiService1
        mAPIService.addDoctor(doctorID,type,room).enqueue(object : Callback<ResponseBody> {

            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {

                if (response.isSuccessful) {
                    progressDialog.dismiss()
                    var response_from_server = response.body()
                    var jsonObject: JSONObject? = null
                    jsonObject = JSONObject(response.body()!!.string())
                    val message: String = jsonObject.getString("message")

                    Toast.makeText(context,message,Toast.LENGTH_LONG).show()

                } else {
                    progressDialog.dismiss()
                    var jsonObject: JSONObject? = null


                    if (response.code() == 400) {
                        jsonObject = JSONObject(response.errorBody()!!.string())
                        val message: String = jsonObject.getString("message")

                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else if (response.code() == 500) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.api_error),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {

                        Toast.makeText(
                            context,
                            context.getString(R.string.something_wrong),
                            Toast.LENGTH_LONG
                        ).show()

                    }

                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Toast.makeText(
                    context,
                    context.getString(R.string.response_failed),
                    Toast.LENGTH_LONG
                ).show()
                progressDialog.dismiss()
            }
        })
    }
}