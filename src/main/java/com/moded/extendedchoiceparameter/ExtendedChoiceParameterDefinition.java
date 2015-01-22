/*
 *Copyright (c) 2013 Costco, RGS
 *Copyright (c) 2013 John DiMatteo
 *See the file license.txt for copying permission.
 */

package com.moded.extendedchoiceparameter;

import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.ParameterDefinition;
import hudson.util.FormValidation;
import hudson.model.Hudson;
import hudson.model.User;
import hudson.security.AuthorizationStrategy;
import com.michelin.cio.hudson.plugins.rolestrategy.Role;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;

import javax.servlet.ServletException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import au.com.bytecode.opencsv.CSVReader;

import org.kohsuke.stapler.bind.JavaScriptMethod;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.SVNDirEntry;

public class ExtendedChoiceParameterDefinition extends ParameterDefinition {
	private static final long serialVersionUID = -2946187268529865645L;

	public static final String PARAMETER_TYPE_SINGLE_SELECT = "PT_SINGLE_SELECT";

	public static final String PARAMETER_TYPE_MULTI_SELECT = "PT_MULTI_SELECT";

	public static final String PARAMETER_TYPE_CHECK_BOX = "PT_CHECKBOX";

	public static final String PARAMETER_TYPE_RADIO = "PT_RADIO";

	public static final String PARAMETER_TYPE_TEXT_BOX = "PT_TEXTBOX";
        
	public static final String PARAMETER_TYPE_MULTI_LEVEL_SINGLE_SELECT = "PT_MULTI_LEVEL_SINGLE_SELECT";
        
	public static final String PARAMETER_TYPE_MULTI_LEVEL_MULTI_SELECT = "PT_MULTI_LEVEL_MULTI_SELECT";

	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {
		@Override
		public String getDisplayName() {
			return Messages.ExtendedChoiceParameterDefinition_DisplayName();
		}

		public FormValidation doCheckPropertyFile(@QueryParameter final String propertyFile, @QueryParameter final String propertyKey, @QueryParameter final String type) throws IOException, ServletException {
			if(StringUtils.isBlank(propertyFile)) {
				return FormValidation.ok();
			}

			Project project = new Project();
			Property property = new Property();
			property.setProject(project);

			File prop = new File(propertyFile);
			try {
				if(prop.exists()) {
					property.setFile(prop);
				}
				else {
					URL propertyFileUrl = new URL(propertyFile);
					property.setUrl(propertyFileUrl);
				}
				property.execute();
			}
			catch(Exception e) {
				return FormValidation.warning(Messages.ExtendedChoiceParameterDefinition_PropertyFileDoesntExist(), propertyFile);
			}

			if(   type.equals(PARAMETER_TYPE_MULTI_LEVEL_SINGLE_SELECT)
				 || type.equals(PARAMETER_TYPE_MULTI_LEVEL_MULTI_SELECT))
			{
				return FormValidation.ok();
			}
			else if(StringUtils.isNotBlank(propertyKey)) {
				if(project.getProperty(propertyKey) != null) {
					return FormValidation.ok();
				}
				else {
					return FormValidation.warning(Messages.ExtendedChoiceParameterDefinition_PropertyFileExistsButProvidedKeyIsInvalid(), propertyFile, propertyKey);
				}
			}
			else {
				return FormValidation.warning(Messages.ExtendedChoiceParameterDefinition_PropertyFileExistsButNoProvidedKey(), propertyFile);
			}
		}

		public FormValidation doCheckPropertyKey(@QueryParameter final String propertyFile, @QueryParameter final String propertyKey,
						@QueryParameter final String type) throws IOException, ServletException {
			return doCheckPropertyFile(propertyFile, propertyKey, type);
		}

		public FormValidation doCheckDefaultPropertyFile(@QueryParameter final String defaultPropertyFile,
				@QueryParameter final String defaultPropertyKey, @QueryParameter final String type) throws IOException, ServletException {
			return doCheckPropertyFile(defaultPropertyFile, defaultPropertyKey, type);
		}

		public FormValidation doCheckDefaultPropertyKey(@QueryParameter final String defaultPropertyFile,
						@QueryParameter final String defaultPropertyKey, @QueryParameter final String type) throws IOException, ServletException
		{
			return doCheckPropertyFile(defaultPropertyFile, defaultPropertyKey, type);
		}
	}

