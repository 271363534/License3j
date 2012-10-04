package com.verhas.licensor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

/**
 * ExtendedLicense supports not only String features, but also Integer, Date and
 * URL features. It is also able to check that the license is not expired and
 * can check the revocation state of the license online.
 * 
 * @author Peter Verhas
 * 
 */
public class ExtendedLicense extends License {
	final private static String EXPIRATION_DATE = "expiryDate";
	final private static String DATE_FORMAT = "yyyy-MM-dd";
	final private static String LICENSE_ID = "licenseId";
	final private static String REVOCATION_URL = "revocationUrl";

	/**
	 * Checks the expiration date of the license and returns true if the license
	 * has expired.
	 * <p>
	 * The expiration date is encoded in the license with the key
	 * {@code expiryDate} in the format {@code yyyy-MM-dd}. A license is expired
	 * if the current date is after the specified expiryDate.
	 * <p>
	 * Note that this method does not ensure license validity. You separately
	 * have to call {@see License#isVerified()} to ensure that the license was
	 * successfully verified.
	 * <p>
	 * The time is calculated using the default locale, thus licenses expire
	 * first in Australia, later in Europe and latest in USA.
	 * 
	 * @return {@code true} if the license is expired
	 * 
	 * @throws ParseException
	 */
	public boolean isExpired() throws ParseException {
		boolean expired = true;
		Date expiryDate;
		try {
			expiryDate = getFeature(EXPIRATION_DATE, Date.class);
			GregorianCalendar today = new GregorianCalendar();
			today.set(Calendar.HOUR, 0);
			today.set(Calendar.MINUTE, 0);
			today.set(Calendar.SECOND, 0);
			today.set(Calendar.MILLISECOND, 0);
			expired = today.getTime().after(expiryDate);
		} catch (Exception e) {
			expired = true;
		}
		return expired;
	}

	/**
	 * Set the expiration date of the license. Since the date is stored in the
	 * format {@code yyyy-MM-dd} the actual hours, minutes and so on will be
	 * chopped off.
	 * 
	 * @param expiryDate
	 */
	public void setExpiry(Date expiryDate) {
		setFeature(EXPIRATION_DATE,expiryDate);
	}

	/**
	 * Generates a new license id.
	 * <p>
	 * Note that this ID is also stored in the license thus there is no need to
	 * call {@see #setFeature(String, UUID)} separately after the UUID was
	 * generated.
	 * <p>
	 * Generating UUID can be handy when you want to identify each license
	 * individually. For example you want to store revocation information about
	 * each license. The url to check the revocation may contain the
	 * <tt>${licenseId}</tt> place holder that will be replaced by the actual
	 * uuid stored in the license.
	 * 
	 * @return the generated uuid.
	 */
	public UUID generateLicenseId() {
		UUID uuid = UUID.randomUUID();
		setLicenseId(uuid);
		return uuid;
	}

	/**
	 * Set the UUID of a license. Note that this UUID can be generated calling
	 * the method {@link #generateLicenseId()}, which method automatically calls
	 * this method setting the generated UUID to be the UUID of the license.
	 * 
	 * @param licenseId
	 *            the uuid that was generated somewhere, presumably not by
	 *            {@link #generateLicenseId()} because the uuid generated by
	 *            that method is automatically stored in the license.
	 */
	public void setLicenseId(UUID licenseId) {
		setFeature(LICENSE_ID, licenseId);
	}

	public UUID getLicenseId() {
		UUID licenseId = null;
		try {
			licenseId = getFeature(LICENSE_ID, UUID.class);
		} catch (Exception e) {
			licenseId = null;
		}
		return licenseId;
	}

	/**
	 * Set an integer feature in the license.
	 * 
	 * @param name
	 *            the name of the feature
	 * @param i
	 *            the value of the integer feature
	 */
	public void setFeature(String name, Integer i) {
		setFeature(name, i.toString());
	}

	/**
	 * Set a date feature in the license.
	 * 
	 * @param name
	 *            the name of the feature
	 * @param date
	 *            the date to be stored for the feature name in the license
	 */
	public void setFeature(String name, Date date) {
		DateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
		setFeature(name, formatter.format(date));
	}

	/**
	 * Set a URL feature in the license.
	 * 
	 * @param name
	 *            the name of the feature
	 * @param url
	 *            the url to store in the license
	 */
	public void setFeature(String name, URL url) {
		setFeature(name, url.toString());
	}

	/**
	 * Set an UUID feature in the license.
	 * 
	 * @param name
	 *            the name of the feature
	 * @param uuid
	 *            the uuid to store in the license
	 */
	public void setFeature(String name, UUID uuid) {
		setFeature(name, uuid.toString());
	}

