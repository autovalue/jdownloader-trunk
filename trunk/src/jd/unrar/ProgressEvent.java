/*
 * Copyright (C) 2002 - 2005 Leonardo Ferracci
 *
 * This file is part of JAxe.
 *
 * JAxe is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * JAxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with JAxe; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.  Or, visit http://www.gnu.org/copyleft/gpl.html
 */

package jd.unrar;

public class ProgressEvent
{
	protected Object oSource;
	protected long lCurrent;
	protected long lMax;

	public ProgressEvent (Object o, long lC, long lM)
	{
		oSource = o;
		lCurrent = lC;
		lMax = lM;
	}

	public void setMax (long l)
	{
		lMax = l;
	}

	public void setCurrent (long l)
	{
		lCurrent = l;
	}

	public long getMax()
	{
		return lMax;
	}

	public long getCurrent()
	{
		return lCurrent;
	}

	public Object getSource()
	{
		return oSource;
	}
}
