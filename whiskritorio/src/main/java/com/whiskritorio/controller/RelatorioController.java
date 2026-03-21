package com.whiskritorio.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.whiskritorio.dao.ProdutoRepository;
import com.whiskritorio.dao.VendaRepository;
import com.whiskritorio.model.FormaPagamento;
import com.whiskritorio.model.Produto;
import com.whiskritorio.model.Venda;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RelatorioController {

    private final VendaRepository vendaRepository;
    private final ProdutoRepository produtoRepository;

    public RelatorioController(VendaRepository vendaRepository, ProdutoRepository produtoRepository) {
        this.vendaRepository = vendaRepository;
        this.produtoRepository = produtoRepository;
    }

    @GetMapping("/relatorios/vendas")
    public String relatorioVendas(@RequestParam(required = false) String aba,
                                  @RequestParam(required = false) String inicio,
                                  @RequestParam(required = false) String fim,
                                  @RequestParam(required = false) FormaPagamento tipo,
                                  Model model) {
        String abaAtiva = aba == null ? "periodo" : aba;

        List<Venda> vendas = aplicarFiltro(abaAtiva, inicio, fim, tipo);
        BigDecimal total = somarTotal(vendas);

        model.addAttribute("vendas", vendas);
        model.addAttribute("totalVendas", total);
        model.addAttribute("abaAtiva", abaAtiva);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("tipo", tipo);
        model.addAttribute("formasPagamento", FormaPagamento.values());
        return "view/relatorio_vendas";
    }

    @GetMapping("/relatorios/vendas/pdf")
    public ResponseEntity<byte[]> relatorioVendasPdf(@RequestParam(required = false) String aba,
                                                     @RequestParam(required = false) String inicio,
                                                     @RequestParam(required = false) String fim,
                                                     @RequestParam(required = false) FormaPagamento tipo) throws Exception {
        String abaAtiva = aba == null ? "periodo" : aba;
        List<Venda> vendas = aplicarFiltro(abaAtiva, inicio, fim, tipo);
        BigDecimal total = somarTotal(vendas);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3.5f, 1.5f});

        PdfPCell left = new PdfPCell();
        left.setBorder(PdfPCell.NO_BORDER);
        left.addElement(new Paragraph("Relatório de Vendas", titleFont));
        left.addElement(new Paragraph(filtroDescricao(abaAtiva, inicio, fim, tipo), bodyFont));

        PdfPCell right = new PdfPCell();
        right.setBorder(PdfPCell.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_TOP);

        try {
            ClassPathResource logoResource = new ClassPathResource("static/img/whiskritorio_logo.png");
            try (InputStream logoStream = logoResource.getInputStream()) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.scalePercent(20f);
                logo.setAlignment(Image.ALIGN_RIGHT);
                right.addElement(logo);
            }
        } catch (Exception ignored) {
            right.addElement(new Paragraph(" "));
        }

        header.addCell(left);
        header.addCell(right);
        document.add(header);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 3.5f, 2.2f, 2.2f, 2.4f});

        adicionarCelulaHeader(table, "ID", headerFont);
        adicionarCelulaHeader(table, "Cliente", headerFont);
        adicionarCelulaHeader(table, "Forma", headerFont);
        adicionarCelulaHeader(table, "Total", headerFont);
        adicionarCelulaHeader(table, "Data", headerFont);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

        for (Venda venda : vendas) {
            table.addCell(new Phrase(String.valueOf(venda.getId()), bodyFont));
            table.addCell(new Phrase(venda.getCliente().getNome(), bodyFont));
            table.addCell(new Phrase(venda.getFormaPagamento().getLabel(), bodyFont));
            table.addCell(new Phrase(moeda.format(venda.getTotal()), bodyFont));
            table.addCell(new Phrase(dtf.format(venda.getCriadoEm()), bodyFont));
        }

        document.add(table);
        document.add(new Paragraph(" "));
        Paragraph totalP = new Paragraph("Total Geral: " + moeda.format(total), headerFont);
        totalP.setAlignment(Element.ALIGN_RIGHT);
        document.add(totalP);

        document.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=relatorio_vendas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    @GetMapping("/relatorios/estoque")
    public String relatorioEstoque(Model model) {
        List<Produto> produtos = produtoRepository.findAll(Sort.by("nome").ascending());
        model.addAttribute("produtos", produtos);
        return "view/relatorio_estoque";
    }

    private List<Venda> aplicarFiltro(String aba, String inicio, String fim, FormaPagamento tipo) {
        List<Venda> vendas = vendaRepository.findAll(Sort.by("criadoEm").descending());

        if (Objects.equals(aba, "tipo") && tipo != null) {
            return vendas.stream()
                    .filter(v -> v.getFormaPagamento() == tipo)
                    .collect(Collectors.toList());
        }

        if (Objects.equals(aba, "periodo")) {
            Optional<LocalDateTime> inicioDt = parseDataInicio(inicio);
            Optional<LocalDateTime> fimDt = parseDataFim(fim);

            return vendas.stream()
                    .filter(v -> dentroPeriodo(v.getCriadoEm(), inicioDt, fimDt))
                    .collect(Collectors.toList());
        }

        return vendas;
    }

    private Optional<LocalDateTime> parseDataInicio(String data) {
        if (data == null || data.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(data).atStartOfDay());
    }

    private Optional<LocalDateTime> parseDataFim(String data) {
        if (data == null || data.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(data).atTime(LocalTime.MAX));
    }

    private boolean dentroPeriodo(LocalDateTime data,
                                  Optional<LocalDateTime> inicio,
                                  Optional<LocalDateTime> fim) {
        if (inicio.isPresent() && data.isBefore(inicio.get())) {
            return false;
        }
        if (fim.isPresent() && data.isAfter(fim.get())) {
            return false;
        }
        return true;
    }

    private BigDecimal somarTotal(List<Venda> vendas) {
        BigDecimal total = BigDecimal.ZERO;
        for (Venda venda : vendas) {
            if (venda.getTotal() != null) {
                total = total.add(venda.getTotal());
            }
        }
        return total;
    }

    private void adicionarCelulaHeader(PdfPTable table, String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBackgroundColor(new Color(245, 245, 245));
        table.addCell(cell);
    }

    private String filtroDescricao(String aba, String inicio, String fim, FormaPagamento tipo) {
        if (Objects.equals(aba, "tipo") && tipo != null) {
            return "Filtro: Tipo de pagamento = " + tipo.getLabel();
        }
        if (Objects.equals(aba, "periodo")) {
            String inicioTxt = (inicio == null || inicio.isBlank()) ? "(início livre)" : inicio;
            String fimTxt = (fim == null || fim.isBlank()) ? "(fim livre)" : fim;
            return "Filtro: Período de " + inicioTxt + " até " + fimTxt;
        }
        return "Filtro: Todos";
    }
}
