package com.mpower.clientcollection.adapters;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.models.MessageInfos;

public class MsgDetailsAdapter extends ArrayAdapter<MessageInfos> {
	Context context;
	List<MessageInfos> items;
	LayoutInflater inflater;

	public MsgDetailsAdapter(Context context, List<MessageInfos> items) {
		super(context, R.layout.details_msg_row, items);
		this.context = context;
		this.items = items;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public List<MessageInfos> getItems() {
		return this.items;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		final MessageInfos mi = items.get(position);
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.details_msg_row, parent, false);
			holder = new ViewHolder();
			convertView.setTag(holder);
			holder.message = (TextView) convertView.findViewById(R.id.detail_msg_txt);
			holder.msgDate = (TextView) convertView.findViewById(R.id.detail_msg_date);
			holder.msgSender = (TextView) convertView.findViewById(R.id.detail_msg_sender);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
	
		holder.msgDate.setText(mi.getDate());
		holder.message.setText(mi.getNotificationMsg());		
		holder.msgSender.setText(ClientCollection.getInstance().getResources().getString(R.string.me));
		
		return convertView;
	}

	public class ViewHolder {
		public TextView message = null, msgDate = null, msgSender = null;
	}
}
