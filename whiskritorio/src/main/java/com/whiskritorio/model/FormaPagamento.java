package com.whiskritorio.model;

public enum FormaPagamento {
    DINHEIRO("Dinheiro"),
    DEBITO("Débito"),
    CREDITO("Crédito"),
    VALE_REFEICAO("Vale-refeição"),
    PIX("Pix");

    private final String label;

    FormaPagamento(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