	private boolean quoteValue;
	
	private boolean svnPath;
	
	private boolean roleBasedFilter;

	private int visibleItemCount;

	private String type;

	private String value;
	
	private boolean bindedSelect;
		
	private String propertyFile;

	private String propertyKey;

	private String defaultValue;

	private String defaultPropertyFile;

	private String defaultPropertyKey;
	
	private String multiSelectDelimiter;
	
	private String bindFieldName;
	
	private String projectName;
	
	private String svnUrl;
	
	private String svnUserName;
	
	private String svnPassword;

	@DataBoundConstructor
	public ExtendedChoiceParameterDefinition(String name, String type, String value, String propertyFile, String propertyKey, String defaultValue,
			String defaultPropertyFile, String defaultPropertyKey, boolean quoteValue, int visibleItemCount, String description,
			String multiSelectDelimiter, String bindFieldName, boolean svnPath, String svnUrl, String svnUserName, String svnPassword, String projectName, boolean bindedSelect, boolean roleBasedFilter) {
		super(name, description);
		this.type = type;

		this.propertyFile = propertyFile;
		this.propertyKey = propertyKey;

		this.defaultPropertyFile = defaultPropertyFile;
		this.defaultPropertyKey = defaultPropertyKey;
		this.value = value;
		this.defaultValue = defaultValue;
		this.quoteValue = quoteValue;
		this.svnPath = svnPath;
		this.roleBasedFilter = roleBasedFilter;
		this.bindFieldName = bindFieldName;
		this.bindedSelect = bindedSelect;		
		if(visibleItemCount == 0) {
			visibleItemCount = 5;
		}
		this.visibleItemCount = visibleItemCount;
		
		if(multiSelectDelimiter.equals("")) {
			multiSelectDelimiter = ",";
		}
		this.multiSelectDelimiter = multiSelectDelimiter;
		this.projectName = projectName;
		this.svnUrl = svnUrl;
		this.svnUserName = svnUserName;
		this.svnPassword = svnPassword;
	}

	private Map<String, Boolean> computeDefaultValueMap() {
		Map<String, Boolean> defaultValueMap = null;
		String effectiveDefaultValue = getEffectiveDefaultValue();
		if(!StringUtils.isBlank(effectiveDefaultValue)) {
			defaultValueMap = new HashMap<String, Boolean>();
			String[] defaultValues = StringUtils.split(effectiveDefaultValue, ',');
			for(String value: defaultValues) {
				defaultValueMap.put(StringUtils.trim(value), true);
			}
		}
		return defaultValueMap;
	}

	@Override
	public ParameterValue createValue(StaplerRequest request) {
		String[] requestValues = request.getParameterValues(getName());
		if(requestValues == null || requestValues.length == 0) {
			return getDefaultParameterValue();
		}
		if(PARAMETER_TYPE_TEXT_BOX.equals(type)) {
			return new ExtendedChoiceParameterValue(getName(), requestValues[0]);
		}
		else {
			String valueStr = getEffectiveValue();
			if(valueStr != null) {
				List<String> result = new ArrayList<String>();

				String[] values = valueStr.split(",");
				Set<String> valueSet = new HashSet<String>();
				for(String value: values) {
					valueSet.add(value);
				}

				for(String requestValue: requestValues) {
					if(valueSet.contains(requestValue)) {
						result.add(requestValue);
					}
				}

				return new ExtendedChoiceParameterValue(getName(), StringUtils.join(result, getMultiSelectDelimiter()));
			}
		}
		return null;
	}
	
