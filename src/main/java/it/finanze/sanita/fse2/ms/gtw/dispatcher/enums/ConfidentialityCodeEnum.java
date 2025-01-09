package it.finanze.sanita.fse2.ms.gtw.dispatcher.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Accepted document types defined by the affinity domain: {@link http://www.hl7italia.it/hl7italia_D7/node/2359}.
 * 
 */
@Getter
@AllArgsConstructor
public enum ConfidentialityCodeEnum {

    NORMAL("N","Normal"),
    RESTRICTED("R", "Restricted"),
    VERY_RESTRICTED("V","Very Restricted");

    private final String code;

    private final String display;

   
    
    public static String getDisplayByCode(String code) {
        for (ConfidentialityCodeEnum el : ConfidentialityCodeEnum.values()) {
            if (el.getCode().equals(code)) {
                return el.getDisplay();
            }
        }
        return "";
    }

}