/*
 * Copyright 2013 Niek Haarman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.haarman.listviewanimations.animationinexamples;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.haarman.listviewanimations.R;

public class AnimationInExamplesActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_examples_animationin);
	}

	public void onBottomInClicked(View view) {
		Intent intent = new Intent(this, SwingBottomInActivity.class);
		startActivity(intent);
	}

	public void onRightInClicked(View view) {
		Intent intent = new Intent(this, SwingRightInActivity.class);
		startActivity(intent);
	}

	public void onLeftInClicked(View view) {
		Intent intent = new Intent(this, SwingLeftInActivity.class);
		startActivity(intent);
	}

	public void onBottomRightInClicked(View view) {
		Intent intent = new Intent(this, SwingBottomRightInActivity.class);
		startActivity(intent);
	}

	public void onScaleInClicked(View view) {
		Intent intent = new Intent(this, ScaleInActivity.class);
		startActivity(intent);
	}

    public void onAlphaInClicked(View view){
        Intent intent = new Intent(this, AlphaInActivity.class);
        startActivity(intent);
    }
}
