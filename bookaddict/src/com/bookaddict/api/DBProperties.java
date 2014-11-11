package com.bookaddict.api;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class DBProperties {
	public static MongoClient mongo;
	public static DB db;
	
	/*public DBProperties() {
		try {
			mongo = new MongoClient( "localhost" , 27017 );
			db = mongo.getDB("bookaddict");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	public static DB getDb() {
		try {
			mongo = new MongoClient( "localhost" , 27017 );
			db = mongo.getDB("bookaddict");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return db;
	}
}
