package com.mpower.clientcollection.activities;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.adapters.MsgAdapter;
import com.mpower.clientcollection.database.MpowerDatabaseHelper;
import com.mpower.clientcollection.models.MessageInfos;

public class MessagingActivity extends Activity {

	ListView msgList = null;

	private static final String MESSAGE_MARK_READ = "Mark Read";
	private static final String MESSAGE_MARK_UNREAD = "Mark Unread";
	private static final String MESSAGE_DELETE = "Delete";
	@SuppressWarnings("unused")
	private static final String TAG = MessagingActivity.class.getSimpleName();
	private boolean registeredReceivers = false;
	

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.msg_layout);

		msgList = (ListView) this.findViewById(R.id.msg_list_view);
		msgList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (msgList.getCount() > 0) {
					MpowerDatabaseHelper sqlDH = new MpowerDatabaseHelper(MessagingActivity.this);
					
					MsgAdapter adapter = (MsgAdapter) msgList.getAdapter();
					List<MessageInfos> miListItems = adapter.getItems();
					long msgId = miListItems.get(position).getDbId();
					sqlDH.updateReadStatus(msgId, 1);
					
					Intent start = new Intent(MessagingActivity.this, MsgDetailActivity.class);
					start.putExtra("hhId", sqlDH.getHHId(msgId));
					start.putExtra("notificationId", sqlDH.getNotificationId(msgId));
					startActivity(start);
				}
			}
		});

		msgList.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				String[] readMsg = { MESSAGE_MARK_READ, MESSAGE_DELETE };
				String[] unReadMsg = { MESSAGE_MARK_UNREAD, MESSAGE_DELETE };
				MsgAdapter adapter = (MsgAdapter) msgList.getAdapter();
				List<MessageInfos> miListItems = adapter.getItems();
				if (miListItems.get(position).getReadStatus() == 1) {
					showAlert(unReadMsg, position);
				} else if (miListItems.get(position).getReadStatus() == 0) {
					showAlert(readMsg, position);
				}
				return true;
			}
		});
		updateMsgList();
		}
	
	private void updateMsgList() {
		MpowerDatabaseHelper sqlDH = new MpowerDatabaseHelper(MessagingActivity.this);
		List<MessageInfos> messagesInfo = sqlDH.getMessageInfo();		
		
		if (messagesInfo.size() > 0) {						
			MsgAdapter msgAdapter = new MsgAdapter(MessagingActivity.this, messagesInfo);
			msgList.setAdapter(msgAdapter);
		}
	}

	private void showAlert(String[] msgStatus, final int position) {

		final String[] msgMenu = msgStatus;
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setItems(msgMenu, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String optionItem = msgMenu[which];
				MpowerDatabaseHelper sqlDH = new MpowerDatabaseHelper(MessagingActivity.this);
				MsgAdapter adapter = (MsgAdapter) msgList.getAdapter();
				List<MessageInfos> miListItems = adapter.getItems();

				if (optionItem == MESSAGE_MARK_READ) {
					sqlDH.updateReadStatus(miListItems.get(position).getDbId(), 1);
					updateMsgList();
				} else if (optionItem == MESSAGE_MARK_UNREAD) {
					sqlDH.updateReadStatus(miListItems.get(position).getDbId(), 0);
					updateMsgList();
				} else if (optionItem == MESSAGE_DELETE) {
					String msgHHId = sqlDH.getHHId(miListItems.get(position).getDbId());
					showAlert(getString(R.string.delete), getString(R.string.this_feature_will_delete_whole_thread), msgHHId);
				}
			}
		});
		adb.show();
	}

	private void showAlert(String title, String msg, final String msgHhId) {

		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		adb.setTitle(title);
		adb.setMessage(msg);
		adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
				MpowerDatabaseHelper sqlDH = new MpowerDatabaseHelper(MessagingActivity.this);
				sqlDH.deleteTotalMessages(msgHhId);
				updateMsgList();
			}
		});
		adb.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		adb.show();
	}

	@Override
	public void onResume() {
		super.onResume();
		// SQLiteDatabaseHelper sqlDH = new SQLiteDatabaseHelper(this);
		// sqlDH.InsertDummyDataIntoDatabase();
		updateMsgList();
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (registeredReceivers) {
			unregisterReceiver(messageUpdatedReceiver);
		}
	}

	private BroadcastReceiver messageUpdatedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateMsgList();
		}
	};

	}