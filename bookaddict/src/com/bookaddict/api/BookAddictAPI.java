package com.bookaddict.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


@RestController
//@RequestMapping("/v1/")
public class BookAddictAPI {
	
	public static final String GOOGLE_BOOKS_API_BASEURL = "https://www.googleapis.com/books/v1/volumes";
	public static final String USER_PROFILE_COLLECTION_NAME = "User_Profile";
	public static final String FAVOURITES_READTO_COLLECTION_NAME = "Favourites_Read";
	public static final String REVIEWS_COLLECTION_NAME = "Reviews";
	public static final String RATING_COLLECTION_NAME = "Ratings";
	public static final String LOGS_COLLECTION_NAME = "Logs";
	public static final String DEFAULT_USERTYPE = "Limited";
	public static final String PREMIUM_USERTYPE = "Premium";
	public static final int API_USAGE_LIMIT = 250;
	
	@RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
	public String welcome() {
		return "Welcome to \"bookaddict.com\" Custom API !!!";
	}
	
	@RequestMapping(value = "/search", method = RequestMethod.GET, produces = "application/json")
	public String searchByName(@RequestParam(required = false, value = "title") String title, @RequestParam(required = false, value = "isbn") String isbn, @RequestParam(required = false, value = "startIndex") Integer startIndex,@RequestParam(required = false, value = "maxResults") Integer maxResults, @RequestParam("apiKey") String apiKey) throws JsonProcessingException {
		return getSearchResults(title.replaceAll(" ", "%20") , isbn, startIndex, maxResults, apiKey);
	}
	
	@RequestMapping(value = "/search/{bookId}", method = RequestMethod.GET, produces = "application/json")
	public String searchBookDetails(@PathVariable("bookId") String bookId) throws JsonProcessingException {
		return getBookDetails(bookId);
	}
	
	@RequestMapping(value="/register", method = RequestMethod.POST, produces = "application/json")
	public String createNewUser(@RequestParam("fname") String fname, @RequestParam("lname") String lname, @RequestParam("email") String email, @RequestParam("password") String password) throws JsonProcessingException {
		return registerUser(fname.replaceAll("%20", " "), lname.replaceAll("%20", " "), email, password, DEFAULT_USERTYPE, "n", "");
	}
	
	@RequestMapping(value="/auth", method = RequestMethod.GET, produces = "application/json")
	public String authUser(@RequestParam("email") String email, @RequestParam("password") String password) throws JsonProcessingException {
		return authenticateUser(email, password);
	}
	
	@RequestMapping(value="/profiles", method = RequestMethod.GET, produces = "application/json")
	public String profile(@RequestParam("email") String email) throws JsonProcessingException {
		return getProfile(email);
	}
	
	@RequestMapping(value="/changePassword", method = RequestMethod.PUT)
	public String password(@RequestParam("email") String email, @RequestParam("currentPassword") String currentPassword, @RequestParam("newPassword") String newPassword) throws JsonProcessingException {
		return changePassword(email, currentPassword, newPassword);
	}
	
	@RequestMapping(value="/updateUserDetails", method = RequestMethod.PUT)
	public String updateUser(@RequestParam("fname") String fname, @RequestParam("lname") String lname, @RequestParam("email") String email) throws JsonProcessingException {
		return updateUserDetails(fname, lname, email);
	}
	
	@RequestMapping(value="/updateUserType", method = RequestMethod.PUT)
	public String changeUserType(@RequestParam(required = false, value = "email") String email, @RequestParam(required = false, value = "apiKey") String apiKey, @RequestParam("userType") String userType) throws JsonProcessingException {
		return updateUserType(email, apiKey, userType);
	}
	
	@RequestMapping(value="/restrictUser", method = RequestMethod.PUT)
	public String restrictUserForDay(@RequestParam(required = false, value = "email") String email, @RequestParam(required = false, value = "apiKey") String apiKey) throws JsonProcessingException {
		return restrictUser(email, apiKey);
	}
	
	@RequestMapping(value="/fav_read/add", method = RequestMethod.POST, produces = "application/json")
	public String addTofavourites(@RequestParam("email") String email, @RequestParam("bookId") String bookId, @RequestParam("type") String type) throws JsonProcessingException {
		return addToFavouritesAndToRead(email, bookId, type);
	}
	
	@RequestMapping(value="/fav_read", method = RequestMethod.GET, produces = "application/json")
	public String favourites(@RequestParam("email") String email, @RequestParam("type") String type) throws JsonProcessingException {
		return getFavouritesAndToRead(email, type);
	}
	
