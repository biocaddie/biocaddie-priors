package org.nationaldataservice.elasticsearch.priors;


public class QueryDocPriorException extends Exception {
	/**
	 * Unique id to identify this {@link Exception}
	 */
	private static final long serialVersionUID = 5961496592606387768L;

	/**
	 * An {@link Exception} encountered during {@link QueryDocPriorSearchRestAction} operations
	 */
	public QueryDocPriorException() {
		
	}

	/**
	 * An {@link Exception} encountered during {@link QueryDocPriorSearchRestAction} operations
	 * 
	 * @param message {@link String} the error message
	 */
	public QueryDocPriorException(String message) {
		super(message);
	}

	/**
	 * An exception encountered during {@link QueryDocPriorSearchRestAction} operations
	 * 
	 * @param cause the {@link Throwable} cause
	 */
	public QueryDocPriorException(Throwable cause) {
		super(cause);
	}

	/**
	 * An exception encountered during {@link QueryDocPriorSearchRestAction} operations
	 * 
	 * @param message {@link String} the error message
	 * @param cause the {@link Throwable} underlying cause
	 */
	public QueryDocPriorException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * An exception encountered during {@link QueryDocPriorSearchRestAction} operations
	 *
	 * @param message {@link String} the error message
	 * @param cause the {@link Throwable} underlying cause
	 * @param enableSuppression a {@link boolean} indicating whether suppression is enabled
	 * @param writableStackTrace a {@link boolean} indicating whether the stackTrace is writeable
	 */
	public QueryDocPriorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
