package it.finanze.sanita.fse2.ms.gtw.dispatcher.client;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.dto.ValidationInfoDTO;

/**
 * Interface of Validator Client.
 * 
 * @author CPIERASC
 */
public interface IValidatorClient {

	ValidationInfoDTO validate(String cda);
}
