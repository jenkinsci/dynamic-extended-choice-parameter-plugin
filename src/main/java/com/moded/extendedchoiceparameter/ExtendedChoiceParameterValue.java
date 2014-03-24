/*
 *Copyright (c) 2013 Costco, RGS
 *See the file license.txt for copying permission.
 */


package com.moded.extendedchoiceparameter;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.StringParameterValue;

public class ExtendedChoiceParameterValue extends StringParameterValue{
	private static final long serialVersionUID = 7993744779892775177L;
	
	@DataBoundConstructor
	public ExtendedChoiceParameterValue(String name, String value) {
		super(name, value);
	}

}
