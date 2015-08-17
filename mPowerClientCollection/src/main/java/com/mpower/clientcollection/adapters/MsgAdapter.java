package com.mpower.clientcollection.adapters;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.models.MessageInfos;

	public class MsgAdapter extends ArrayAdapter<MessageInfos> {
	LayoutInflater inflater;
	List<MessageInfos> items;
	Context context;

	public MsgAdapter(Context context, List<MessageInfos> items) {
		super(context, R.layout.msg_row, items);
		this.items = items;
		this.context = context;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public List<MessageInfos> getItems() {
		return this.items;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		final ViewHolder holder;
		final MessageInfos mi = items.get(position);
		if (view == null) {
			view = inflater.inflate(R.layout.msg_row, parent, false);
			holder = new ViewHolder();
			view.setTag(holder);
			holder.sender = (TextView) view.findViewById(R.id.msg_sender);
			holder.date = (TextView) view.findViewById(R.id.msg_date);
			holder.message = (TextView) view.findViewById(R.id.msg_message);
			
		} else {
			holder = (ViewHolder) view.getTag();
		}
		holder.id = mi.getNotificationId();
		RelativeLayout relativeLayout = (RelativeLayout)view.findViewById(R.id.msg_list_row);
		Log.d(MsgAdapter.class.getSimpleName(), "readStatus = " + mi.getReadStatus());
		if(mi.getReadStatus() == 0){			
			relativeLayout.setBackgroundColor(Color.WHITE);			
		}else if(mi.getReadStatus() == 1){
			relativeLayout.setBackgroundColor(Color.LTGRAY);
		}
		holder.sender.setText(mi.getBeneficiaryName());
		holder.date.setText(mi.getDate());
		holder.message.setText(mi.getNotificationMsg());
		
		return view;
	}

	public class ViewHolder {
		public String id;
		public TextView sender = null, date = null, message = null;
	}
}
