/*
 * Microba controls http://sourceforge.net/projects/microba/
 * Copyright (c) 2005-2006, Michael Baranov
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.  
 * 3. Neither the name of the MICROBA, MICHAELBARANOV.COM, MICHAEL BARANOV nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY 
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package hc.com.michaelbaranov.microba.calendar.ui.basic;

import hc.com.michaelbaranov.microba.calendar.CalendarResources;
import hc.com.michaelbaranov.microba.calendar.VetoPolicy;
import hc.com.michaelbaranov.microba.common.PolicyEvent;
import hc.com.michaelbaranov.microba.common.PolicyListener;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.swing.JButton;
import javax.swing.JPanel;


class AuxPanel extends JPanel implements PropertyChangeListener, PolicyListener {

	public static final String PROPERTY_NAME_LOCALE = "locale";

	public static final String PROPERTY_NAME_DATE = "date";

	public static final String PROPERTY_NAME_ZONE = "zone";

	public static final String PROPERTY_NAME_RESOURCES = "resources";

	public static final String PROPERTY_NAME_VETO_MODEL = "vetoModel";

	private Locale locale;

	private TimeZone zone;

	private JButton todayButton;

	private JButton noneButton;

	private DateFormat fullDateFormat;

	private Date currentDate;

	private Set focusableComponents = new HashSet();

	private VetoPolicy vetoModel;

	private boolean showTodayBtn;

	private CalendarResources resources;

	private boolean showNoneButton;

	public AuxPanel(Locale locale, TimeZone zone, VetoPolicy vetoModel,
			boolean showTodayBtn, boolean showNoneButton,
			CalendarResources resources) {
		this.locale = locale;
		this.zone = zone;
		this.vetoModel = vetoModel;
		this.showTodayBtn = showTodayBtn;
		this.showNoneButton = showNoneButton;
		this.resources = resources;
		if (vetoModel != null)
			vetoModel.addVetoPolicyListener(this);

		setLayout(new GridBagLayout());

		todayButton = new JButton();
		todayButton.setBorderPainted(false);
		todayButton.setContentAreaFilled(false);
		todayButton.setVisible(showTodayBtn);

		noneButton = new JButton();
		noneButton.setBorderPainted(false);
		noneButton.setContentAreaFilled(false);
		noneButton.setVisible(showNoneButton);

		add(todayButton, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0,
						0, 0, 0), 0, 0));
		add(noneButton, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
				GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0,
						0, 0, 0), 0, 0));

		currentDate = new Date();
		validateAgainstVeto();
		createLocaleAndZoneSensitive();
		reflectData();

		todayButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				currentDate = new Date();
				firePropertyChange(PROPERTY_NAME_DATE, null, currentDate);
			}
		});
		noneButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				firePropertyChange(PROPERTY_NAME_DATE, null, null);
			}
		});

		focusableComponents.add(todayButton);
		this.addPropertyChangeListener(this);

		// Timer timer = new Timer(true);
		// timer.schedule(new TimerTask() {
		//
		// public void run() {
		// currentDate = new Date();
		// validateAgainstVeto();
		// }
		// }, 0, 60000);

	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("focusable")) {
			Boolean value = (Boolean) evt.getNewValue();
			todayButton.setFocusable(value.booleanValue());
			noneButton.setFocusable(value.booleanValue());
		}
		if (evt.getPropertyName().equals("enabled")) {
			Boolean value = (Boolean) evt.getNewValue();
			todayButton.setEnabled(value.booleanValue());
			noneButton.setEnabled(value.booleanValue());
		}
		if (evt.getPropertyName().equals(PROPERTY_NAME_VETO_MODEL)) {
			VetoPolicy oldValue = (VetoPolicy) evt.getOldValue();
			VetoPolicy newValue = (VetoPolicy) evt.getOldValue();
			if (oldValue != null)
				oldValue.removeVetoPolicyListener(this);
			if (newValue != null)
				newValue.addVetoPolicyListener(this);
			validateAgainstVeto();
		}

	}

	private void createLocaleAndZoneSensitive() {
		fullDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
		fullDateFormat.setTimeZone(zone);
	}

	private void reflectData() {
		String today = resources.getResource(CalendarResources.KEY_TODAY,
				locale);
		String none = resources.getResource(CalendarResources.KEY_NONE, locale);
		todayButton.setText(today + ": " + fullDateFormat.format(currentDate));
		noneButton.setText(none);

	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		Locale old = this.locale;
		this.locale = locale;
		firePropertyChange(PROPERTY_NAME_LOCALE, old, locale);
		createLocaleAndZoneSensitive();
		reflectData();
	}

	public Collection getFocusableComponents() {
		return focusableComponents;
	}

	public TimeZone getZone() {
		return zone;
	}

	public void setZone(TimeZone zone) {
		this.zone = zone;
		createLocaleAndZoneSensitive();
		reflectData();
	}

	public Date getDate() {
		return currentDate;
	}

	public VetoPolicy getVetoModel() {
		return vetoModel;
	}

	public void setVetoModel(VetoPolicy vetoModel) {
		VetoPolicy old = this.vetoModel;
		this.vetoModel = vetoModel;
		firePropertyChange(PROPERTY_NAME_VETO_MODEL, old, vetoModel);
	}

	public void policyChanged(PolicyEvent event) {
		validateAgainstVeto();
	}

	private void validateAgainstVeto() {
		Calendar c = Calendar.getInstance(zone, locale);
		c.setTime(currentDate);
		if (vetoModel != null) {
			todayButton.setEnabled(!vetoModel.isRestricted(this, c));
			noneButton.setEnabled(!vetoModel.isRestrictNull(this));
		} else {
			todayButton.setEnabled(this.isEnabled());
			noneButton.setEnabled(this.isEnabled());
		}

	}

	public void setShowTodayBtn(boolean value) {
		showTodayBtn = value;
		todayButton.setVisible(showTodayBtn);
	}

	public void setResources(CalendarResources resources) {
		CalendarResources old = this.resources;
		this.resources = resources;
		firePropertyChange(PROPERTY_NAME_RESOURCES, old, resources);
		reflectData();
	}

	public void setShowNoneButton(boolean value) {
		showNoneButton = value;
		noneButton.setVisible(showNoneButton);
	}

}
