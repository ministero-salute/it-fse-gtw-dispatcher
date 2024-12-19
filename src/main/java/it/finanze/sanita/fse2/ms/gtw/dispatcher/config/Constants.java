
/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Copyright (C) 2023 Ministero della Salute
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtw.dispatcher.config;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

/**
 * Constants application.
 */
public final class Constants {
 
	public static final class Collections {

		/** 
		 * INI EDS Invocation Collection 
		 */
		public static final String INI_EDS_INVOCATION = "ini_eds_invocation";

		/** 
		 * Collection where validated documents info is saved
		 */
		public static final String VALIDATED_DOCUMENTS = "validated_documents"; 
		
		/** 
		 * Collection where validated documents info is saved
		 */
		public static final String AUDIT = "audit";
		public static final String ENGINES = "engines";
		

		private Collections() {

		}
	}
	
	public static final class Headers {
		
		/**
		 * JWT header field.
		 */
		public static final String JWT_HEADER = "FSE-JWT-Signature";

		/**
		 * JWT header field of GovWay.
		 */
		public static final String JWT_GOVWAY_HEADER = "GovWay-ModI-Info";
		

		private Headers() {
			//This method is intentionally left blank.
		}

	}
 
	public static final class Profile {

		public static final String TEST = "test";
		
		public static final String DEV = "dev";
		
		public static final String DOCKER = "docker";

		public static final String TEST_PREFIX = "test_";

		/** 
		 * Constructor.
		 */
		private Profile() {
			//This method is intentionally left blank.
		}

	}

	public static final class App {
		
		public static final String JWT_TOKEN_TYPE = "JWT";

		public static final String BEARER_PREFIX = "Bearer ";
        public static final String SHA_ERROR = "Errore in fase di calcolo SHA-256";
		public static final String SHA_ALGORITHM = "SHA-256";
		public static final String SHA1_ALGORITHM = "SHA-1";
		public static final String JWT_MISSING_ISSUER_PLACEHOLDER = "UNDEFINED_JWT_ISSUER";
		public static final String JWT_MISSING_SUBJECT = "UNDEFINED_SUBJECT";


		public static final String MISSING_WORKFLOW_PLACEHOLDER = "UNKNOWN_WORKFLOW_ID";
		public static final String MISSING_DOC_TYPE_PLACEHOLDER = "UNKNOWN_DOCUMENT_TYPE";
		
		public static final String LOG_TYPE_CONTROL = "control-structured-log";
		public static final int MAX_SIZE_WARNING = 200000;
		public static final String BENCHMARK_ISSUER = "TEST-BENCHMARK";
		
		
		private App() {
			//This method is intentionally left blank.
		}
	}
	
	public static final class OIDS {

        public static final String OID_MEF = "2.16.840.1.113883.2.9.4.3.2";
        
        private OIDS() {
            //This method is intentionally left blank.
        }
    }

	public static final class Misc {
		/**
		 * Prefix for priority queues
		 */
		public static final String LOW_PRIORITY = "_LOW";
		public static final String MEDIUM_PRIORITY = "_MEDIUM";
		public static final String HIGH_PRIORITY = "_HIGH";
        public static final String WARN_EXTRACTION_SELECTION = "Attenzione, non è stata selezionata la modalità di estrazione del CDA";
        public static final String WARN_ASYNC_TRANSACTION = "Attenzione, transazione presa in carico. Nuovo tentativo in corso";
        
		/**
		 * Pattern of the date in the format required from INI.
		 */
		public static final String INI_DATE_PATTERN = "yyyyMMddHHmmss";
		
		/**
		 * Constructor.
		 */
		private Misc() {
			//This method is intentionally left blank.
		}

	}

	public static final class Client {

		private Client() {}

		@NoArgsConstructor(access = PRIVATE)
		public static final class Eds {
			public static final String DELETE_PATH = "/v1/documents/{idDoc}";
			public static final String UPDATE_PATH = "/v1/documents/{idDoc}/metadata";
			public static final String ID_DOC_PLACEHOLDER = "{idDoc}";
		}

		@NoArgsConstructor(access = PRIVATE)
		public static final class Config {
			public static final String MOCKED_GATEWAY_NAME = "mocked-gateway";
		}
	}
  
	/**
	 *	Constants.
	 */
	private Constants() {

	}

}
