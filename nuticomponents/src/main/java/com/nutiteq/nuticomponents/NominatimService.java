package com.nutiteq.nuticomponents;

import android.support.v4.util.ArrayMap;

import com.carto.core.MapBounds;
import com.carto.core.MapPos;
import com.flurry.android.FlurryAgent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class NominatimService {

	private String query;

	private double x;
	private double y;

	private HttpURLConnection conn;
	private BufferedReader reader;

	public ArrayList<GeocodeResult> geocode(String query)
			throws InterruptedException, ExecutionException {
		this.query = query;

		ExecutorService exService = Executors.newSingleThreadExecutor();
		FutureTask<ArrayList<GeocodeResult>> futureTask = new FutureTask<ArrayList<GeocodeResult>>(
				new GeocodeTask());

		exService.execute(futureTask);

		return futureTask.get();
	}

	public String reverseGeocode(double x, double y)
			throws InterruptedException, ExecutionException {
		this.x = x;
		this.y = y;

		ExecutorService exService = Executors.newSingleThreadExecutor();
		FutureTask<String> futureTask = new FutureTask<String>(
				new ReverseGeocodeTask());

		exService.execute(futureTask);

		return futureTask.get();
	}

	class GeocodeTask implements Callable<ArrayList<GeocodeResult>> {

		public ArrayList<GeocodeResult> call() {
			ArrayList<GeocodeResult> geocodeResults = new ArrayList<GeocodeResult>();

			try {
				String link = "http://api.nutiteq.com/search/search.php?user_key="
						+ Const.NUTITEQ_KEY
						+ "&format=json&q="
						+ URLEncoder.encode(query, "utf-8").replaceAll("\\+", "%20");

				URL url = new URL(link);

				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
				conn.setDoInput(true);

				conn.connect();

				reader = new BufferedReader(new InputStreamReader(
						conn.getInputStream(), "UTF-8"));
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}

				String result = sb.toString();

				JSONArray jArray = new JSONArray(result);

				// register query and response count with Flurry
				Map<String, String> parameters = new ArrayMap<String, String>();
				parameters.put("query", query);
				parameters.put("response count",
						String.valueOf(jArray.length()));
				FlurryAgent.logEvent("SEARCH_RESULT", parameters);

				int l = jArray.length();

				String line1;
				String line2;
				String s;

				double lon;
				double lat;

				double x1;
				double y1;
				double x2;
				double y2;

				int k;

				for (int i = 0; i < l; i++) {
					try {
						JSONObject o = jArray.getJSONObject(i);

						line1 = "";
						line2 = "";
						s = "";

						lon = Double.parseDouble(o.getString("lon").trim());
						lat = Double.parseDouble(o.getString("lat").trim());

						y1 = o.getJSONArray("boundingbox").getDouble(0);
						y2 = o.getJSONArray("boundingbox").getDouble(1);
						x1 = o.getJSONArray("boundingbox").getDouble(2);
						x2 = o.getJSONArray("boundingbox").getDouble(3);

						s = o.getString("display_name").trim();
						k = s.indexOf(",");

						if (k != -1) {
							line1 += s.substring(0, k).trim();

							try {
								Integer.parseInt(line1);

								s = s.substring(k + 1).trim();
								k = s.indexOf(",");

								if (k != -1) {
									line1 += " " + s.substring(0, k).trim();
								} else {
									line1 += s;
								}

							} catch (Exception e) {
							}

							line2 += s.substring(k + 1).trim();
						} else {
							line1 = s;
						}

						geocodeResults.add(new GeocodeResult(line1, line2, lon,
								lat, new MapBounds(new MapPos(x1, y1),
										new MapPos(x2, y2))));
					} catch (Exception e) {
						geocodeResults = null;
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				geocodeResults = null;
			} catch (JSONException e) {
				e.printStackTrace();
				geocodeResults = null;
			} finally {
				if (conn != null) {
					disconect();
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

			return geocodeResults;
		}
	}

	class ReverseGeocodeTask implements Callable<String> {

		public String call() {
			String reverseResult = "";

			try {
				String link = "http://api.nutiteq.com/reverse?user_key="
						+ Const.NUTITEQ_KEY + "&format=json&lat=" + y
						+ "&lon=" + x;

				URL url = new URL(link);

				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
				conn.setDoInput(true);

				conn.connect();

				reader = new BufferedReader(new InputStreamReader(
						conn.getInputStream(), "UTF-8"));
				StringBuilder sb = new StringBuilder();

				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}

				String result = sb.toString();

				JSONObject j = new JSONObject(result);

				try {
					if (j.getString("display_name") != null) {
						reverseResult = j.getString("display_name");
					} else {
						reverseResult = null;
					}
				} catch (JSONException e) {
					reverseResult = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
				reverseResult = null;
			} catch (JSONException e) {
				e.printStackTrace();
				reverseResult = null;
			} finally {
				if (conn != null) {
					try {
						conn.disconnect();
					} catch (Exception e) {
					}
				}
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			return reverseResult;
		}
	}

	private void disconect() {
		if (conn != null) {
			try {
				conn.disconnect();
			} catch (Exception e) {
			}
		}
	}
}