	@Override
	public ParameterValue createValue(StaplerRequest request, JSONObject jO) {		
		Object value = jO.get("value");
		String strValue = "";
		if(value instanceof String) {
			strValue = (String)value;
		}
		else if(value instanceof JSONArray) {
			JSONArray jsonValues = (JSONArray)value;
			if (   type.equals(PARAMETER_TYPE_MULTI_LEVEL_SINGLE_SELECT)
				  || type.equals(PARAMETER_TYPE_MULTI_LEVEL_MULTI_SELECT))
			{
				final int valuesBetweenLevels = this.value.split(",").length;
				
				Iterator it = jsonValues.iterator();
				for (int i = 1; it.hasNext(); i++)
				{
					String nextValue = it.next().toString();
					if (i % valuesBetweenLevels == 0)
					{
						if (strValue.length() > 0)
						{
							strValue += getMultiSelectDelimiter();
						}
						strValue += nextValue;
					}
				}
			}
			else
			{
				strValue = StringUtils.join(jsonValues.iterator(), getMultiSelectDelimiter());
			}
		}

		if(quoteValue) {
			strValue = "\"" + strValue + "\"";
		}
		return new ExtendedChoiceParameterValue(getName(), strValue);
	}

	@Override
	public ParameterValue getDefaultParameterValue() {
		String defaultValue = getEffectiveDefaultValue();
		if(!StringUtils.isBlank(defaultValue)) {
			if(quoteValue) {
				defaultValue = "\"" + defaultValue + "\"";
			}
			return new ExtendedChoiceParameterValue(getName(), defaultValue);
		}
		return super.getDefaultParameterValue();
	}
	
	
	private String getSvnUrlListContent(String svnUrl, String svnUserName, String svnPassword, String svnPath){
		
		SVNRepository repository = null;
		String svn_list_content="";
		try {
			repository = SVNRepositoryFactory.create( SVNURL.parseURIDecoded( svnUrl ) );
			ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( svnUserName , svnPassword );
			repository.setAuthenticationManager( authManager );
			Collection entries = repository.getDir( svnPath, -1 , null , (Collection) null );
			Iterator iterator = entries.iterator( );
			SortedMap<Long,String> sortedSvnListMap = new TreeMap<Long,String>();
			while ( iterator.hasNext( ) ) {
				SVNDirEntry entry = ( SVNDirEntry ) iterator.next( );
				sortedSvnListMap.put(entry.getRevision(),entry.getName());
			}
			
			entries=sortedSvnListMap.values();
			iterator = entries.iterator( );
			
			while ( iterator.hasNext( ) ) {
				svn_list_content = ( String ) iterator.next( ) + "," + svn_list_content;
			}
			
			
			return svn_list_content;
		}
		catch(Exception e) {
			
			return "";
		}
	}
	
