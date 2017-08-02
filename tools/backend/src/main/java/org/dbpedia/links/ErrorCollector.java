/**
 * 
 */
package org.dbpedia.links;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

/**
 * Logs errors of checked files in repositories during validation phase.
 * Outputs the information to JSON. 
 * @author Amit
 *
 */
public class ErrorCollector {

	Table<String,String,List<String>> errorsTable;
	
	
	/**
	 * 
	 */
	public ErrorCollector() 
	{
		errorsTable = TreeBasedTable.create();
	}
	
	
	/*
	 * a table-entry/record  for a detected file in a repo, to report also files that exhibit no errors.
	 */
	public void addRepoFile(String repoName,String fileName)
	{
		List<String> repoFileErrors  = 	errorsTable.get(repoName, fileName);
		if(repoFileErrors == null)
		{
			errorsTable.put(repoName,fileName, new ArrayList<String>());
			
		}
	}
	
	
	public void addError(String repoName, String fileName, String errorMsg )
	{
		List<String> repoFileErrors  = 	errorsTable.get(repoName, fileName);
		if(repoFileErrors == null)
		{
			repoFileErrors = new ArrayList<String>();
		}
		repoFileErrors.add(errorMsg);
		errorsTable.put(repoName, fileName,repoFileErrors);
	}
	
	@SuppressWarnings("unchecked")
	public JSONArray toJSONArray()
	{
		JSONArray errosJA = new JSONArray();
		Map<String, Map<String, List<String>>> errorsMap =  errorsTable.rowMap();
		for(Map.Entry<String, Map<String, List<String>>> repoErrors: errorsMap.entrySet())
		{
			
			
			JSONObject repoErrorsJO = new JSONObject();
			
			JSONObject fileErrorsJO = new JSONObject();
			
			repoErrorsJO.put("entry", repoErrors.getKey());
			for(Map.Entry<String, List<String>> fileErrorsE: repoErrors.getValue().entrySet())
			{
				JSONArray fileErrorListJA = new JSONArray();
				fileErrorListJA.addAll(fileErrorsE.getValue());
				fileErrorsJO.put(fileErrorsE.getKey(), fileErrorListJA);
				
			}
			
			
			repoErrorsJO.put("error msgs",fileErrorsJO);
			

			errosJA.add(repoErrorsJO);
		}
		
		
		return errosJA;
	}
	

}
