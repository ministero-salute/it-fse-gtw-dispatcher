
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
package it.finanze.sanita.fse2.ms.gtw.dispatcher.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Accepted document types defined by the affinity domain: {@link http://www.hl7italia.it/hl7italia_D7/node/2359}.
 * 
 */
@Getter
@AllArgsConstructor
public enum DocumentTypeEnum {

    CODE_57833_6("57833-6","Prescrizione farmaceutica"),
    CODE_60591_5("60591-5","Profilo Sanitario Sintetico"),
    CODE_11502_2("11502-2","Referto di Laboratorio"),
    CODE_57829_4("57829-4","Prescrizione per prodotto o apparecchiature mediche"),
    CODE_34105_7("34105-7","Lettera di dimissione ospedaliera"),
    CODE_18842_5("18842-5","Lettera di dimissione non ospedaliera"),
    CODE_59258_4("59258-4","Verbale di pronto soccorso"),
    CODE_68604_8("68604-8","Referto di radiologia"),
    CODE_11526_1("11526-1","Referto di anatomia patologica"),
    CODE_59284_0("59284-0","Documento dei consensi"),
    CODE_28653_4("28653-4","Certificato di malattia"),
    CODE_57832_8("57832-8","Prescrizione diagnostica o specialistica"),
    CODE_29304_3("29304-3","Erogazione farmaceutica"),
    CODE_11488_4("11488-4","Referto specialistico"),
    CODE_57827_8("57827-8","Documento di esenzione"),
    CODE_81223_0("81223-0","Erogazione specialistica"),
    CODE_18776_5("18776-5","Piano terapeutico"),
    CODE_97500_3("97500-3","Certificazione verde Covid-19 (Digital Green Certificate)"),
    CODE_87273_9("87273-9","Scheda singola vaccinazione"),
    CODE_82593_5("82593-5","Certificato vaccinale"),
    CODE_97499_8("97499-8","Certificato di guarigione da Covid-19"),
    CODE_55750_4("55750-4","Resoconto relativo alla sicurezza del paziente"),
    CODE_68814_3("68814-3","Bilanci di salute pediatrici");

    private final String code;

    private final String documentType;

    public static DocumentTypeEnum getByCode(String code) {
        for (DocumentTypeEnum documentTypeEnum : DocumentTypeEnum.values()) {
            if (documentTypeEnum.getCode().equals(code)) {
                return documentTypeEnum;
            }
        }
        return null;
    }

}
