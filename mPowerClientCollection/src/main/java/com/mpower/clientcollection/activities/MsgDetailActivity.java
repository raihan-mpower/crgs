package com.mpower.clientcollection.activities;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.adapters.MsgDetailsAdapter;
import com.mpower.clientcollection.database.MpowerDatabaseHelper;
import com.mpower.clientcollection.models.MessageInfos;
import com.mpower.clientcollection.utilities.UserCollection;



public class MsgDetailActivity extends Activity {

	ListView msgDetailListView;
	
	String hhId = null;
	@SuppressWarnings("unused")
	private static final String TAG = MsgDetailActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (UserCollection.getInstance().isLoggedin()) {
			Log.d("login", "login=" + UserCollection.getInstance().getUserData().getUsername());
			setContentView(R.layout.details_msg_layout);

			
			msgDetailListView = (ListView) this
					.findViewById(R.id.msg_details_list);
			msgDetailListView
					.setOnItemLongClickListener(new OnItemLongClickListener() {
						@Override
						public boolean onItemLongClick(AdapterView<?> parent,
								View view, int position, long id) {
							if (hhId != null) {
								MsgDetailsAdapter adapter = (MsgDetailsAdapter) msgDetailListView
										.getAdapter();
								List<MessageInfos> miListItems = adapter
										.getItems();
								showAlert(
										getString(R.string.delete),
										getString(R.string.this_message_will_be_deleted),
										miListItems.get(position).getDbId());
							}
							return true;
						}
					});
			Bundle extra = getIntent().getExtras();
			if (extra == null) {
				return;
			} else {
				// long msgId = extra.getLong("msgId");
				MpowerDatabaseHelper sqlDH = new MpowerDatabaseHelper(this);
				hhId = extra.getString("hhId");
				List<MessageInfos> msgInfo = new ArrayList<MessageInfos>();
				msgInfo = sqlDH.getSelectedMessageInfo(hhId);
				MsgDetailsAdapter adapter = new MsgDetailsAdapter(
						MsgDetailActivity.this, msgInfo);
				msgDetailListView.setAdapter(adapter);
				
				sqlDH.close();
			}
			
		} else {
			
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("Log In");
			dialog.setCancelable(false);
			dialog.setMessage("You must be logged in");
			dialog.setPositiveButton(R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							Intent start = new Intent(MsgDetailActivity.this, ClientCollectionLoginActivity.class);
							startActivity(start);
						}
					});
			dialog.show();
		}
	}

	private void showAlert(String title, String msg, final long msgId) {

		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setTitle(title);
		adb.setMessage(msg);
		adb.setPositiveButton(R.string.ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						MpowerDatabaseHelper sqlDH = new MpowerDatabaseHelper(
								MsgDetailActivity.this);
						sqlDH.deleteMessage(msgId);
						updateMsgList();
						sqlDH.close();
					}
				});
		adb.setNegativeButton(R.string.cancel,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		adb.show();
	}

	private void updateMsgList() {

		List<MessageInfos> msgInfo = new ArrayList<MessageInfos>();
		if (hhId != null) {
			MpowerDatabaseHelper sqlDH = new MpowerDatabaseHelper(
					MsgDetailActivity.this);
			msgInfo = sqlDH.getSelectedMessageInfo(hhId);
			MsgDetailsAdapter adapter = new MsgDetailsAdapter(
					MsgDetailActivity.this, msgInfo);
			msgDetailListView.setAdapter(adapter);
			sqlDH.close();
		}
	}

}