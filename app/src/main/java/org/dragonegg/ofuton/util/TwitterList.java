package org.dragonegg.ofuton.util;

public class TwitterList {
	private long userId;
	private long listId;
	private String name;
	private String fullName;
	private String creatorName;

	public TwitterList(long uid, long l, String name, String fname){
		userId = uid;
		listId = l;
		this.name = name;
		fullName = fname;
	}

	public TwitterList(long uid, long l, String name, String fname, String cname){
		userId = uid;
		listId = l;
		this.name = name;
		fullName = fname;
		creatorName = cname;
	}

	public long getUserId() {
		return userId;
	}

	public long getListId() {
		return listId;
	}

	public String getName() {
		return name;
	}

	public String getFullName() {
		return fullName;
	}
	
	public String getCreatorName() { return creatorName; }
}