	@RequestMapping(value="/fav_read/remove", method = RequestMethod.DELETE, produces = "application/json")
	public String removeFromfavourites(@RequestParam("email") String email, @RequestParam("bookId") String bookId, @RequestParam("type") String type) throws JsonProcessingException {
		return removeFromFavouritesAndToRead(email, bookId, type);
	}
	
	@RequestMapping(value="/reviews/add", method = RequestMethod.POST, produces = "application/json")
	public String addReview(@RequestParam("bookId") String bookId, @RequestParam("isbn") String isbn, @RequestParam("email") String email, @RequestParam(required = false, value = "review") String review, @RequestParam("rating") String rating, @RequestParam("apiKey") String apiKey) throws JsonProcessingException {
		return addReviewForBook(bookId, isbn, email, review, rating ,apiKey);
	}
	
	@RequestMapping(value="/logs", method = RequestMethod.GET, produces = "application/json")
	public String logs(@RequestParam(required = false, value = "email") String email, @RequestParam(required = false, value = "apiKey") String apiKey) throws JsonProcessingException {
		return getLogs(email, apiKey);
	}
	
	public String getSearchResults(String title, String isbn, Integer startIndex, Integer maxResults, String apiKey) throws JsonProcessingException {
		Map<String, Object> jsonReviews = new LinkedHashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		Object jsonTest = null;
		Map<String, Object> userDetails = getUserDetailsFromAPIKey(apiKey);
		DateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy");
		Date date = new Date();
		if(userDetails.get("userType").equals("Limited")) {
			if(userDetails.get("isRestrictedFlag").toString().equals("y")) {
				if(userDetails.get("dateRestricted").equals(dateFormat.format(date))) {
					jsonReviews.put("restricted", "y");
					return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonReviews);
				}
				else {
					DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
					BasicDBObject searchQuery = new BasicDBObject();
					searchQuery.put("apiKey", apiKey);
					BasicDBObject newDocumentItems = new BasicDBObject();
					newDocumentItems.put("isRestrictedFlag", "n");
					newDocumentItems.put("dateRestricted", "");
					BasicDBObject newDocument = new BasicDBObject();
					newDocument.put("$set", newDocumentItems);
					collection.update(searchQuery, newDocument);
				}
			}
			else if(getAPIUsageForUserByAPIKey(apiKey) >= API_USAGE_LIMIT) {
				DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
				BasicDBObject searchQuery = new BasicDBObject();
				searchQuery.put("apiKey", apiKey);
				BasicDBObject newDocumentItems = new BasicDBObject();
				newDocumentItems.put("isRestrictedFlag", "y");
				newDocumentItems.put("dateRestricted", dateFormat.format(date));
				BasicDBObject newDocument = new BasicDBObject();
				newDocument.put("$set", newDocumentItems);
				collection.update(searchQuery, newDocument);
				jsonReviews.put("restricted", "y");
				return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonReviews);
			}
		}
		String searchURL = null;
		String output, res="";
		String bookDetails = "";
		if(title!=null && isbn != null) {
			searchURL = GOOGLE_BOOKS_API_BASEURL + "?q=intitle:" + title + "+isbn:" + isbn;
			bookDetails = "book " + title.replaceAll("%20", " ") + " and ISBN " + isbn;
		}
		else if(title != null) {
			searchURL = GOOGLE_BOOKS_API_BASEURL + "?q=intitle:" + title;
			bookDetails = "book " + title.replaceAll("%20", " ");
		}
		else if(isbn != null) {
			searchURL = GOOGLE_BOOKS_API_BASEURL + "?q=isbn:" + isbn;
			bookDetails = "ISBN " + isbn;
		}
		if(startIndex == null) { startIndex = 0; }
		if(maxResults == null) { maxResults = 10; }
		searchURL += "&startIndex=" + startIndex + "&maxResults=" + maxResults;
		try {
			 
			URL url = new URL(searchURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));
			while ((output = br.readLine()) != null) {
				res += output;
			}
			jsonTest = mapper.readValue(res, Object.class);
			conn.disconnect();
		}catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		jsonReviews.put("restricted", "false");
		jsonReviews.putAll((Map)jsonTest);
		createLogs("GET", bookDetails, apiKey);
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonReviews);
	}
	
	public String getBookDetails(String volumeID) throws JsonProcessingException {
		String queryURL = GOOGLE_BOOKS_API_BASEURL + "/" + volumeID;
		String output, res="";
		ObjectMapper mapper = new ObjectMapper();
		Object jsonTest = null;
		try {
			URL url = new URL(queryURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));
			while ((output = br.readLine()) != null) {
				res += output;
			}
			jsonTest = mapper.readValue(res, Object.class);
			conn.disconnect();
		}catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		Map<String, Object> jsonReviews = new LinkedHashMap<String, Object>();
		jsonReviews.putAll((LinkedHashMap)jsonTest);
		Map<String, Object> rev = getReviewsAndRatings(volumeID);
		jsonReviews.put("totalReviews", rev.get("totalReviews"));
		jsonReviews.put("averageRating", rev.get("averageRating"));
		jsonReviews.put("reviews", rev.get("reviews"));
		jsonReviews.put("ratings", rev.get("ratings"));
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonReviews);
	}
	
	public Map<String, Object> getBookDetailsInMap(String volumeID) throws JsonProcessingException {
		String queryURL = GOOGLE_BOOKS_API_BASEURL + "/" + volumeID;
		String output, res="";
		ObjectMapper mapper = new ObjectMapper();
		Object jsonTest = null;
		try {
			URL url = new URL(queryURL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));
			while ((output = br.readLine()) != null) {
				res += output;
			}
			jsonTest = mapper.readValue(res, Object.class);
			conn.disconnect();
		}catch (MalformedURLException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		Map<String, Object> jsonReviews = new LinkedHashMap<String, Object>();
		jsonReviews.putAll((LinkedHashMap)jsonTest);
		Map<String, Object> rev = getReviewsAndRatings(volumeID);
		jsonReviews.put("totalReviews", rev.get("totalReviews"));
		jsonReviews.put("averageRating", rev.get("averageRating"));
		jsonReviews.put("reviews", rev.get("reviews"));
		jsonReviews.put("ratings", rev.get("ratings"));
		return jsonReviews;
	}
	
	public String getMD5hash(String input) {
		try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            String hashtext = number.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
	}
	
	public String registerUser(String fname, String lname, String email, String password, String userType, String isRestrictedFlag, String dateRestricted) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		DBObject user = collection.findOne(searchQuery);
		Map<String, Object> userDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		if(user != null) {
			userDetails.put("email", "invalid");
			userDetails.put("registered", "false");
		}
		else {
			BasicDBObject document = new BasicDBObject();
			document.put("fname", fname);
			document.put("lname", lname);
			document.put("email", email);
			document.put("password", getMD5hash(password));
			document.put("apiKey", getMD5hash(email+password+(new Date())));
			document.put("userType", userType);
			document.put("isRestrictedFlag", isRestrictedFlag);
			document.put("dateRestricted", dateRestricted);
			collection.insert(document);
			userDetails.put("email", "valid");
			userDetails.put("registered", "true");
		}
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDetails);
	}
	
	public String authenticateUser(String email, String password) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		searchQuery.put("password", getMD5hash(password));
		DBObject user = collection.findOne(searchQuery);		
		Map<String, Object> userDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		if(user == null)
			userDetails.put("validUser", "false");
		else {
			Map<String, Object> userDetailsMap = new HashMap<String, Object>();
			userDetailsMap.put("fname", user.get("fname"));
			userDetailsMap.put("lname", user.get("lname"));
			userDetailsMap.put("apiKey", user.get("apiKey"));
			userDetailsMap.put("userType", user.get("userType"));
			userDetailsMap.put("isRestricted", user.get("isRestrictedFlag"));
			userDetails.put("validUser", "true");
			userDetails.put("userDetails", userDetailsMap);
		}
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDetails);
	}
	
	public String getProfile(String email) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		DBObject user = collection.findOne(searchQuery);
		Map<String, Object> userDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		if(user == null)
			userDetails.put("validUser", "false");
		else {
			userDetails.put("fname", user.get("fname"));
			userDetails.put("lname", user.get("lname"));
			userDetails.put("email", user.get("email"));
			userDetails.put("userType", user.get("userType"));
			userDetails.put("apiKey", user.get("apiKey"));
		}
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDetails);
	}
	
	public String updateUserDetails(String fname, String lname, String email) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject newDocument = new BasicDBObject();
		BasicDBObject newDocumentDetails = new BasicDBObject();
		newDocumentDetails.append("fname", fname);
		newDocumentDetails.append("lname", lname);
		newDocument.append("$set", newDocumentDetails);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		collection.update(searchQuery, newDocument);
		Map<String, Object> userDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		userDetails.put("success", "true");
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDetails);
	}
	
	public String changePassword(String email, String currentPassword, String newPassword) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject newDocument = new BasicDBObject();
		BasicDBObject newDocumentDetails = new BasicDBObject();
		newDocumentDetails.append("password", getMD5hash(newPassword));
		newDocument.append("$set", newDocumentDetails);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		collection.update(searchQuery, newDocument);
		Map<String, Object> userDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		userDetails.put("success", "true");
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDetails);
	}
	
	public String updateUserType(String email, String apiKey, String userType) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject newDocument = new BasicDBObject();
		BasicDBObject newDocumentDetails = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		if(email != null)
			searchQuery.put("email", email);
		else if(apiKey != null)
			searchQuery.put("apiKey", apiKey);
		newDocumentDetails.put("userType", userType);
		newDocument.append("$set", newDocumentDetails);
		collection.update(searchQuery, newDocument);
		Map<String, Object> userDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		userDetails.put("success", "true");
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDetails);
	}
	
	public String restrictUser(String email, String apiKey) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject newDocument = new BasicDBObject();
		BasicDBObject newDocumentDetails = new BasicDBObject();
		BasicDBObject searchQuery = new BasicDBObject();
		if(email != null)
			searchQuery.append("email", email);
		else if(apiKey != null)
			searchQuery.append("apiKey", apiKey);
		newDocumentDetails.put("isRestrictedFlag", "y");
		DateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy");
		Date date = new Date();
		newDocumentDetails.put("dateRestricted", dateFormat.format(date));
		newDocument.append("$set", newDocumentDetails);
		collection.update(searchQuery, newDocument);
		Map<String, Object> userDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		userDetails.put("success", "true");
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDetails);
	}
	
	public String getFavouritesAndToRead(String email, String type)throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(FAVOURITES_READTO_COLLECTION_NAME);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		searchQuery.put("type", type);
		DBCursor fav = collection.find(searchQuery);
		Map<String, Object> favourites = new HashMap<String, Object>();
		favourites.put("totalItems", fav.count());
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		while(fav.hasNext()) {
			BasicDBObject obj = (BasicDBObject)fav.next();
			Map<String, Object> favItems = new HashMap<String, Object>();
			favItems.put("bookId", obj.get("bookId"));
			favItems.put("bookName", obj.get("bookName"));
			favItems.put("authors", obj.get("author"));
			favItems.put("publisher", obj.get("publisher"));
			favItems.put("publishedDate", obj.get("publishedDate"));
			favItems.put("infoLink", obj.get("infoLink"));
			favItems.put("imageLink", obj.get("imageLink"));
			list.add(favItems);
		}
		favourites.put("items", list);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(favourites);
	}
	
	public String addToFavouritesAndToRead(String email, String bookID, String type) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(FAVOURITES_READTO_COLLECTION_NAME);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		searchQuery.put("bookId", bookID);
		searchQuery.put("type", type);
		DBObject doc = collection.findOne(searchQuery);
		Map<String, Object> favDetails = new HashMap<String, Object>();
		ObjectMapper mapper = new ObjectMapper();
		if(doc == null) {
			String queryURL = GOOGLE_BOOKS_API_BASEURL + "/" + bookID;
			String output, res="";
			Object obj = null;
			try {
				URL url = new URL(queryURL);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Accept", "application/json");
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ conn.getResponseCode());
				}
				BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));
				while ((output = br.readLine()) != null) {
					res += output;
				}
				JSONParser parser = new JSONParser();
				try {
					obj = parser.parse(res);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				conn.disconnect();
			}catch (MalformedURLException e) {
				e.printStackTrace();
			}catch (IOException e) {
				e.printStackTrace();
			}
			
			JSONObject array = (JSONObject)obj;
			String id = (String)array.get("id");
			JSONObject volumeInfo = (JSONObject)array.get("volumeInfo");
			JSONObject imageLinks = (JSONObject)volumeInfo.get("imageLinks");
			
			BasicDBObject document = new BasicDBObject();
			document.put("email", email);
			document.put("bookId", id);
			document.put("bookName", volumeInfo.get("title"));
			document.put("author", volumeInfo.get("authors"));
			document.put("publisher", volumeInfo.get("publisher"));
			document.put("publishedDate", volumeInfo.get("publishedDate"));
			document.put("infoLink", volumeInfo.get("infoLink"));
			document.put("imageLink", imageLinks.get("thumbnail"));
			document.put("type", type);
			collection.insert(document);
			favDetails.put("success", "true");
		}
		else
			favDetails.put("success", "false");
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(favDetails);
	}
	
	public String removeFromFavouritesAndToRead(String email, String bookId, String type) throws JsonProcessingException {
		DBCollection collection = DBProperties.getDb().getCollection(FAVOURITES_READTO_COLLECTION_NAME);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("email", email);
		searchQuery.put("bookId", bookId);
		searchQuery.put("type", type);
		collection.remove(searchQuery);
		Map<String, Object> favDetails = new HashMap<String, Object>();
		favDetails.put("success", "true");
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(favDetails);
	}
	
	public String addReviewForBook(String bookId, String isbn, String email, String review, String rating, String apiKey) throws JsonProcessingException {
		DBCollection reviewCollection = DBProperties.getDb().getCollection(REVIEWS_COLLECTION_NAME);
		DBCollection ratingCollection = DBProperties.getDb().getCollection(RATING_COLLECTION_NAME);
		if(review != null && !review.equals("undefined")) {
			BasicDBObject newReview = new BasicDBObject();
			newReview.put("bookId", bookId);
			newReview.put("userEmail", email);
			newReview.put("review", review);
			DateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy 'at' HH:mm");
			Date date = new Date();
			newReview.put("date", dateFormat.format(date));
			reviewCollection.insert(newReview);
		}
		BasicDBObject newRating = new BasicDBObject();
		newRating.put("bookId", bookId);
		newRating.put("userEmail", email);
		DBObject doc = ratingCollection.findOne(newRating);
		if(doc == null) {
			newRating.put("rating", rating);
			ratingCollection.insert(newRating);
		}
		else {
			BasicDBObject updateRating = new BasicDBObject();
			BasicDBObject updateDocument = new BasicDBObject();
			updateRating.put("rating", rating);
			updateDocument.put("$set", updateRating);
			ratingCollection.update(newRating, updateDocument);
		}
		
		Map<String, Object> reviewDetails = new HashMap<String, Object>();
		reviewDetails.put("success", "true");
		reviewDetails.putAll(getBookDetailsInMap(bookId));
		ObjectMapper mapper = new ObjectMapper();
		createLogs("POST", "ISBN " + isbn, apiKey);
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(reviewDetails);
	}
	
	public Map<String, Object> getReviewsAndRatings(String volumeID) {
		DBCollection reviewCollection = DBProperties.getDb().getCollection(REVIEWS_COLLECTION_NAME);
		DBCollection ratingCollection = DBProperties.getDb().getCollection(RATING_COLLECTION_NAME);
		BasicDBObject searchQuery = new BasicDBObject();
		searchQuery.put("bookId", volumeID);
		DBCursor reviewsCursor = reviewCollection.find(searchQuery).sort(new BasicDBObject("_id", 1));
		DBCursor ratingCursor = ratingCollection.find(searchQuery);
		List<Map<String, Object>> reviewsList = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> ratingList = new ArrayList<Map<String, Object>>();
		Double averageRating = 0.0;
		while(reviewsCursor.hasNext()) {
			Map<String, Object> reviewItems = new HashMap<String, Object>();
			BasicDBObject dbObj = (BasicDBObject)reviewsCursor.next();
			reviewItems.put("user", dbObj.get("userEmail"));
			reviewItems.put("review", dbObj.get("review"));
			reviewItems.put("date", dbObj.get("date"));
			reviewsList.add(reviewItems);
		}
		while(ratingCursor.hasNext()) {
			Map<String, Object> ratingItem = new HashMap<String, Object>();
			BasicDBObject dbObj = (BasicDBObject)ratingCursor.next();
			ratingItem.put("user", dbObj.get("userEmail"));
			ratingItem.put("rating", dbObj.get("rating"));
			averageRating += Double.parseDouble(dbObj.getString(("rating")));
			ratingList.add(ratingItem);
		}
		averageRating = (double) Math.round(averageRating/ratingCursor.count()*10)/10.0;
		Map<String, Object> jsonReviews = new LinkedHashMap<String, Object>();
		jsonReviews.put("totalReviews", reviewsCursor.count());
		jsonReviews.put("averageRating", averageRating);
		jsonReviews.put("reviews", reviewsList);
		jsonReviews.put("ratings", ratingList);
		return jsonReviews;
	}
	
	public void createLogs(String requestType, String bookDetails, String apiKey) {
		DBCollection logsCollection = DBProperties.getDb().getCollection(LOGS_COLLECTION_NAME);
		BasicDBObject log = new BasicDBObject();
		String operation = requestType + " request for " + bookDetails;
		DateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy");
		Date date = new Date();
		log.put("operation", operation);
		log.put("date", dateFormat.format(date));
		log.put("user", getUserFromAPIKey(apiKey));
		log.put("userType", getUserType(null, apiKey));
		log.put("apiKey", apiKey);
		logsCollection.insert(log);
	}
	
	public String getUserFromAPIKey(String apiKey) {
		DBCollection profileCollection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject user = new BasicDBObject();
		user.put("apiKey", apiKey);
		DBObject result = profileCollection.findOne(user);
		return result.get("email").toString();
	}
	
	public String getUserType(String email, String apiKey) {
		DBCollection profileCollection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject user = new BasicDBObject();
		if(email != null)
			user.put("email", email);
		else if(apiKey != null)
			user.put("apiKey", apiKey);
		DBObject result = profileCollection.findOne(user);
		if(result != null)
			return result.get("userType").toString();
		return "";
	}
	
	public String getUserStatus(String email, String apiKey) {
		DBCollection profileCollection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject user = new BasicDBObject();
		if(email != null)
			user.put("email", email);
		else if(apiKey != null)
			user.put("apiKey", apiKey);
		DBObject result = profileCollection.findOne(user);
		if(result != null)
			return result.get("isRestrictedFlag").toString();
		return "";
	}
	
	public String getLogs(String email, String apiKey) throws JsonProcessingException {
		DBCollection logsCollection = DBProperties.getDb().getCollection(LOGS_COLLECTION_NAME);
		BasicDBObject log = new BasicDBObject();
		DateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy");
		Date date = new Date();
		log.put("date", dateFormat.format(date));
		DBCursor logs;
		if(email != null && apiKey != null && !email.equals("undefined") && !apiKey.equals("undefined") && !email.equals("") && !apiKey.equals("")) {
			log.put("user", email);
			log.put("apiKey", apiKey);
		}
		else if (email != null && !email.equals("undefined") && !email.equals("")) {
			log.put("user", email);
		}
		else if(apiKey != null && !apiKey.equals("undefined") && !apiKey.equals("")) {
			log.put("apiKey", apiKey);
		}
		logs = logsCollection.find(log);
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> logItems = new HashMap<String, Object>();
		logItems.put("totalItems", logs.count());
		date = new Date();
		logItems.put("logDate", dateFormat.format(date));
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		while(logs.hasNext()) {
			Map<String, Object> items = new HashMap<String, Object>();
			BasicDBObject dbObj = (BasicDBObject)logs.next();
			items.put("operation", dbObj.get("operation"));
			items.put("date", dbObj.get("date"));
			items.put("user", dbObj.get("user"));
			items.put("userType", getUserType(email, apiKey));
			items.put("restricted", getUserStatus(email, apiKey));
			items.put("apiKey", dbObj.get("apiKey"));
			list.add(items);
		}
		logItems.put("items", list);
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(logItems);
	}
	
	public Map<String, Object> getUserDetailsFromAPIKey(String apiKey) {
		DBCollection profileCollection = DBProperties.getDb().getCollection(USER_PROFILE_COLLECTION_NAME);
		BasicDBObject user = new BasicDBObject();
		user.put("apiKey", apiKey);
		DBObject result = profileCollection.findOne(user);
		Map<String, Object> userDetails = new HashMap<String, Object>();
		userDetails.put("fname", result.get("fname"));
		userDetails.put("lname", result.get("lname"));
		userDetails.put("email", result.get("email"));
		userDetails.put("userType", result.get("userType"));
		userDetails.put("isRestrictedFlag", result.get("isRestrictedFlag"));
		userDetails.put("dateRestricted", result.get("dateRestricted"));
		return userDetails;
	}
	
	public int getAPIUsageForUserByAPIKey(String apiKey) {
		DBCollection logsCollection = DBProperties.getDb().getCollection(LOGS_COLLECTION_NAME);
		BasicDBObject user = new BasicDBObject();
		user.put("apiKey", apiKey);
		DBCursor logs = logsCollection.find(user);
		return logs.count();
		//return API_USAGE_LIMIT;
	}
}