
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.utility;

import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.Constants;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.PriorityDocumentCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.DestinationTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.PriorityTypeEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.enums.TipoDocAltoLivEnum;
import it.finanze.sanita.fse2.ms.gtw.dispatcher.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PriorityUtility {

    @Autowired
    private KafkaTopicCFG kafkaTopicCFG;

    @Autowired
    private PriorityDocumentCFG priorityDocumentCFG;

    public String computeTopic(PriorityTypeEnum priorityType, DestinationTypeEnum destinationType, TipoDocAltoLivEnum documentType) {
        String destTopic = "";

        switch (destinationType) {
            case INDEXER:
                destTopic = kafkaTopicCFG.getDispatcherIndexerTopic();
                break;
            case PUBLISHER:
                destTopic = kafkaTopicCFG.getDispatcherPublisherTopic();
                break;
            default:
                throw new BusinessException("Non e' stata specificata una destinazione valida");
        }

        switch (priorityType) {
            case NULL:
                destTopic += this.computePriorityPerDocumentType(documentType);
                break;
            case LOW:
                destTopic += Constants.Misc.LOW_PRIORITY;
                break;
            case HIGH:
                destTopic += Constants.Misc.HIGH_PRIORITY;
                break;
            default:
                throw new BusinessException("Error in computing topic priority for notification");
        }

        return destTopic;
    }

    /**
     * Compute priority per document type
     * @param documentType
     * @return
     */
    public String computePriorityPerDocumentType(TipoDocAltoLivEnum documentType) {
        if (priorityDocumentCFG.getLowPriorityDocuments().contains(documentType.getCode())) {
            log.debug("Low priority document: {}", documentType.getCode());
            return Constants.Misc.LOW_PRIORITY;
        } else if (priorityDocumentCFG.getMediumPriorityDocuments().contains(documentType.getCode())) {
            log.debug("Medium priority document: {}", documentType.getCode());
            return Constants.Misc.MEDIUM_PRIORITY;
        } else {
            log.debug("High priority document: {}", documentType.getCode());
            return Constants.Misc.HIGH_PRIORITY;
        }
    }
}
