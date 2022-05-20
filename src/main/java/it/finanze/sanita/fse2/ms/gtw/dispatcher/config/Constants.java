package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

/**
 * 
 * @author vincenzoingenito
 *
 * Constants application.
 */
public final class Constants {

	/**
	 *	Path scan.
	 */
	public static final class ComponentScan {

		/**
		 * Base path.
		 */
		public static final String BASE = "it.sanita.dispatcher";

		/**
		 * Controller path.
		 */
		public static final String CONTROLLER = "it.sanita.dispatcher.controller";

		/**
		 * Service path.
		 */
		public static final String SERVICE = "it.sanita.dispatcher.service";

		/**
		 * Configuration path.
		 */
		public static final String CONFIG = "it.sanita.dispatcher.config";
		
		/**
		 * Configuration mongo path.
		 */
		public static final String CONFIG_MONGO = "it.sanita.dispatcher.config.mongo";
		
		/**
		 * Configuration mongo repository path.
		 */
		public static final String REPOSITORY_MONGO = "it.sanita.dispatcher.repository";
		 
		
		private ComponentScan() {
			//This method is intentionally left blank.
		}

	}
 
	public static final class Profile {
		public static final String TEST = "TEST";

		/** 
		 * Constructor.
		 */
		private Profile() {
			//This method is intentionally left blank.
		}

	}
  
	/**
	 *	Constants.
	 */
	private Constants() {

	}

}