	@SuppressWarnings("unchecked")
	public <T> T getFeature(String name, Class<? extends T> klass)
			throws InstantiationException, IllegalAccessException,
			ParseException, MalformedURLException {
		T result = null;
		String resultString = getFeature(name);
		if (Integer.class == klass ) {
			result = (T) (Integer) Integer.parseInt(resultString);
		} else if (Date.class == klass) {
			result = (T) new SimpleDateFormat(DATE_FORMAT)
					.parse(getFeature(name));
		} else if (UUID.class == klass) {
			result = (T) UUID.fromString(getFeature(name));
		} else if (URL.class == klass) {
			result = (T) new URL(getFeature(name));
		} else {
			throw new IllegalArgumentException("'" + klass.toString()
					+ "' is not handled");
		}
		return result;
	}

	/**
	 * Get the revocation URL of the license. This feature is stored in the
	 * license under the name {@code revocationUrl}. This URL may contain the
	 * string <code>${licenseId}</code> which is replaced by the actual license
	 * ID. Thus there is no need to wire into the revocation URL the license ID.
	 * <p>
	 * If there is no license id defined in the license then the place holder
	 * will not be replaced.
	 * 
	 * @return the revocation URL with the license id place holder filled in.
	 * @throws MalformedURLException
	 */
	public URL getRevocationURL() throws MalformedURLException {
		URL revocationURL = null;
		String revocationURLString = getFeature(REVOCATION_URL);
		if (revocationURLString != null) {
			UUID licenseId = getLicenseId();
			if (licenseId != null) {
				revocationURLString = revocationURLString.replaceAll(
						"\\$\\{licenseId\\}", licenseId.toString());
			}
			revocationURL = new URL(revocationURLString);
		}
		return revocationURL;
	}

	/**
	 * Set the revocation URL. This method accepts the url as a string that
	 * makes it possible to use a string that contains the
	 * <code>${licenseId}</code> place holder.
	 * 
	 * @param url
	 *            the url from where the revocation information can be
	 *            downloaded
	 */
	public void setRevocationURL(String url) {
		setFeature(REVOCATION_URL, url);
	}

	/**
	 * Set the revocation URL. Using this method is discouraged in case the URL
	 * contains the <code>${licenseId}</code> place holder. In that case it is
	 * recommended to use the {@link #setRevocationURL(String)} method instead.
	 * 
	 * @param url
	 *            the revocation url
	 */
	public void setRevocationURL(URL url) {
		setRevocationURL(url.toString());
	}

	/**
	 * Check if the license was revoked or not. For more information see the
	 * documentation of the method {@link #isRevoked(boolean)}. Calling this
	 * method is equivalent to calling {@code isRevoked(false)}, meaning that
	 * the license is signaled to be revoked if the revocation URL can not be
	 * reached.
	 * 
	 * @return {@code true} if the license was revoked and {@code false} if the
	 *         license was not revoked. It also returns {@code true} if the
	 *         revocation url is unreachable.
	 * 
	 */
	public boolean isRevoked() {
		return isRevoked(false);
	}

	/**
	 * Check if the license is revoked or not. To get the revocation information
	 * the method tries to issue a http connection (GET) to the url specified in
	 * the license feature {@code revocationUrl}. If the URL returns anything
	 * with http status code {@code 200} then the license is not revoked.
	 * <p>
	 * The url string in the feature {@code revocationUrl} may contain the place
	 * holder <code>${licenseId}</code>, which is replaced by the feature value
	 * {@code licenseId}. This feature makes it possible to setup a revocation
	 * service and use a constant string in the different licenses.
	 * <p>
	 * The method can work in two different ways. One way is to ensure that the
	 * license is not revoked and return {@code true} only if it is sure that
	 * the license is revoked or revocation information is not available.
	 * <p>
	 * The other way is to ensure that the license is revoked and return
	 * {@code false} if the license was not revoked or the revocation
	 * information is not available.
	 * <p>
	 * The difference is whether to treat the license revoked when the
	 * revocation service is not reachable.
	 * 
	 * @param forceOnline
	 *            should be {@code true} to treat the license revoked when the
	 *            revocation service is not reachable. In this case the program
	 *            using the license manager will treat the license revoked if
	 *            the revocation service is not reachable. Setting this argument
	 *            {@code false} makes the revocation handling more polite: if
	 *            the license revocation service is not reachable then the
	 *            license is treated as not revoked.
	 * @return {@code true} if the license is revoked and {@code false} is the
	 *         license is not revoked.
	 */
	public boolean isRevoked(boolean forceOnline) {
		boolean revoked = true;
		try {
			URL url = getRevocationURL();
			URLConnection connection = url.openConnection();
			connection.setUseCaches(false);
			if (connection instanceof HttpURLConnection) {
				connection.connect();
				HttpURLConnection httpUrlConnection = (HttpURLConnection) connection;
				int responseCode = httpUrlConnection.getResponseCode();
				if (responseCode == 200) {
					revoked = false;
				}
			}
		} catch (IOException exception) {
			revoked = forceOnline;
		}
		return revoked;
	}

}
