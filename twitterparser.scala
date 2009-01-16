package org.scala_twitter
import java.net._
import java.io._
import scala.util.parsing.json._
import scala.collection.jcl.ArrayList
/**
 * Connects to twitter JSON API
 */
class TwitterParser(username:String,password:String){
	private val http = "http://twitter.com/"
	private val connect = new Auth(username,password);
	private def getConnection(turl:String):URLConnection = {
		try{
			//Fail fast on bad usernames, passwords, or urls
			System.setProperty("http.maxRedirects","2");
			Authenticator.setDefault(connect)
			var url:URL = new URL(turl)
			url.openConnection
		}
		catch{
			case e:ProtocolException => {println("username or password incorrect");null}
			case e:MalformedURLException => {println("url was bad");null}
			case e:Exception => {println(e.getMessage());null}
		}
	}
	/**
	 * Pull line from twitter url JSON feed and parse it
	 */
	private def get(turl:String):Array[Tweet] = {
		if (turl == null) return null;
		var connection:URLConnection = getConnection(turl)
		if (connection == null) return null;
		var content:InputStream = connection.getInputStream();
		var reader:BufferedReader = new BufferedReader(new InputStreamReader(content));
		var line:String = reader.readLine();
		var json = JSON.parseFull(line);
		var response:List[List[Tuple2[String,Any]]] = null
		json match{
			case Some(j)=>{
				if (j.isInstanceOf[List[List[Tuple2[String,Any]]]]) response = j.asInstanceOf[List[List[Tuple2[String,Any]]]]
				else if (j.isInstanceOf[List[Tuple2[String,Any]]]) response = List(j.asInstanceOf[List[Tuple2[String,Any]]])
			}
			case None => null
		}
		var tweets:ArrayList[Tweet] = new ArrayList[Tweet]
		if (response != null){
			response.foreach(tweet => {
			var i = new Tweet
			i.parse(tweet)
			tweets.add(i)
		 })
		}
		return tweets.toArray[Tweet]
	}
	/**
	 * Push an update to twitter
	 */
	private def push(url:String,update:String):Unit = {
		var connection:URLConnection = getConnection(url)
		if (connection == null) return;
		connection.setDoOutput(true)
		var wr = new OutputStreamWriter(connection.getOutputStream())
		wr.write(URLEncoder.encode("status","UTF-8") + "=" + URLEncoder.encode(update,"UTF-8"))
		wr.flush()
		var rd = new BufferedReader(new InputStreamReader(connection.getInputStream()))
		rd.readLine()
		wr.close()
		rd.close()
	}
	//Only necessary for Authenticator
	class Auth(username:String, password:String) extends Authenticator{
		override def getPasswordAuthentication():PasswordAuthentication = {new PasswordAuthentication(username,password.toCharArray())}
	}
	/**
	 * Get a timeline
	 */
	def getTimeline(timeline:String):Array[Tweet] = {
		var url:String = http+"statuses/"
		if (timeline == "friends" || timeline == "public" || timeline == "user"){
			url+=timeline+"_timeline.json"
		}
		else {
		 url = null
		}
		return get(url)
	}
	/**
	 * Send update to twitter
	 */
	def update(status:String):Unit = push("http://twitter.com/statuses/update.json",status)
	/**
	 * Delete message by id
	 */
	def delete(id:Double):Unit = push("http://twitter.com/statuses/destroy/"+id+".json","")
	/**
	 * Get a single message by id
	 */
	def getMsg(id:Double):Unit = get("http://twitter.com/statuses/show/"+id+".json")
	/**
	 * Shorten a url with is.gd
	 */
	def shortenURL(url:String):String = {
		var makeShort = new URL("http://is.gd/api.php?longurl="+url)
		var connection = new BufferedReader(new InputStreamReader(makeShort.openConnection().getInputStream()))
		var shortUrl:String = connection.readLine()
		return shortUrl
	}
}
