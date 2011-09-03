/*
 * Copyright (C) 2011 asksven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asksven.betterlatitude;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

public class PositionOverlay extends ItemizedOverlay<OverlayItem> 
{
	
	private ArrayList<OverlayItem> mapOverlays = new ArrayList<OverlayItem>();
	
	private Context context;
		
	public PositionOverlay(Drawable defaultMarker, Context context)
	{
		  super(boundCenterBottom(defaultMarker));
		  this.context = context;
		  
		  populate();  // must be alled to avoid Null Pointer Exception on tap (see http://stackoverflow.com/questions/3755921/problem-with-crash-with-itemizedoverlay) 
	}

	@Override
	protected OverlayItem createItem(int i)
	{
		return mapOverlays.get(i);
	}

	@Override
	public int size()
	{
		return mapOverlays.size();
	}
	
	@Override
	protected boolean onTap(int index)
	{
		OverlayItem item = mapOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return true;
	}
	
	public void addOverlay(OverlayItem overlay)
	{
		mapOverlays.add(overlay);
	    this.populate();
	}

}