	@JavaScriptMethod
	public String computeValue(String propertyFilePath, String propertyKey, String src_name) {
	//public String computeValue(String propertyFilePath, String propertyKey, String type, String visibleItemCount, boolean svnPath, String src_name, String svnUrl, String svnUserName) {
		if(!StringUtils.isBlank(propertyFilePath) && !StringUtils.isBlank(propertyKey) && !StringUtils.isBlank(this.type)) {
			try {
				String file_content="";
				if (this.svnPath)
				{
					if (src_name.equals("trunk"))
					{
						file_content="trunk";
					}
					else
					{
						file_content=getSvnUrlListContent(this.svnUrl,this.svnUserName,this.svnPassword,src_name);
					}
					
				}
				else{
					Project project = new Project();
					Property property = new Property();
					property.setProject(project);

					File propertyFile = new File(propertyFilePath);
				
					if(propertyFile.exists()) {
						property.setFile(propertyFile);
					}
					else {
						URL propertyFileUrl = new URL(propertyFilePath);
						property.setUrl(propertyFileUrl);
					}
					property.execute();
					
					if (!StringUtils.isBlank(this.projectName))
					{
						propertyKey=propertyKey + "_" + this.projectName;
					}
				
					file_content=project.getProperty(propertyKey);
				}
				
				file_content = "Select" + "," + file_content;
					
				String[] list_arr = file_content.split(",");
				
				String result="";
				
				if (this.type.equals("PT_SINGLE_SELECT"))
				{
					result+="<select name=\"value\" id=\"selectlist\" class=\"" + this.bindedSelect + "\">"; // class=\"true\"	
					for (int i = 0; i < list_arr.length; ++i)
					{
				
						result+="<option value=\"" + list_arr[i] + "\">" + list_arr[i] + "</option>";
					
					}
					result+="</select>";
				}
				else if (this.type.equals("PT_MULTI_SELECT"))
				{
					result+="<select name=\"value\" multiple=\"multiple\" size=\"" + this.visibleItemCount + "\">";
					for (int i = 0; i < list_arr.length; ++i)
					{
				
						result+="<option value=\"" + list_arr[i] + "\">" + list_arr[i] + "</option>";
					
					}
					result+="</select>";
				}
				else if (this.type.equals("PT_CHECKBOX"))
				{
					String name="module";
					result+="<div id=\"ecp_" + name + "\" padding-left:25px\">";
					int index=0;
					result+="<table id=\"tbl_ecp_" + name + "\">";
					for (int value = 0; value < list_arr.length; ++value)
					{
						result+="<tr id=\"ecp_" + name + "_" + index + "\" style=\"white-space:nowrap\">";
						result+="<td>";
						result+="<input type=\"checkbox\" name=\"value\" title=\"" + list_arr[value] + "\" value=\"" + list_arr[value] + "\" json=\"" + list_arr[value] + "\">" + list_arr[value] + "<br>";
						result+="</td>";
						result+="</tr>";
						index=index + 1;
					}
					result+="</table>";
					result+="</div>";
					result+="<script>";
					result+="<![CDATA[  ";
					result+="(function() {";
					result+="var f = function() {";
					result+="var height = 0;";
					result+="var maxCount = " + index + ";";
					result+="if(maxCount > " + this.visibleItemCount + ") {";
					result+="maxCount = " + this.visibleItemCount;
					result+="}";
	  			
					result+="if(maxCount > 0 && document.getElementById(\"ecp_" + name +"_0\").offsetHeight !=0) {";
		  			result+="for(var i=0; i< maxCount; i++) {";
		  			result+="height += document.getElementById(\"ecp_" + name + "_\" + i).offsetHeight + 3;";
		  			result+="}";
					result+="}";
					result+="else {";
	  				result+="height = maxCount * 25.5;";
					result+="}";
					result+="height = Math.floor(height);";
					result+="document.getElementById(\"ecp_" + name + "\").style.height = height + \"px\";";
					result+="};";
	  		
					result+="f();";
					result+="})();";
					result+="]]>  		";
					result+="</script>";
				
							
				}
				
				
				return result;
			}
			catch(Exception e) {

			}
		}
		
		return "<select name=\"value\"><option value=\"select\">Select</option></select>";
	}
	
	private List<String> getRole(String role_level)
	{
		List<String> assignedRoles = new ArrayList<String>();
		try {
			// get logged-in user id
			String userId = User.current().getId();

			// get assigned role(s)
			AuthorizationStrategy authStrategy = Hudson.getInstance().getAuthorizationStrategy();
			User user = Hudson.getInstance().getUser(userId);

			if (authStrategy instanceof RoleBasedAuthorizationStrategy) {
				RoleBasedAuthorizationStrategy roleBasedAuthStrategy = (RoleBasedAuthorizationStrategy) authStrategy;
				SortedMap<Role, Set<String>> globalRoles = roleBasedAuthStrategy.getGrantedRoles(role_level);
				for (Map.Entry<Role, Set<String>> entry : globalRoles.entrySet()) {         
					Set<String> set = entry.getValue();
						if (set.contains(userId)) {
							String role = entry.getKey().getName();
							assignedRoles.add(role);
							}                
				}
			}
		
		}
		catch(Exception e){
		}
		finally{
			return assignedRoles;
		}
	}
	
