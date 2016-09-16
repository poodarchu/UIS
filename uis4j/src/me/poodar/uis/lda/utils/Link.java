package me.poodar.uis.lda.utils;

public class Link {
	
	public int follower; // map id to TwitterIDUtil 's index, in link ,index are used
	public int followee; 
	
	public Link() {
    // TODO Auto-generated constructor stub
	  followee = follower = -1;
  }
	
	public Link (int follower, int followee) {
	  this.follower = follower;
	  this.followee = followee;
	}
	
  public Link(String follower,String followee) {
    int followerID = Integer.valueOf(follower);
    int followeeID = Integer.valueOf(followee);
    this.followee = TwitterIDUtil.getIndex(String.valueOf(followeeID));
    this.follower = TwitterIDUtil.getIndex(String.valueOf(followerID));
  }
	
  public static String getLinkeeVaule(int index) {
    return TwitterIDUtil.getID(index);
  }
  
	public boolean equals(Link link) {
		if (link.follower == this.follower && link.followee == this.followee ) {
			return true;
		}
		return false;
	}
}
