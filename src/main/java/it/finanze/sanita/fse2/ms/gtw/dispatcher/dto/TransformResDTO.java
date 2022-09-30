package it.finanze.sanita.fse2.ms.gtw.dispatcher.dto;

import javax.validation.constraints.Size;

import org.bson.Document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 
 * @author vincenzoingenito
 *
 *	DTO used to return document reference creation result.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TransformResDTO extends AbstractDTO {

	/**
	 * Serial version uid.
	 */
	private static final long serialVersionUID = -2618965716083072681L;
	
	@Size(min = 0, max = 1000)
	private String errorMessage;
	
	@Size(min = 0, max = 1000)
	private Document json;
	
}