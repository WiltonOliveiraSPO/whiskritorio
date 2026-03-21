package com.whiskritorio.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class FormaPagamentoConverter implements AttributeConverter<FormaPagamento, String> {

    @Override
    public String convertToDatabaseColumn(FormaPagamento attribute) {
        if (attribute == null) {
            return null;
        }
        return switch (attribute) {
            case VALE_REFEICAO -> "VALE-REFEICAO";
            default -> attribute.name();
        };
    }

    @Override
    public FormaPagamento convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return switch (dbData) {
            case "VALE-REFEICAO" -> FormaPagamento.VALE_REFEICAO;
            default -> FormaPagamento.valueOf(dbData);
        };
    }
}
