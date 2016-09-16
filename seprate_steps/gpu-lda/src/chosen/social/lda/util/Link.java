package chosen.social.lda.util;

public class Link {
	
	//Link中存放的是ID对应的index，而不是本身
	public int follower; // map id to TwitterIDUtil 's index, in link ,index are used
	public int followee; 
	
	public Link() {
	  followee = follower = -1;
  }
	
	public Link (int follower, int followee) {
	  this.follower = follower;
	  this.followee = followee;
	}
	
  public Link(String follower,String followee) {
//    int followerID = Integer.valueOf(follower);
//    int followeeID = Integer.valueOf(followee);
    this.followee = TwitterIDUtil.getIndex(followee);
    this.follower = TwitterIDUtil.getIndex(follower);
  }
	
  //获取index对应的ID
  public static String getLinkeeVaule(int index) {
    return TwitterIDUtil.getID(index);
  }
  
  //判断两个Link是否相同
	public boolean equals(Link link) {
		if (link.follower == this.follower && link.followee == this.followee ) {
			return true;
		}
		return false;
	}
}
