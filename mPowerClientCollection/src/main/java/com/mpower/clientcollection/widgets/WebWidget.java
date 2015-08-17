/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.mpower.clientcollection.widgets;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryController;
import org.javarosa.form.api.FormEntryPrompt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.mpower.clientcollection.activities.FormEntryActivity;
import com.mpower.clientcollection.activities.WebServiceActivity;
import com.mpower.clientcollection.activity.R;
import com.mpower.clientcollection.application.ClientCollection;
import com.mpower.clientcollection.logic.FormController;
import com.mpower.clientcollection.utilities.LogUtils;

/**
 * The most basic widget that allows for entry of any text.
 * 
 * @author ratna halder (ratnacse06@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com) 
 */
public class WebWidget extends QuestionWidget implements IBinaryWidget{
	private static final String ROWS = "rows";

    boolean mReadOnly = false;
    private Button mGetServerInfoButton;
    protected TextView mAnswerText;
    private FormEntryPrompt fep;

    public WebWidget(Context context, FormEntryPrompt prompt) {
    	this(context, prompt, true);
    	fep = prompt;
    	setupChangeListener();
    }

    protected WebWidget(Context context, FormEntryPrompt prompt, boolean derived) {
        super(context, prompt);
        LogUtils.informationLog(this, "WebWidget is creating..");
        
        fep = prompt;
        mAnswerText = new TextView(context);
        mAnswerText.setId(QuestionWidget.newUniqueId());
        mReadOnly = prompt.isReadOnly();

        mAnswerText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, mAnswerFontsize);

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();

        String height = prompt.getQuestion().getAdditionalAttribute(null, ROWS);
        if ( height != null && height.length() != 0 ) {
        	try {
	        	int rows = Integer.valueOf(height);
	        	mAnswerText.setMinLines(rows);
	        	mAnswerText.setGravity(Gravity.TOP); // to write test starting at the top of the edit area
        	} catch (Exception e) {
        		Log.e(this.getClass().getName(), "Unable to process the rows setting for the answer field: " + e.toString());
        	}
        }

        params.setMargins(7, 5, 7, 5);
        mAnswerText.setLayoutParams(params);

        // needed to make long read only text scroll
        mAnswerText.setHorizontallyScrolling(false);
                
        if (mReadOnly) {
            mAnswerText.setBackgroundDrawable(null);
            mAnswerText.setFocusable(false);
            mAnswerText.setClickable(false);
        }
        
        mGetServerInfoButton = new Button(getContext());
		mGetServerInfoButton.setId(QuestionWidget.newUniqueId());
		mGetServerInfoButton.setPadding(20, 20, 20, 20);
		mGetServerInfoButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
				mAnswerFontsize);
		mGetServerInfoButton.setText(getResources().getString(R.string.validated_with_server));
		mGetServerInfoButton.setEnabled(!prompt.isReadOnly());
		mGetServerInfoButton.setLayoutParams(params);
		mGetServerInfoButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String ans = previousQusAnswer();
				LogUtils.informationLog(this, "Clicked on search button");
				Intent intent = new Intent(getContext(), WebServiceActivity.class);
				intent.putExtra("searchedValue", ans);				
				ClientCollection.getInstance().getFormController()
						.setIndexWaitingForData(mPrompt.getIndex());
				((Activity) getContext()).startActivityForResult(intent,
						FormEntryActivity.WEB_CAPTURE);						
			}
		});

        addView(mAnswerText);
        addView(mGetServerInfoButton);
        
     // figure out what text and buttons to enable or to show...
     		boolean dataAvailable = false;
     		String s = prompt.getAnswerText();
     		if (s != null && !s.equals("")) {
     			dataAvailable = true;
     			setBinaryData(s);
     		}
     		updateButtonLabelsAndVisibility(dataAvailable);
    }
    
    private String previousQusAnswer(){
    	
    	FormController formController = ClientCollection.getInstance().getFormController();
    	if (formController.getEvent() != FormEntryController.EVENT_BEGINNING_OF_FORM) {
			formController.stepToPreviousScreenEvent();
			FormEntryPrompt promp = formController.getQuestionPrompt();
			String txt = promp.getAnswerText();
			LogUtils.informationLog(this, "first = " + txt);
			
			formController.stepToNextScreenEvent();
			return txt;
    	}
    	return null;
    	
    }
    
    protected void setupChangeListener() {
        mAnswerText.addTextChangedListener(new TextWatcher() {
        	private String oldText = "";

			@Override
			public void afterTextChanged(Editable s) {
				if (!s.toString().equals(oldText)) {
					ClientCollection.getInstance().getActivityLogger()
						.logInstanceAction(this, "answerTextChanged", s.toString(),	getPrompt().getIndex());
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				oldText = s.toString();
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) { }
        });
    }

    @Override
    public void clearAnswer() {
        mAnswerText.setText(null);
    }


    @Override
    public IAnswerData getAnswer() {
    	clearFocus();
    	String s = mAnswerText.getText().toString();
        if (s == null || s.equals("")) {
            return null;
        } else {
            return new StringData(s);
        }
    }


    @Override
    public void setFocus(Context context) {
        // Put focus on text input field and display soft keyboard if appropriate.
        mAnswerText.requestFocus();
        InputMethodManager inputManager =
            (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!mReadOnly) {
            inputManager.showSoftInput(mAnswerText, 0);
            /*
             * If you do a multi-question screen after a "add another group" dialog, this won't
             * automatically pop up. It's an Android issue.
             *
             * That is, if I have an edit text in an activity, and pop a dialog, and in that
             * dialog's button's OnClick() I call edittext.requestFocus() and
             * showSoftInput(edittext, 0), showSoftinput() returns false. However, if the edittext
             * is focused before the dialog pops up, everything works fine. great.
             */
        } else {
            inputManager.hideSoftInputFromWindow(mAnswerText.getWindowToken(), 0);
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.isAltPressed() == true) {
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mAnswerText.setOnLongClickListener(l);
    }


    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        mAnswerText.cancelLongPress();
    }

	@Override
	public void setBinaryData(Object answer) {
		String s = (String)answer;
		mAnswerText.setText(s);
		
		ClientCollection.getInstance().getFormController().setIndexWaitingForData(null);
		updateButtonLabelsAndVisibility(true);
	}

	private void updateButtonLabelsAndVisibility(boolean b) {
		if(b){
			mGetServerInfoButton.setText("Sync Again");
		}else{
			mAnswerText.setVisibility(View.GONE);
		}
	}

	@Override
	public void cancelWaitingForBinaryData() {
		ClientCollection.getInstance().getFormController().setIndexWaitingForData(null);
		
	}

	@Override
	public boolean isWaitingForBinaryData() {
		return mPrompt.getIndex().equals(
				ClientCollection.getInstance().getFormController()
						.getIndexWaitingForData());
	}

}