	private List<String> getAllRoles()
	{
		List<String> allRoles = new ArrayList<String>();
		
		allRoles = getRole("globalRoles");
		allRoles.addAll(getRole("projectRoles"));
		List<String> uniqueList = new ArrayList<String>();
		
		for(String value : allRoles)
		{
			if (!uniqueList.contains(value))
			{
				uniqueList.add(value);
			}
		}
		return uniqueList;
	}
	
	// note that computeValue is not called by multiLevel.jelly
	private String computeValue(String value, String propertyFilePath, String propertyKey, String projectName) {
		List<String> allRoles = getAllRoles();
		String modedContent = "Select,";
		if(!StringUtils.isBlank(propertyFile) && !StringUtils.isBlank(propertyKey)) {
			try {

				Project project = new Project();
				Property property = new Property();
				property.setProject(project);

				File propertyFile = new File(propertyFilePath);
				if(propertyFile.exists()) {
					property.setFile(propertyFile);
				}
				else {
					URL propertyFileUrl = new URL(propertyFilePath);
					property.setUrl(propertyFileUrl);
				}
				property.execute();		
				if( (this.svnPath) || (!this.roleBasedFilter) )
				{
					modedContent += project.getProperty(propertyKey);
					return modedContent;
				}
				else
				{
					String[] actualContent = project.getProperty(propertyKey).split(",");
					if (allRoles.contains("admin")){
						modedContent += project.getProperty(propertyKey);
					}
					else
					{
						for (String val : actualContent) {
							if(projectName.isEmpty()) {
								if (allRoles.contains(val)){
									modedContent += val + ",";
								}
							}
							else {
								if (allRoles.contains(projectName + "_" + val)){
									modedContent += val + ",";
								}
								
							}
						}				
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		else if(!StringUtils.isBlank(value)) {
			if( (this.svnPath) || (!this.roleBasedFilter) )
			{
				 modedContent = value;
			}
			else
			{
				String[] actualContent = value.split(",");
				for (String val : actualContent) {
					if(projectName.isEmpty()) {
						if (allRoles.contains(val)){
							modedContent += val + ",";
						}
					}
					else {
						if (allRoles.contains(projectName + "_" + val)){
							modedContent += val + ",";
						}
						
					}
				}			
			}
		}
		return modedContent;
	}

	@Override
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getBindFieldName() {
		return bindFieldName;
	}
	
	public void setBindFieldName(String bindFieldName) {
		this.bindFieldName = bindFieldName;
	}
	
	public String getProjectName() {
		return projectName;
	}
	
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	
	public String getSvnUrl() {
		return svnUrl;
	}
	
	public void setSvnUrl(String svnUrl) {
		this.svnUrl = svnUrl;
	}
	
	public String getSvnUserName() {
		return svnUserName;
	}
	
	public void setSvnUserName(String svnUserName) {
		this.svnUserName = svnUserName;
	}
	
	public String getSvnPassword() {
		return svnPassword;
	}
	
	public void setSvnPassword(String svnPassword) {
		this.svnPassword = svnPassword;
	}

	public String getEffectiveDefaultValue() {
		return computeValue(defaultValue, defaultPropertyFile, defaultPropertyKey, projectName);
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getPropertyFile() {
		return propertyFile;
	}

	public void setPropertyFile(String propertyFile) {
		this.propertyFile = propertyFile;
	}

	public String getDefaultPropertyKey() {
		return defaultPropertyKey;
	}

	public void setDefaultPropertyKey(String defaultPropertyKey) {
		this.defaultPropertyKey = defaultPropertyKey;
	}

	public String getEffectiveValue() {
		return computeValue(value, propertyFile, propertyKey, projectName);
	}
	
	private ArrayList<Integer> columnIndicesForDropDowns(String[] headerColumns)
	{
		ArrayList<Integer> columnIndicesForDropDowns = new ArrayList<Integer>();
		
		String[] dropDownNames = value.split(",");

		for (String dropDownName : dropDownNames)
		{
			for (int i = 0; i < headerColumns.length; ++i)
			{
				if (headerColumns[i].equals(dropDownName))
				{
					columnIndicesForDropDowns.add(new Integer(i));
				}
			}
		}
		
		return columnIndicesForDropDowns;
	}
	
	LinkedHashMap<String, LinkedHashSet<String>> calculateChoicesByDropdownId() throws Exception
	{
		List<String[]> fileLines =
			new CSVReader(new FileReader(propertyFile), '\t').readAll();

		if (fileLines.size() < 2)
		{
			throw new Exception("Multi level tab delimited file must have at least 2 "
							+ "lines (one for the header, and one or more for the data)");
		}

		ArrayList<Integer> columnIndicesForDropDowns =
						columnIndicesForDropDowns(fileLines.get(0));
		
		List<String[]> dataLines = fileLines.subList(1, fileLines.size());

		LinkedHashMap<String, LinkedHashSet<String>> choicesByDropdownId =
						new LinkedHashMap<String, LinkedHashSet<String>>();

		String prefix = getName() + " dropdown MultiLevelMultiSelect 0";
		choicesByDropdownId.put(prefix, new LinkedHashSet<String>());

		for (int i=0; i < columnIndicesForDropDowns.size(); ++i)
		{
			String prettyCurrentColumnName = value.split(",")[i];
			prettyCurrentColumnName = prettyCurrentColumnName.toLowerCase();
			prettyCurrentColumnName = prettyCurrentColumnName.replace("_", " ");

			for (String[] dataLine : dataLines)
			{
				String priorLevelDropdownId = prefix;
				String currentLevelDropdownId = prefix;

				int column = 0;
				for (int j=0; j <= i; ++j)
				{
					column = columnIndicesForDropDowns.get(j);

					if (j < i)
					{
						priorLevelDropdownId += " " + dataLine[column];
					}
					currentLevelDropdownId += " " + dataLine[column];
				}					
				if (i != columnIndicesForDropDowns.size() - 1)
				{
					choicesByDropdownId.put(currentLevelDropdownId, new LinkedHashSet<String>());
				}
				LinkedHashSet<String> choicesForPriorDropdown
								= choicesByDropdownId.get(priorLevelDropdownId);
				choicesForPriorDropdown.add("Select a " + prettyCurrentColumnName
																		+ "...");
				choicesForPriorDropdown.add(dataLine[column]);
			}				
		}

		return choicesByDropdownId;
	}
	
	public String getMultiLevelDropdownIds() throws Exception
	{
		String dropdownIds = new String();
		
		LinkedHashMap<String, LinkedHashSet<String>> choicesByDropdownId = 
						calculateChoicesByDropdownId();
		
		for (String id : choicesByDropdownId.keySet())
		{
			if (dropdownIds.length() > 0)
			{
				dropdownIds += ",";
			}
			dropdownIds += id;
		}
				
		return dropdownIds;
		
		/* dropdownIds is of a form like this:
		return name + " dropdown MultiLevelMultiSelect 0," 
				   // next select the source of the genome -- each genome gets a seperate dropdown id:"
				 + name + " dropdown MultiLevelMultiSelect 0 HG18,dropdown MultiLevelMultiSelect 0 ZZ23,"
				 // next select the cell type of the source -- each source gets a seperate dropdown id
				 + name + " dropdown MultiLevelMultiSelect 0 HG18 Diffuse large B-cell lymphoma, dropdown MultiLevelMultiSelect 0 HG18 Multiple Myeloma,"
				 + name + " dropdown MultiLevelMultiSelect 0 ZZ23 Neuroblastoma,"
				 // next select the name from the cell type -- each cell type gets a seperate dropdown id
				 + name + " dropdown MultiLevelMultiSelect 0 HG18 Diffuse large B-cell lymphoma LY1,"
				 + name + " dropdown MultiLevelMultiSelect 0 HG18 Multiple Myeloma MM1S,"
				 + name + " dropdown MultiLevelMultiSelect 0 ZZ23 Neuroblastoma BE2C,"
				 + name + " dropdown MultiLevelMultiSelect 0 ZZ23 Neuroblastoma SKNAS";*/
	}
	
	public Map<String, String> getChoicesByDropdownId() throws Exception
	{
		LinkedHashMap<String, LinkedHashSet<String>> choicesByDropdownId = 
			calculateChoicesByDropdownId();
		
		Map<String, String> collapsedMap = new LinkedHashMap<String, String>();
		
		for (String dropdownId : choicesByDropdownId.keySet())
		{
			String choices = new String();
			for (String choice : choicesByDropdownId.get(dropdownId))
			{
				if (choices.length() > 0)
				{
					choices += ",";
				}
				choices += choice;
			}
			
			collapsedMap.put(dropdownId, choices);
		}
				
		/* collapsedMap is of a form like this:
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0", "Select a genome...,HG18,ZZ23");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 HG18", "Select a source...,Diffuse large B-cell lymphoma,Multiple Myeloma");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 ZZ23", "Select a source...,Neuroblastoma");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 HG18 Diffuse large B-cell lymphoma","Select a cell type...,LY1");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 HG18 Multiple Myeloma","Select a cell type...,MM1S");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 ZZ23 Neuroblastoma","Select a cell type...,BE2C,SKNAS");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 HG18 Diffuse large B-cell lymphoma LY1","Select a name...,LY1_BCL6_DMSO,LY1_BCL6_JQ1");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 HG18 Multiple Myeloma MM1S", "Select a name...,MM1S_BRD4_150nM_JQ1,MM1S_BRD4_500nM_JQ1");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 ZZ23 Neuroblastoma BE2C", "Select a name...,BE2C_BRD4");
		collapsedMap.put(name + " dropdown MultiLevelMultiSelect 0 ZZ23 Neuroblastoma SKNAS", "Select a name...,SKNAS_H3K4ME3");
		*/
		
		return collapsedMap;
	}
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getPropertyKey() {
		return propertyKey;
	}

	public void setPropertyKey(String propertyKey) {
		this.propertyKey = propertyKey;
	}

	public String getDefaultPropertyFile() {
		return defaultPropertyFile;
	}

	public boolean isQuoteValue() {
		return quoteValue;
	}

	public void setQuoteValue(boolean quoteValue) {
		this.quoteValue = quoteValue;
	}
	
	public boolean isBindedSelect() {
		return bindedSelect;
	}

	public void setBindedSelect(boolean bindedSelect) {
		this.bindedSelect = bindedSelect;
	}

	public boolean isSvnPath() {
		return svnPath;
	}

	public void setSvnPath(boolean svnPath) {
		this.svnPath = svnPath;
	}
	
	public boolean isRoleBasedFilter() {
		return roleBasedFilter;
	}

	public void setRoleBasedFilter(boolean roleBasedFilter) {
		this.roleBasedFilter = roleBasedFilter;
	}
	
	public int getVisibleItemCount() {
		return visibleItemCount;
	}

	public void setVisibleItemCount(int visibleItemCount) {
		this.visibleItemCount = visibleItemCount;
	}
	
	public String getMultiSelectDelimiter() {
		return this.multiSelectDelimiter;
	}
	
	public void setMultiSelectDelimiter(final String multiSelectDelimiter) {
		this.multiSelectDelimiter = multiSelectDelimiter;
	}

	public void setDefaultPropertyFile(String defaultPropertyFile) {
		this.defaultPropertyFile = defaultPropertyFile;
	}

	public Map<String, Boolean> getDefaultValueMap() {
		return computeDefaultValueMap();
	}
}
