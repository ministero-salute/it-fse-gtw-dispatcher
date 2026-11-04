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

import lombok.AccessLevel;

/**
 * Constants application.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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
		 * Collection where audit info is saved
		 */
		public static final String AUDIT = "audit";
		
		/**
		 * Collection where engines info is saved
		 */
		public static final String ENGINES = "engines";
		
		/**
		 * Collection where routing table entries are saved
		 * Maps JWT ISSUER to notification endpoints
		 */
		public static final String ROUTING_TABLE = "routing_table";
		
		/**
		 * Collection where issuer configurations are saved
		 */
		public static final String ISSUERS = "issuers";

		/**
		 * Collection where structured logs are saved
		 */
		public static final String LOG_CONTROL_COLLECTION_NAME = "log_collector";

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

		@NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static class Custom {
            public static final String DATE_PATTERN = "dd-MM-yyyy HH:mm:ss.SSS";
        }
		
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

		public static final String MASTER_ID_SEPARATOR = "^";
		public static final String WIF_SUFFIX = "^^^^urn:ihe:iti:xdw:2013:workflowInstanceId";
		public static final String WIF_SEPARATOR = ".";

		public static final String JSON_EXTENSION = ".json";
		
		
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
			public static final String DELETE_PATH = "/v1/documents/{idDoc}/{fiscalCode}";
			public static final String UPDATE_PATH = "/v1/documents/{idDoc}/metadata";
			public static final String ID_DOC_PLACEHOLDER = "{idDoc}";
		}

		@NoArgsConstructor(access = PRIVATE)
		public static final class Config {
			public static final String MOCKED_GATEWAY_NAME = "mocked-gateway";
		}
	}
	
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	public static final class Properties {
		public static final String MS_NAME = "gtw-dispatcher";
		 
	}
	

	public static class Mongo {
        public static class Query {

            public static final String UNKNOWN_WORKFLOW_ID = "UNKNOWN_WORKFLOW_ID";
            public static final String REGION = "op_issuer.region";
        }

        public static class Fields {
            public static final String ID = "_id";

            private Fields() {}
            public static final String LOG_TYPE = "log_type";
            public static final String MESSAGE = "message";
            public static final String OPERATION = "operation";
            public static final String OP_RESULT = "op_result";
            public static final String OP_TIMESTAMP_START = "op_timestamp_start";
            public static final String OP_TIMESTAMP_END = "op_timestamp_end";
            public static final String OP_ERROR = "op_error";
            public static final String OP_ERROR_DESCRIPTION = "op_error_description";
            public static final String OP_ISSUER = "op_issuer";
            public static final String OP_LOCALITY = "op_locality";
            public static final String OP_DOCUMENT_TYPE = "op_document_type";
            public static final String MICROSERVICE_NAME = "microservice_name";
            public static final String OP_ROLE = "op_role";
            public static final String OP_FISCAL_CODE = "op_fiscal_code";
            public static final String GATEWAY_NAME = "gateway_name";
            public static final String OP_WARNING = "op_warning";
            public static final String OP_WARNING_DESCRIPTION = "op_warning_description";
            public static final String OP_SUBJ_APPLICATION = "op_subj_application";
            public static final String OP_SUBJ_APPLICATION_ID = "op_application_id";
            public static final String OP_SUBJ_APPLICATION_VENDOR = "op_application_vendor";
            public static final String OP_SUBJ_APPLICATION_VERSION = "op_application_version";
            public static final String WORKFLOW_INSTANCE_ID = "workflow_instance_id";
            public static final String TYPE_ID_EXTENSION = "typeIdExtension";
            public static final String ID_DOCUMENTO = "idDocumento";
            public static final String PROCESSED = "processed";
            public static final String ADMINISTRATIVE_REQUEST = "administrative_request";
            public static final String AUTHOR_INSTITUTION = "author_institution";
            public static final String IS_SSN = "is_ssn";
            public static final String REGISTRY = "registry";
            public static final String REGION = "region";
            public static final String COMPANY = "company";
            public static final String STRUCTURE = "structure";

            public static final String LOG_TYPE_KPI = "kpi-structured-log";
            public static final String LOG_TYPE_CONTROL = "control-structured-log";

        }
	}
   
}
