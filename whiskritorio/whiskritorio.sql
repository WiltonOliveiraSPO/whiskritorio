use whiskritorio;

CREATE TABLE clientes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  email VARCHAR(160),
  telefone VARCHAR(30),
  documento VARCHAR(30),
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE produtos (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome VARCHAR(160) NOT NULL,
  categoria VARCHAR(80),
  preco DECIMAL(10,2) NOT NULL,
  estoque INT NOT NULL DEFAULT 0,
  ativo TINYINT(1) NOT NULL DEFAULT 1,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE vendas (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  cliente_id BIGINT NOT NULL,
  forma_pagamento ENUM('DINHEIRO','DEBITO','CREDITO','VALE_REFEICAO','PIX') NOT NULL,
  total DECIMAL(12,2) NOT NULL,
  criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_vendas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id)
) ENGINE=InnoDB;

CREATE TABLE venda_itens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  venda_id BIGINT NOT NULL,
  produto_id BIGINT NOT NULL,
  quantidade INT NOT NULL,
  preco_unitario DECIMAL(10,2) NOT NULL,
  subtotal DECIMAL(12,2) NOT NULL,
  CONSTRAINT fk_itens_venda FOREIGN KEY (venda_id) REFERENCES vendas(id),
  CONSTRAINT fk_itens_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
) ENGINE=InnoDB;

CREATE INDEX idx_vendas_cliente ON vendas(cliente_id);
CREATE INDEX idx_itens_venda ON venda_itens(venda_id);
CREATE INDEX idx_itens_produto ON venda_itens(produto_id);

DELIMITER $$

CREATE TRIGGER trg_baixa_estoque
AFTER INSERT ON venda_itens
FOR EACH ROW
BEGIN
  IF (SELECT estoque FROM produtos WHERE id = NEW.produto_id) < NEW.quantidade THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Estoque insuficiente';
  END IF;

  UPDATE produtos
  SET estoque = estoque - NEW.quantidade
  WHERE id = NEW.produto_id;
END$$

DELIMITER ;


